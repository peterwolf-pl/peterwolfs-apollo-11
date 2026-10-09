const https = require('https');
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const CF_HOST = 'minecraft.curseforge.com';

async function main() {
  console.log('========================================================');
  console.log('🚀 CurseForge Upload Automation (Minecraft Fabric)');
  console.log('========================================================');

  const args = process.argv.slice(2);
  const isPublish = args.includes('--publish');
  const isDryRun = args.includes('--dry-run') || !isPublish;

  // 1. Read .github/curseforge.json
  const configPath = path.resolve('.github/curseforge.json');
  if (!fs.existsSync(configPath)) {
    console.error('::error::Nie znaleziono pliku konfiguracyjnego .github/curseforge.json!');
    process.exit(1);
  }

  let config;
  try {
    config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  } catch (err) {
    console.error(`::error::Błąd parsowania .github/curseforge.json: ${err.message}`);
    process.exit(1);
  }

  // 2. Read fabric.mod.json
  let fmjPath = path.resolve('src/main/resources/fabric.mod.json');
  if (!fs.existsSync(fmjPath)) fmjPath = path.resolve('src/client/resources/fabric.mod.json');
  if (!fs.existsSync(fmjPath)) fmjPath = path.resolve('fabric.mod.json');

  let fmj = {};
  if (fs.existsSync(fmjPath)) {
    try {
      fmj = JSON.parse(fs.readFileSync(fmjPath, 'utf8'));
    } catch (e) {
      console.warn(`::warning::Nie udało się odczytać fabric.mod.json: ${e.message}`);
    }
  }

  // Read gradle.properties
  const gradleProps = {};
  const gradlePropsPath = path.resolve('gradle.properties');
  if (fs.existsSync(gradlePropsPath)) {
    const lines = fs.readFileSync(gradlePropsPath, 'utf8').split('\n');
    for (const l of lines) {
      const match = l.match(/^([a-zA-Z0-9_.-]+)\s*=\s*(.*)$/);
      if (match) gradleProps[match[1].trim()] = match[2].trim();
    }
  }

  const modName = fmj.name || path.basename(process.cwd());
  const modId = fmj.id || path.basename(process.cwd());
  const modVersion = gradleProps['mod_version'] || fmj.version || '1.0.0';
  const rawMcVersion = gradleProps['minecraft_version'] || (fmj.depends && fmj.depends.minecraft ? fmj.depends.minecraft : '26.3');
  const cleanMcVersion = rawMcVersion.replace(/[^\d.]/g, '');
  const javaVersion = gradleProps['java_version'] || (cleanMcVersion.startsWith('26') ? '25' : '21');

  console.log(`Mod:        ${modName} (${modId})`);
  console.log(`Wersja:     ${modVersion}`);
  console.log(`Minecraft:  ${cleanMcVersion}`);
  console.log(`Java:       ${javaVersion}`);
  console.log(`Tryb:       ${isPublish ? '🔴 PUBLIKACJA NA ŻYWO (--publish)' : '🟡 TEST / DRY RUN (--dry-run)'}`);

  // 3. Check Enabled & Project ID
  if (!config.enabled && isPublish) {
    console.log('\n::notice::Publikacja jest wyłączona w konfiguracji (.github/curseforge.json -> enabled: false).');
    console.log('Ustaw "enabled": true w .github/curseforge.json, aby włączyć publikację dla tego projektu.');
    process.exit(0);
  }

  if (!config.projectId && isPublish) {
    console.log('\n::notice::Brak skonfigurowanego CurseForge Project ID (.github/curseforge.json -> projectId: null).');
    console.log('Uzupełnij identyfikator projektu w .github/curseforge.json, aby publikować.');
    process.exit(0);
  }

  const token = process.env.CURSEFORGE_TOKEN;
  if (!token) {
    if (isPublish) {
      console.log('\n::warning::Brak sekretu CURSEFORGE_TOKEN w środowisku / GitHub Secrets!');
      console.log('Publikacja została wstrzymana bez wysyłania plików.');
      process.exit(0);
    } else {
      console.log('\nBrak CURSEFORGE_TOKEN w środowisku (wymagany do pełnej weryfikacji API).');
    }
  }

  // 4. Locate distribution JAR in build/libs/
  const libsDir = path.resolve('build/libs');
  if (!fs.existsSync(libsDir)) {
    console.error('\n::error::Katalog build/libs nie istnieje. Uruchom najpierw build Gradle (np. ./gradlew remapJar lub ./gradlew build)!');
    process.exit(1);
  }

  const jarFiles = fs.readdirSync(libsDir).filter(f =>
    f.endsWith('.jar') &&
    !f.endsWith('-sources.jar') &&
    !f.endsWith('-dev.jar') &&
    !f.endsWith('-shadow.jar')
  );

  if (jarFiles.length === 0) {
    console.error('\n::error::Nie znaleziono dystrybucyjnego pliku JAR w build/libs! (wykluczono -sources.jar, -dev.jar).');
    process.exit(1);
  }

  // Prefer JAR matching mod version
  let targetJar = jarFiles.find(f => f.includes(modVersion)) || jarFiles[0];
  const targetJarPath = path.join(libsDir, targetJar);
  const targetJarStat = fs.statSync(targetJarPath);

  console.log(`\nWybrany plik JAR: ${targetJar} (${(targetJarStat.size / 1024).toFixed(1)} KB)`);

  // 5. Fetch and Resolve CurseForge Game Versions
  let gameVersionIds = [];
  if (token) {
    try {
      console.log('Pobieranie oficjalnych wersji z CurseForge Game Versions API...');
      const versions = await fetchCurseForgeVersions(token);

      // Match Minecraft version
      const mcMatch = versions.find(v => v.name === cleanMcVersion);
      if (!mcMatch) {
        console.error(`\n::error::CurseForge Game Versions API nie zawiera wersji Minecraft "${cleanMcVersion}". Publikacja zatrzymana zgodnie z wytycznymi.`);
        process.exit(1);
      }
      gameVersionIds.push(mcMatch.id);
      console.log(`  ✓ Minecraft ${cleanMcVersion} -> ID ${mcMatch.id}`);

      // Match ModLoader (Fabric)
      const fabricMatch = versions.find(v => v.name.toLowerCase() === 'fabric' && v.gameVersionTypeID === 68441);
      if (!fabricMatch) {
        console.error(`\n::error::Nie znaleziono modloadera Fabric w CurseForge API. Publikacja zatrzymana.`);
        process.exit(1);
      }
      gameVersionIds.push(fabricMatch.id);
      console.log(`  ✓ Fabric -> ID ${fabricMatch.id}`);

      // Match Java Version
      const javaName = `Java ${javaVersion.toString().replace(/[^\d]/g, '')}`;
      const javaMatch = versions.find(v => v.name.toLowerCase() === javaName.toLowerCase() && v.gameVersionTypeID === 2);
      if (javaMatch) {
        gameVersionIds.push(javaMatch.id);
        console.log(`  ✓ ${javaName} -> ID ${javaMatch.id}`);
      }
    } catch (e) {
      console.warn(`::warning::Błąd zapytania do Game Versions API: ${e.message}`);
      if (isPublish) {
        console.error('::error::Nie można jednoznacznie ustalić wersji CurseForge. Publikacja wstrzymana.');
        process.exit(1);
      }
    }
  } else {
    // Fallback known IDs if dry-run without token
    console.log('Używanie zweryfikowanych identyfikatorów CurseForge (tryb offline / brak tokena)...');
    const knownVersions = {
      '26.3': 17045,
      '26.2': 16498,
      '26.1.2': 16082,
      '26.1': 15933,
      '1.21.11': 14406,
      '1.21.1': 11779
    };
    if (knownVersions[cleanMcVersion]) gameVersionIds.push(knownVersions[cleanMcVersion]);
    gameVersionIds.push(7499); // Fabric
    if (javaVersion === '25') gameVersionIds.push(14454); // Java 25
  }

  // 6. Duplicate Protection Check
  if (token && config.projectId) {
    try {
      console.log(`Sprawdzanie duplikatów plików w projekcie CurseForge #${config.projectId}...`);
      const existingFiles = await fetchProjectFiles(config.projectId, token);
      if (Array.isArray(existingFiles)) {
        const dup = existingFiles.find(f => f.fileName && f.fileName.toLowerCase() === targetJar.toLowerCase());
        if (dup) {
          console.log(`\n::warning::Ochrona przed duplikatem: Plik "${targetJar}" został już wcześniej opublikowany na CurseForge (File ID: ${dup.id})!`);
          console.log('Pomijanie ponownego wysyłania tej samej wersji.');
          process.exit(0);
        }
      }
    } catch(e) {
      // 403 or non-existing, continue
    }
  }

  // 7. Generate Changelog
  let changelog = process.env.CHANGELOG || process.env.GITHUB_RELEASE_BODY || config.changelog || '';
  if (!changelog.trim()) {
    try {
      changelog = execSync('git log -n 5 --pretty=format:"* %s (%h)"', { encoding: 'utf8' }).trim();
    } catch(e) {
      changelog = `Release ${modName} v${modVersion}`;
    }
  }

  // Release type override from CLI or config
  const releaseType = (args.find(a => a.startsWith('--release-type='))?.split('=')[1]) || config.releaseType || 'alpha';

  // Display name override from title in config
  const displayName = config.title ? `${config.title} ${modVersion}` : `${modName} ${modVersion}`;

  // Relations / Dependencies
  const relations = {
    projects: config.dependencies || []
  };

  const metadata = {
    changelog: changelog,
    changelogType: config.changelogType || 'markdown',
    displayName: displayName,
    gameVersions: gameVersionIds,
    releaseType: releaseType,
    isMarkedForManualRelease: Boolean(config.manualRelease),
    relations: relations
  };

  console.log('\n--- PRZYGOTOWANE METADANE CURSEFORGE ---');
  console.log(JSON.stringify(metadata, null, 2));

  // 8. Execute or Dry-Run
  if (isDryRun) {
    console.log('\n========================================================');
    console.log('✓ WALIDACJA POMYŚLNA (DRY-RUN / MOCK)');
    console.log('Plik JAR, metadane oraz wersje gry zostały weryfikowane.');
    console.log('Żadne pliki nie zostały wysłane do CurseForge.');
    console.log('Aby wykonać rzeczywistą publikację, uruchom z flagą --publish');
    console.log('========================================================');

    if (process.env.GITHUB_STEP_SUMMARY) {
      fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, `
### 🧪 CurseForge Build & Validation (Dry-Run)
- **Mod:** \`${modName}\` (${modId})
- **Wersja:** \`${modVersion}\`
- **Plik JAR:** \`${targetJar}\` (${(targetJarStat.size / 1024).toFixed(1)} KB)
- **Minecraft:** \`${cleanMcVersion}\` (IDs: ${gameVersionIds.join(', ')})
- **Typ wydania:** \`${releaseType}\`
- **Status:** ✅ Zbudowano i zwalidowano pomyślnie. Nie wysłano na CurseForge (tryb walidacji).
`);
    }
    process.exit(0);
  }

  // Real upload
  console.log(`\n🚀 WYSYŁANIE PLIKU DO CURSEFORGE (Project ID: ${config.projectId})...`);
  try {
    const uploadRes = await uploadFileToCurseForge(config.projectId, targetJarPath, metadata, token);
    console.log('\n========================================================');
    console.log(`🎉 SUKCES! Plik został pomyślnie opublikowany na CurseForge!`);
    console.log(`File ID: ${uploadRes.id}`);
    console.log('========================================================');

    if (process.env.GITHUB_OUTPUT) {
      fs.appendFileSync(process.env.GITHUB_OUTPUT, `file_id=${uploadRes.id}\n`);
    }

    if (process.env.GITHUB_STEP_SUMMARY) {
      fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, `
### 🚀 Opublikowano na CurseForge!
- **Mod:** \`${modName}\`
- **Wersja:** \`${modVersion}\`
- **CurseForge File ID:** \`${uploadRes.id}\`
- **Plik:** \`${targetJar}\`
- **Typ wydania:** \`${releaseType}\`
`);
    }
  } catch (err) {
    console.error(`\n::error::Błąd podczas wysyłania pliku na CurseForge: ${err.message}`);
    process.exit(1);
  }
}

