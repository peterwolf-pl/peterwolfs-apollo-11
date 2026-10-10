package pl.peterwolf.apollo11.launch;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import pl.peterwolf.apollo11.ModBlocks;
import pl.peterwolf.apollo11.ModItems;
import pl.peterwolf.apollo11.landing.LandingDifficulty;
import pl.peterwolf.apollo11.landing.LandingDifficulty.LandingOutcome;
import pl.peterwolf.apollo11.landing.LandingDifficultyStore;
import pl.peterwolf.apollo11.network.FlightStartPayload;
import pl.peterwolf.apollo11.world.MoonDimension;
import pl.peterwolf.apollo11.world.MoonTerrain;

public final class LaunchSequence {
    private static final int COUNTDOWN_TICKS = 100;
    private static final int FLIGHT_DURATION_TICKS = 100;
    private static final Map<LaunchKey, ActiveLaunch> ACTIVE_LAUNCHES = new HashMap<>();

    private LaunchSequence() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(LaunchSequence::tick);
    }

    public static boolean begin(ServerPlayer player, ServerLevel level, BlockPos gantryPos, ItemStack rocketStack) {
        if (!rocketStack.is(ModItems.APOLLO_ROCKET)) {
            return false;
        }
        if (level.getServer().getLevel(MoonDimension.KEY) == null) {
            player.sendSystemMessage(Component.literal("Launch unavailable: the Moon dimension is not loaded."));
            return false;
        }

        LaunchKey key = new LaunchKey(level.dimension(), gantryPos);
        if (ACTIVE_LAUNCHES.containsKey(key)) {
            player.sendSystemMessage(Component.literal("Launch sequence already in progress."));
            return false;
        }

        if (!player.getAbilities().instabuild) {
            rocketStack.shrink(1);
        }
        ACTIVE_LAUNCHES.put(key, new ActiveLaunch(level, player.getUUID(), COUNTDOWN_TICKS,
                LandingDifficultyStore.get(player), player.position(), player.getYRot(), player.getXRot()));
        player.sendSystemMessage(Component.literal("Launch sequence initiated. T-5 seconds."));
        emitExhaust(level, gantryPos, 8);
        return true;
    }

    public static boolean isActive(ServerLevel level, BlockPos gantryPos) {
        return ACTIVE_LAUNCHES.containsKey(new LaunchKey(level.dimension(), gantryPos));
    }

    public static boolean performLandingBurn(ServerPlayer player) {
        for (ActiveLaunch launch : ACTIVE_LAUNCHES.values()) {
            if (!launch.playerId.equals(player.getUUID()) || !launch.flightStarted) {
                continue;
            }
            if (launch.difficulty == LandingDifficulty.EASY) {
                player.sendSystemMessage(Component.translatable("landing.peterwolfs_apollo11.burn.autopilot"));
                return false;
            }
            if (launch.manualBurnApplied) {
                player.sendSystemMessage(Component.translatable("landing.peterwolfs_apollo11.burn.already"));
                return true;
            }

            launch.manualBurnApplied = true;
            player.sendSystemMessage(Component.translatable("landing.peterwolfs_apollo11.burn.accepted"));
            return true;
        }
        player.sendSystemMessage(Component.translatable("landing.peterwolfs_apollo11.burn.no_descent"));
        return false;
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<LaunchKey, ActiveLaunch>> iterator = ACTIVE_LAUNCHES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LaunchKey, ActiveLaunch> entry = iterator.next();
            LaunchKey key = entry.getKey();
            ActiveLaunch launch = entry.getValue();

            if (!launch.level.getBlockState(key.position()).is(ModBlocks.LAUNCH_GANTRY)) {
                notifyPlayer(server, launch.playerId, "Launch cancelled: the gantry is no longer operational.");
                iterator.remove();
                continue;
            }

            if (!launch.flightStarted) {
                launch.ticksRemaining--;
                if (launch.ticksRemaining > 0 && launch.ticksRemaining % 20 == 0) {
                    int secondsRemaining = launch.ticksRemaining / 20;
                    notifyPlayer(server, launch.playerId, "T-" + secondsRemaining + "...");
                    emitExhaust(launch.level, key.position(), 12);
                } else if (launch.ticksRemaining <= 0) {
                    beginFlight(server, key, launch);
                    launch.flightStarted = true;
                    launch.ticksRemaining = FLIGHT_DURATION_TICKS;
                }
            } else if (--launch.ticksRemaining <= 0) {
                liftoff(server, launch);
                iterator.remove();
            }
        }
    }

    private static void beginFlight(MinecraftServer server, LaunchKey key, ActiveLaunch launch) {
        BlockPos pos = key.position();
        Vec3 center = Vec3.atCenterOf(pos);
        launch.level.sendParticles(ParticleTypes.CLOUD, center.x, pos.getY() + 1.0, center.z,
                50, 0.8, 0.2, 0.8, 0.08);
        launch.level.sendParticles(ParticleTypes.FLAME, center.x, pos.getY() + 1.0, center.z,
                30, 0.45, 0.15, 0.45, 0.06);
        launch.level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 1.0F, 0.75F);
        ServerPlayer player = server.getPlayerList().getPlayer(launch.playerId);
        if (player != null) {
            ServerPlayNetworking.send(player, new FlightStartPayload());
            player.sendSystemMessage(Component.literal("Liftoff! Earth is receding through the capsule window."));
            player.sendSystemMessage(Component.translatable(
                    "landing.peterwolfs_apollo11.approach." + launch.difficulty.id()));
        }
    }

    private static void liftoff(MinecraftServer server, ActiveLaunch launch) {
        ServerPlayer player = server.getPlayerList().getPlayer(launch.playerId);
        ServerLevel moon = server.getLevel(MoonDimension.KEY);
        if (player == null) {
            return;
        }
        LandingOutcome outcome = launch.difficulty.resolveLanding(launch.manualBurnApplied);
        if (outcome == LandingOutcome.ABORTED) {
            player.teleportTo(launch.level, launch.origin.x, launch.origin.y, launch.origin.z,
                    Set.of(), launch.yRot, launch.xRot, false);
            player.sendSystemMessage(Component.translatable("landing.peterwolfs_apollo11.abort"));
            return;
        }
        if (moon == null) {
            player.sendSystemMessage(Component.literal("Liftoff! Moon navigation is unavailable."));
            return;
        }

        moon.getChunk(0, 0);
        MoonTerrain.prepareLandingArea(moon);
        boolean arrived = player.teleportTo(moon, 0.5, 6.0, 0.5, Set.of(), player.getYRot(), player.getXRot(), false);
        if (arrived) {
            String resultKey = switch (outcome) {
                case AUTOPILOT -> "landing.peterwolfs_apollo11.result.autopilot";
                case ASSISTED_AUTOPILOT -> "landing.peterwolfs_apollo11.result.assisted";
                case ASSISTED_MANUAL -> "landing.peterwolfs_apollo11.result.assisted_manual";
                case MANUAL -> "landing.peterwolfs_apollo11.result.manual";
                case ABORTED -> throw new IllegalStateException("Aborted landings are handled before transfer");
            };
            player.sendSystemMessage(Component.translatable(resultKey));
        } else {
            player.sendSystemMessage(Component.literal("Liftoff! The Moon landing transfer failed."));
        }
    }

    private static void emitExhaust(ServerLevel level, BlockPos pos, int count) {
        Vec3 center = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.SMOKE, center.x, pos.getY() + 0.8, center.z,
                count, 0.35, 0.05, 0.35, 0.01);
        level.sendParticles(ParticleTypes.FLAME, center.x, pos.getY() + 0.35, center.z,
                Math.max(1, count / 2), 0.2, 0.05, 0.2, 0.02);
    }

    private static void notifyPlayer(MinecraftServer server, UUID playerId, String message) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    private record LaunchKey(ResourceKey<Level> dimension, BlockPos position) {
        private LaunchKey {
            position = position.immutable();
        }
    }

    private static final class ActiveLaunch {
        private final ServerLevel level;
        private final UUID playerId;
        private final LandingDifficulty difficulty;
        private final Vec3 origin;
        private final float yRot;
        private final float xRot;
        private int ticksRemaining;
        private boolean flightStarted;
        private boolean manualBurnApplied;

        private ActiveLaunch(ServerLevel level, UUID playerId, int ticksRemaining, LandingDifficulty difficulty,
                             Vec3 origin, float yRot, float xRot) {
            this.level = level;
            this.playerId = playerId;
            this.ticksRemaining = ticksRemaining;
            this.difficulty = difficulty;
            this.origin = origin;
            this.yRot = yRot;
            this.xRot = xRot;
        }
    }
}
