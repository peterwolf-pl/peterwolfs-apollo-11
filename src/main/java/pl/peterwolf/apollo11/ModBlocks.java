package pl.peterwolf.apollo11;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import pl.peterwolf.apollo11.block.LaunchGantryBlock;

public final class ModBlocks {
    public static final Block LAUNCH_GANTRY = register("launch_gantry");

    private ModBlocks() {
    }

    private static Block register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(Apollo11.MOD_ID, name);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        Block block = new LaunchGantryBlock(
                BlockBehaviour.Properties.of().setId(key).strength(3.0F, 6.0F).noOcclusion()
        );
        return Registry.register(BuiltInRegistries.BLOCK, id, block);
    }

    public static void initialize() {
        // Loading this class registers the blocks before their matching items.
    }
}
