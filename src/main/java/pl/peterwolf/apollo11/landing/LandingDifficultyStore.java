package pl.peterwolf.apollo11.landing;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

public final class LandingDifficultyStore {
    private static final Map<UUID, LandingDifficulty> PLAYER_DIFFICULTIES = new HashMap<>();

    private LandingDifficultyStore() {
    }

    public static void initialize() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                PLAYER_DIFFICULTIES.remove(handler.player.getUUID()));
    }

    public static LandingDifficulty get(ServerPlayer player) {
        return PLAYER_DIFFICULTIES.getOrDefault(player.getUUID(), LandingDifficulty.EASY);
    }

    public static void set(ServerPlayer player, LandingDifficulty difficulty) {
        PLAYER_DIFFICULTIES.put(player.getUUID(), difficulty);
    }
}
