package pl.peterwolf.apollo11.launch;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
import pl.peterwolf.apollo11.world.MoonDimension;

public final class LaunchSequence {
    private static final int COUNTDOWN_TICKS = 100;
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
        ACTIVE_LAUNCHES.put(key, new ActiveLaunch(level, player.getUUID(), COUNTDOWN_TICKS));
        player.sendSystemMessage(Component.literal("Launch sequence initiated. T-5 seconds."));
        emitExhaust(level, gantryPos, 8);
        return true;
    }

    public static boolean isActive(ServerLevel level, BlockPos gantryPos) {
        return ACTIVE_LAUNCHES.containsKey(new LaunchKey(level.dimension(), gantryPos));
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

            launch.ticksRemaining--;
            if (launch.ticksRemaining > 0 && launch.ticksRemaining % 20 == 0) {
                int secondsRemaining = launch.ticksRemaining / 20;
                notifyPlayer(server, launch.playerId, "T-" + secondsRemaining + "...");
                emitExhaust(launch.level, key.position(), 12);
            } else if (launch.ticksRemaining <= 0) {
                liftoff(server, key, launch);
                iterator.remove();
            }
        }
    }

    private static void liftoff(MinecraftServer server, LaunchKey key, ActiveLaunch launch) {
        ServerLevel level = launch.level;
        BlockPos pos = key.position();
        Vec3 center = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.CLOUD, center.x, pos.getY() + 1.0, center.z,
                50, 0.8, 0.2, 0.8, 0.08);
        level.sendParticles(ParticleTypes.FLAME, center.x, pos.getY() + 1.0, center.z,
                30, 0.45, 0.15, 0.45, 0.06);
        level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 1.0F, 0.75F);
        ServerPlayer player = server.getPlayerList().getPlayer(launch.playerId);
        ServerLevel moon = server.getLevel(MoonDimension.KEY);
        if (player == null) {
            return;
        }
        if (moon == null) {
            player.sendSystemMessage(Component.literal("Liftoff! Moon navigation is unavailable."));
            return;
        }

        moon.getChunk(0, 0);
        boolean arrived = player.teleportTo(moon, 0.5, 6.0, 0.5, Set.of(), player.getYRot(), player.getXRot(), false);
        if (arrived) {
            player.sendSystemMessage(Component.literal("Liftoff! Lunar landing complete."));
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
        private int ticksRemaining;

        private ActiveLaunch(ServerLevel level, UUID playerId, int ticksRemaining) {
            this.level = level;
            this.playerId = playerId;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
