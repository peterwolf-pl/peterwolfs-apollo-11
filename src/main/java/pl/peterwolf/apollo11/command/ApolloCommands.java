package pl.peterwolf.apollo11.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import pl.peterwolf.apollo11.ModBlocks;
import pl.peterwolf.apollo11.ModItems;
import pl.peterwolf.apollo11.landing.LandingDifficulty;
import pl.peterwolf.apollo11.landing.LandingDifficultyStore;
import pl.peterwolf.apollo11.launch.LaunchSequence;

public final class ApolloCommands {
    private static final int GANTRY_SEARCH_RADIUS = 4;

    private ApolloCommands() {
    }

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("apollo")
                        .then(Commands.literal("launch")
                                .requires(source -> source.getEntity() instanceof ServerPlayer)
                                .executes(context -> launch(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("difficulty")
                                .requires(source -> source.getEntity() instanceof ServerPlayer)
                                .executes(context -> showDifficulty(context.getSource().getPlayerOrException()))
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .executes(context -> setDifficulty(
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "mode")))))
                        .then(Commands.literal("landing")
                                .requires(source -> source.getEntity() instanceof ServerPlayer)
                                .executes(context -> showLandingHelp(context.getSource().getPlayerOrException()))
                                .then(Commands.literal("burn")
                                        .executes(context -> LaunchSequence.performLandingBurn(
                                                context.getSource().getPlayerOrException()) ? 1 : 0)))));
    }

    private static int launch(ServerPlayer player) {
        if (!player.getMainHandItem().is(ModItems.APOLLO_ROCKET)) {
            player.sendSystemMessage(Component.translatable("command.peterwolfs_apollo11.launch.requires_rocket"));
            return 0;
        }

        BlockPos gantry = findNearbyGantry(player);
        if (gantry == null) {
            player.sendSystemMessage(Component.translatable("command.peterwolfs_apollo11.launch.no_gantry"));
            return 0;
        }

        return LaunchSequence.begin(player, (ServerLevel) player.level(), gantry, player.getMainHandItem()) ? 1 : 0;
    }

    private static int showLandingHelp(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("command.peterwolfs_apollo11.landing.usage"));
        return 1;
    }

    private static int showDifficulty(ServerPlayer player) {
        LandingDifficulty difficulty = LandingDifficultyStore.get(player);
        player.sendSystemMessage(Component.translatable("command.peterwolfs_apollo11.difficulty.current",
                Component.translatable("apollo.peterwolfs_apollo11.difficulty." + difficulty.id())));
        return 1;
    }

    private static int setDifficulty(ServerPlayer player, String mode) {
        LandingDifficulty difficulty;
        try {
            difficulty = LandingDifficulty.parse(mode);
        } catch (IllegalArgumentException exception) {
            player.sendSystemMessage(Component.translatable("command.peterwolfs_apollo11.difficulty.invalid"));
            return 0;
        }

        LandingDifficultyStore.set(player, difficulty);
        player.sendSystemMessage(Component.translatable("command.peterwolfs_apollo11.difficulty.set",
                Component.translatable("apollo.peterwolfs_apollo11.difficulty." + difficulty.id())));
        return 1;
    }

    private static BlockPos findNearbyGantry(ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        BlockPos closest = null;
        double closestDistance = GANTRY_SEARCH_RADIUS * GANTRY_SEARCH_RADIUS;

        for (int x = -GANTRY_SEARCH_RADIUS; x <= GANTRY_SEARCH_RADIUS; x++) {
            for (int y = -2; y <= 1; y++) {
                for (int z = -GANTRY_SEARCH_RADIUS; z <= GANTRY_SEARCH_RADIUS; z++) {
                    BlockPos candidate = origin.offset(x, y, z);
                    if (!player.level().getBlockState(candidate).is(ModBlocks.LAUNCH_GANTRY)) {
                        continue;
                    }
                    double distance = Vec3.atCenterOf(candidate).distanceToSqr(player.position());
                    if (distance <= closestDistance) {
                        closest = candidate;
                        closestDistance = distance;
                    }
                }
            }
        }

        return closest;
    }
}
