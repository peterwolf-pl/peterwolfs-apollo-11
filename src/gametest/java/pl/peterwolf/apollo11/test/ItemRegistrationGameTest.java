package pl.peterwolf.apollo11.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Item;
import pl.peterwolf.apollo11.Apollo11;
import pl.peterwolf.apollo11.ModBlocks;
import pl.peterwolf.apollo11.block.LaunchGantryBlock;
import pl.peterwolf.apollo11.ModItems;
import pl.peterwolf.apollo11.launch.LaunchSequence;

public final class ItemRegistrationGameTest implements FabricClientGameTest {
    private static final ResourceKey<Level> MOON_DIMENSION = ResourceKey.create(Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(Apollo11.MOD_ID, "moon"));
    private static final String[] ITEM_NAMES = {
            "rocket_frame", "rocket_engine", "guidance_computer", "apollo_rocket", "launch_gantry"
    };
    private static final Item[] ITEMS = {
            ModItems.ROCKET_FRAME, ModItems.ROCKET_ENGINE, ModItems.GUIDANCE_COMPUTER,
            ModItems.APOLLO_ROCKET, ModItems.LAUNCH_GANTRY
    };
    private static final String[] RECIPE_NAMES = {
            "rocket_frame", "rocket_engine", "guidance_computer", "apollo_rocket", "launch_gantry"
    };

    @Override
    public void runTest(ClientGameTestContext context) {
        for (int i = 0; i < ITEM_NAMES.length; i++) {
            String name = ITEM_NAMES[i];
            Identifier itemId = Identifier.fromNamespaceAndPath(Apollo11.MOD_ID, name);
            require(BuiltInRegistries.ITEM.get(itemId).orElseThrow().value() == ITEMS[i],
                    "Apollo item was not registered under " + itemId);

            boolean modelExists = context.computeOnClient(client -> client.getResourceManager().getResource(
                    Identifier.fromNamespaceAndPath(Apollo11.MOD_ID, "items/" + name + ".json")).isPresent());
            require(modelExists, "Item model definition is missing for " + itemId);
        }

        Identifier gantryId = Identifier.fromNamespaceAndPath(Apollo11.MOD_ID, "launch_gantry");
        require(BuiltInRegistries.BLOCK.get(gantryId).orElseThrow().value() == ModBlocks.LAUNCH_GANTRY,
                "Launch gantry block was not registered under " + gantryId);
        require(ModItems.LAUNCH_GANTRY instanceof BlockItem blockItem
                        && blockItem.getBlock() == ModBlocks.LAUNCH_GANTRY,
                "Launch gantry item is not linked to its placeable block");
        boolean blockstateExists = context.computeOnClient(client -> client.getResourceManager().getResource(
                Identifier.fromNamespaceAndPath(Apollo11.MOD_ID, "blockstates/launch_gantry.json")).isPresent());
        require(blockstateExists, "Launch gantry blockstate resource is missing");

        var modContainer = FabricLoader.getInstance().getModContainer(Apollo11.MOD_ID).orElseThrow();
        for (String recipeName : RECIPE_NAMES) {
            String recipePath = "data/" + Apollo11.MOD_ID + "/recipe/" + recipeName + ".json";
            require(modContainer.findPath(recipePath).isPresent(), "Crafting recipe resource is missing: " + recipePath);
        }
        require(modContainer.findPath("data/" + Apollo11.MOD_ID + "/loot_table/blocks/launch_gantry.json").isPresent(),
                "Launch gantry block loot table is missing");

        try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(true).create()) {
            world.getConnection().waitForChunksDownload();
            BlockPos pos = new BlockPos(0, 80, 0);
            boolean sequenceStarted = world.getServer().computeOnServer(server -> {
                var level = server.overworld();
                if (!level.setBlock(pos, ModBlocks.LAUNCH_GANTRY.defaultBlockState(), 3)) {
                    return false;
                }
                var player = server.getPlayerList().getPlayers().get(0);
                player.setGameMode(GameType.SURVIVAL);
                ItemStack rocket = new ItemStack(ModItems.APOLLO_ROCKET);
                player.setItemInHand(InteractionHand.MAIN_HAND, rocket);
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                var result = ((LaunchGantryBlock) ModBlocks.LAUNCH_GANTRY).useItemOn(rocket,
                        level.getBlockState(pos), level, pos,
                        player, InteractionHand.MAIN_HAND, hit);
                return result.consumesAction() && rocket.isEmpty() && LaunchSequence.isActive(level, pos);
            });
            require(sequenceStarted, "Using the Apollo rocket on the launch gantry did not start the countdown");

            context.waitTicks(105);
            boolean sequenceCompleted = world.getServer().computeOnServer(
                    server -> !LaunchSequence.isActive(server.overworld(), pos));
            require(sequenceCompleted, "Launch countdown did not complete after five seconds");
            boolean arrivedOnMoon = world.getServer().computeOnServer(server -> {
                var moon = server.getLevel(MOON_DIMENSION);
                var player = server.getPlayerList().getPlayers().get(0);
                return moon != null && player.level() == moon
                        && moon.getBlockState(new BlockPos(0, 5, 0)).is(Blocks.CONCRETE.gray());
            });
            require(arrivedOnMoon, "Launch completed without transferring the player to the Moon dimension");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
