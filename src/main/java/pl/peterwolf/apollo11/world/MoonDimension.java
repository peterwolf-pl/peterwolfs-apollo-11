package pl.peterwolf.apollo11.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import pl.peterwolf.apollo11.Apollo11;

public final class MoonDimension {
    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(Apollo11.MOD_ID, "moon"));

    private MoonDimension() {
    }
}
