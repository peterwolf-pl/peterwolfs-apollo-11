package pl.peterwolf.apollo11.world;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import pl.peterwolf.apollo11.Apollo11;

/** Generates a deterministic crater field once as each Moon chunk is created. */
public final class MoonCraterFeature implements Feature {
    public static final int CELL_SIZE = 32;
    public static final int LANDING_CLEAR_RADIUS = 32;
    private static final int MIN_RADIUS = 2;
    private static final int MAX_RADIUS = 18;
    private static final int RIM_WIDTH = 2;
    private static final int MAX_REACH = MAX_RADIUS + RIM_WIDTH;
    private static final int SURFACE_Y = 5;
    private static final MapCodec<MoonCraterFeature> CODEC = MapCodec.unit(MoonCraterFeature::new);
    private static final BlockState REGOLITH = Blocks.TUFF.defaultBlockState();
    private static final BlockState SURFACE = Blocks.CONCRETE.gray().defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private MoonCraterFeature() {
    }

    public static void initialize() {
        Identifier id = Identifier.fromNamespaceAndPath(Apollo11.MOD_ID, "moon_crater_field");
        Registry.register(BuiltInRegistries.FEATURE_TYPE, id, CODEC);
    }

    public static Crater craterForCell(long worldSeed, int cellX, int cellZ) {
        RandomSource random = RandomSource.create(cellSeed(worldSeed, cellX, cellZ));
        int roll = random.nextInt(100);
        int radius;
        if (roll < 68) {
            radius = MIN_RADIUS + random.nextInt(4);
        } else if (roll < 94) {
            radius = 6 + random.nextInt(6);
        } else {
            radius = 12 + random.nextInt(7);
        }
        int centerX = cellX * CELL_SIZE + random.nextInt(CELL_SIZE);
        int centerZ = cellZ * CELL_SIZE + random.nextInt(CELL_SIZE);
        int depth = Math.min(4, Math.max(1, (int) Math.round(radius * 0.28)));
        return new Crater(centerX, centerZ, radius, depth);
    }

    @Override
    public MapCodec<? extends Feature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        ChunkPos chunk = new ChunkPos(origin.getX() >> 4, origin.getZ() >> 4);
        int minCellX = Math.floorDiv(chunk.getMinBlockX() - MAX_REACH, CELL_SIZE);
        int maxCellX = Math.floorDiv(chunk.getMaxBlockX() + MAX_REACH, CELL_SIZE);
        int minCellZ = Math.floorDiv(chunk.getMinBlockZ() - MAX_REACH, CELL_SIZE);
        int maxCellZ = Math.floorDiv(chunk.getMaxBlockZ() + MAX_REACH, CELL_SIZE);
        List<Crater> candidates = new ArrayList<>((maxCellX - minCellX + 1) * (maxCellZ - minCellZ + 1));

        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                Crater crater = craterForCell(world.getSeed(), cellX, cellZ);
                if (!overlapsLandingArea(crater)) {
                    candidates.add(crater);
                }
            }
        }

        boolean changed = false;
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                int topY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                position.set(x, topY, z);
                if (topY != SURFACE_Y || !isMoonSurface(world.getBlockState(position))) {
                    continue;
                }

                int heightOffset = craterOffset(x, z, candidates);
                if (heightOffset == 0) {
                    continue;
                }

                int targetY = topY + heightOffset;
                if (targetY < topY) {
                    for (int y = targetY + 1; y <= topY; y++) {
                        position.set(x, y, z);
                        world.setBlock(position, AIR, 2);
                    }
                    position.set(x, targetY, z);
                    world.setBlock(position, REGOLITH, 2);
                } else {
                    for (int y = topY + 1; y < targetY; y++) {
                        position.set(x, y, z);
                        world.setBlock(position, REGOLITH, 2);
                    }
                    position.set(x, targetY, z);
                    world.setBlock(position, SURFACE, 2);
                }
                changed = true;
            }
        }
        return changed;
    }

    private static int craterOffset(int x, int z, List<Crater> candidates) {
        int strongestOffset = 0;
        double strongestInfluence = 0.0;
        for (Crater crater : candidates) {
            double influence = crater.influence(x, z);
            if (influence > strongestInfluence) {
                strongestInfluence = influence;
                strongestOffset = crater.surfaceOffset(x, z);
            }
        }
        return strongestOffset;
    }

    private static boolean overlapsLandingArea(Crater crater) {
        int safeReach = LANDING_CLEAR_RADIUS + crater.radius + RIM_WIDTH;
        return square(crater.centerX) + square(crater.centerZ) < square(safeReach);
    }

    private static boolean isMoonSurface(BlockState state) {
        return state.is(Blocks.CONCRETE.gray()) || state.is(Blocks.TUFF) || state.is(Blocks.STONE);
    }

    private static long cellSeed(long worldSeed, int cellX, int cellZ) {
        long value = worldSeed ^ ((long) cellX * 0x9E3779B97F4A7C15L)
                ^ ((long) cellZ * 0xC2B2AE3D27D4EB4FL);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static long square(int value) {
        return (long) value * value;
    }

    public record Crater(int centerX, int centerZ, int radius, int depth) {
        private double influence(int x, int z) {
            int deltaX = x - centerX;
            int deltaZ = z - centerZ;
            double distance = Math.sqrt(square(deltaX) + square(deltaZ));
            int outerRadius = radius + RIM_WIDTH;
            return distance > outerRadius ? 0.0 : 1.0 - distance / outerRadius;
        }

        private int surfaceOffset(int x, int z) {
            int deltaX = x - centerX;
            int deltaZ = z - centerZ;
            long distanceSquared = square(deltaX) + square(deltaZ);
            int outerRadius = radius + RIM_WIDTH;
            if (distanceSquared > square(outerRadius)) {
                return 0;
            }

            double distance = Math.sqrt(distanceSquared);
            double normalized = distance / radius;
            if (normalized < 0.68) {
                double bowlPosition = normalized / 0.68;
                double bowl = 1.0 - bowlPosition * bowlPosition;
                return -(int) Math.max(1, Math.round(depth * bowl));
            }
            if (normalized < 1.0) {
                double slope = (normalized - 0.68) / 0.32;
                return -(int) Math.round(depth * (1.0 - slope));
            }

            double rimProgress = (distance - radius) / RIM_WIDTH;
            return (int) Math.round(2.0 * Math.sin(Math.PI * rimProgress));
        }
    }
}