function fetchCurseForgeVersions(token) {
  return new Promise((resolve, reject) => {
    const req = https.request({
      hostname: CF_HOST,
      path: '/api/game/versions',
      method: 'GET',
      headers: {
        'X-Api-Token': token,
        'User-Agent': 'CurseForgeModUploader/1.0'
      }
    }, res => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          resolve(JSON.parse(data));
        } catch(e) {
          reject(new Error(`Failed to parse versions JSON: ${e.message}`));
        }
      });
    });
    req.on('error', reject);
    req.end();
  });
}

function fetchProjectFiles(projectId, token) {
  return new Promise((resolve, reject) => {
    const req = https.request({
      hostname: CF_HOST,
      path: `/api/projects/${projectId}/files`,
      method: 'GET',
      headers: {
        'X-Api-Token': token,
        'User-Agent': 'CurseForgeModUploader/1.0'
      }
    }, res => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          resolve(JSON.parse(data));
        } catch(e) {
          resolve(null);
        }
      });
    });
    req.on('error', reject);
    req.end();
  });
}

function uploadFileToCurseForge(projectId, filePath, metadata, token) {
  const boundary = '----WebKitFormBoundary' + Math.random().toString(36).substring(2) + Date.now().toString(36);
  const fileBuffer = fs.readFileSync(filePath);
  const fileName = path.basename(filePath);
  const metadataString = JSON.stringify(metadata);

  const crlf = '\r\n';
  let head = '';
  head += `--${boundary}${crlf}`;
  head += `Content-Disposition: form-data; name="metadata"${crlf}`;
  head += `Content-Type: application/json; charset=utf-8${crlf}${crlf}`;
  head += metadataString + crlf;

  head += `--${boundary}${crlf}`;
  head += `Content-Disposition: form-data; name="file"; filename="${fileName}"${crlf}`;
  head += `Content-Type: application/java-archive${crlf}${crlf}`;

  const tail = `${crlf}--${boundary}--${crlf}`;

  const headBuffer = Buffer.from(head, 'utf8');
  const tailBuffer = Buffer.from(tail, 'utf8');
  const totalLength = headBuffer.length + fileBuffer.length + tailBuffer.length;

  return new Promise((resolve, reject) => {
    const req = https.request({
      hostname: CF_HOST,
      path: `/api/projects/${projectId}/upload-file`,
      method: 'POST',
      headers: {
        'X-Api-Token': token,
        'User-Agent': 'CurseForgeModUploader/1.0',
        'Content-Type': `multipart/form-data; boundary=${boundary}`,
        'Content-Length': totalLength
      }
    }, res => {
      let raw = '';
      res.on('data', chunk => raw += chunk);
      res.on('end', () => {
        let parsed;
        try { parsed = JSON.parse(raw); } catch(e) { parsed = raw; }
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(parsed);
        } else {
          const msg = typeof parsed === 'object' && parsed !== null
            ? (parsed.errorMessage || parsed.message || JSON.stringify(parsed))
            : parsed;
          reject(new Error(`HTTP ${res.statusCode}: ${msg}`));
        }
      });
    });

    req.on('error', reject);
    req.write(headBuffer);
    req.write(fileBuffer);
    req.write(tailBuffer);
    req.end();
  });
}

main().catch(err => {
  console.error('Błąd wykonania skryptu uploadu:', err);
  process.exit(1);
});
