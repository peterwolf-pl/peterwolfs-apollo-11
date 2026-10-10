package pl.peterwolf.apollo11.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.resources.model.sprite.SpriteId;
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
import pl.peterwolf.apollo11.client.FlightWindowHud;
import pl.peterwolf.apollo11.ModBlocks;
import pl.peterwolf.apollo11.block.LaunchGantryBlock;
import pl.peterwolf.apollo11.ModItems;
import pl.peterwolf.apollo11.landing.LandingDifficulty;
import pl.peterwolf.apollo11.landing.LandingDifficultyStore;
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
        boolean earthTextureExists = context.computeOnClient(client -> client.getResourceManager().getResource(
                Identifier.fromNamespaceAndPath(Apollo11.MOD_ID,
                        "textures/gui/sprites/earth_globe.png")).isPresent());
        require(earthTextureExists, "Earth flight-window texture is missing");
        boolean earthSpriteLoaded = context.computeOnClient(client -> client.getAtlasManager().get(new SpriteId(
                Identifier.withDefaultNamespace("textures/atlas/gui.png"),
                Identifier.fromNamespaceAndPath(Apollo11.MOD_ID, "earth_globe"))) != null);
        require(earthSpriteLoaded, "Earth flight-window sprite is missing from the GUI atlas");

        var modContainer = FabricLoader.getInstance().getModContainer(Apollo11.MOD_ID).orElseThrow();
        for (String recipeName : RECIPE_NAMES) {
            String recipePath = "data/" + Apollo11.MOD_ID + "/recipe/" + recipeName + ".json";
            require(modContainer.findPath(recipePath).isPresent(), "Crafting recipe resource is missing: " + recipePath);
        }
        require(modContainer.findPath("data/" + Apollo11.MOD_ID + "/loot_table/blocks/launch_gantry.json").isPresent(),
                "Launch gantry block loot table is missing");

        try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(true).create()) {
            world.getConnection().waitForChunksDownload();
            double overworldGravity = world.getServer().computeOnServer(
                    server -> server.getPlayerList().getPlayers().get(0).getGravity());
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

            world.getServer().computeOnServer(server -> {
                server.overworld().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                return null;
            });
            context.waitTicks(1);
            boolean commandStarted = world.getServer().computeOnServer(server -> {
                var level = server.overworld();
                level.setBlock(pos, ModBlocks.LAUNCH_GANTRY.defaultBlockState(), 3);
                var player = server.getPlayerList().getPlayers().get(0);
                player.teleportTo(0.5, 81.0, 0.5);
                boolean startsEasy = LandingDifficultyStore.get(player) == LandingDifficulty.EASY;
                server.getCommands().performPrefixedCommand(player.createCommandSourceStack(),
                        "apollo difficulty professional");
                boolean professionalSelected = LandingDifficultyStore.get(player) == LandingDifficulty.PROFESSIONAL;
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.APOLLO_ROCKET));
                server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), "apollo launch");
                return startsEasy && professionalSelected && LaunchSequence.isActive(level, pos)
                        && player.getMainHandItem().isEmpty();
            });
            require(commandStarted, "The /apollo launch command did not start the nearby gantry");

            context.waitTicks(105);
            int manualBurnAccepted = world.getServer().computeOnServer(server -> {
                try {
                    return server.getCommands().getDispatcher().execute("apollo landing burn",
                            server.getPlayerList().getPlayers().get(0).createCommandSourceStack());
                } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
                    return 0;
                }
            });
            require(manualBurnAccepted == 1,
                    "Professional landing did not accept the required manual retrograde burn");
            boolean flightWindowActive = context.computeOnClient(client -> FlightWindowHud.isActive());
            boolean flightStillInProgress = world.getServer().computeOnServer(
                    server -> LaunchSequence.isActive(server.overworld(), pos)
                            && server.getPlayerList().getPlayers().get(0).level() == server.overworld());
            require(flightWindowActive && flightStillInProgress,
                    "T-0 did not start the Earth-receding flight window before Moon transfer");

            context.waitTicks(100);
            boolean sequenceCompleted = world.getServer().computeOnServer(
                    server -> !LaunchSequence.isActive(server.overworld(), pos));
            require(sequenceCompleted, "Five-second translunar flight did not complete");
            boolean lunarExperienceReady = world.getServer().computeOnServer(server -> {
                var moon = server.getLevel(MOON_DIMENSION);
                var player = server.getPlayerList().getPlayers().get(0);
                return moon != null && player.level() == moon
                        && moon.getBlockState(new BlockPos(0, 5, 0)).is(Blocks.CONCRETE.gray())
                        && Math.abs(player.getGravity() - overworldGravity / 6.0) < 1.0e-9
                        && moon.getBlockState(new BlockPos(12, 5, 0)).isAir()
                        && moon.getBlockState(new BlockPos(12, 3, 0)).is(Blocks.TUFF)
                        && moon.getBlockState(new BlockPos(17, 6, 0)).is(Blocks.TUFF);
            });
            require(lunarExperienceReady,
                    "Moon landing did not provide one-sixth gravity, a safe landing pad, and generated craters");
            context.waitTicks(2);
            require(context.computeOnClient(client -> !FlightWindowHud.isActive()),
                    "Earth-receding flight window remained open after Moon arrival");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
