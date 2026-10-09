package pl.peterwolf.apollo11;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Registry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModItems {
    public static final Item ROCKET_FRAME = register("rocket_frame");
    public static final Item ROCKET_ENGINE = register("rocket_engine");
    public static final Item GUIDANCE_COMPUTER = register("guidance_computer");
    public static final Item APOLLO_ROCKET = register("apollo_rocket");
    public static final Item LAUNCH_GANTRY = registerBlock("launch_gantry", ModBlocks.LAUNCH_GANTRY);

    private ModItems() {
    }

    private static Item register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(Apollo11.MOD_ID, name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, id, new Item(new Item.Properties().setId(key)));
    }

    private static Item registerBlock(String name, Block block) {
        Identifier id = Identifier.fromNamespaceAndPath(Apollo11.MOD_ID, name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        return Registry.register(BuiltInRegistries.ITEM, id,
                new BlockItem(block, new Item.Properties().setId(key)));
    }

    public static void initialize() {
        ResourceKey<net.minecraft.world.item.CreativeModeTab> toolsTab = ResourceKey.create(
                Registries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath("minecraft", "tools_and_utilities")
        );
        CreativeModeTabEvents.modifyOutputEvent(toolsTab).register(output -> {
            output.accept(ROCKET_FRAME);
            output.accept(ROCKET_ENGINE);
            output.accept(GUIDANCE_COMPUTER);
            output.accept(APOLLO_ROCKET);
            output.accept(LAUNCH_GANTRY);
        });
    }
}
