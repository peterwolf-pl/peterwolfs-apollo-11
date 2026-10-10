package pl.peterwolf.apollo11.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import pl.peterwolf.apollo11.ModBlocks;
import pl.peterwolf.apollo11.ModItems;
import pl.peterwolf.apollo11.landing.LandingDifficulty;
import pl.peterwolf.apollo11.landing.LandingDifficultyStore;
import pl.peterwolf.apollo11.launch.LaunchSequence;

public final class ProfessionalAbortGameTest implements FabricClientGameTest {
    private static final BlockPos GANTRY_POS = new BlockPos(0, 80, 0);
    private static final Vec3 LAUNCH_ORIGIN = new Vec3(0.5, 81.0, 0.5);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(true).create()) {
            world.getConnection().waitForChunksDownload();
            boolean started = world.getServer().computeOnServer(server -> {
                var level = server.overworld();
                level.setBlock(GANTRY_POS, ModBlocks.LAUNCH_GANTRY.defaultBlockState(), 3);
                var player = server.getPlayerList().getPlayers().get(0);
                player.teleportTo(LAUNCH_ORIGIN.x, LAUNCH_ORIGIN.y, LAUNCH_ORIGIN.z);
                player.setGameMode(GameType.SURVIVAL);
                LandingDifficultyStore.set(player, LandingDifficulty.PROFESSIONAL);
                player.setItemInHand(InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(
                        ModItems.APOLLO_ROCKET));
                return LaunchSequence.begin(player, level, GANTRY_POS, player.getMainHandItem());
            });
            require(started, "The professional abort test could not start its launch sequence");

            context.waitTicks(205);
            String abortState = world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().get(0);
                return "dimension=" + player.level().dimension() + ", position=" + player.position()
                        + ", alive=" + player.isAlive() + ", sequenceActive="
                        + LaunchSequence.isActive(server.overworld(), GANTRY_POS);
            });
            boolean safelyAborted = world.getServer().computeOnServer(server -> {
                var player = server.getPlayerList().getPlayers().get(0);
                return player.level() == server.overworld()
                        && player.position().distanceToSqr(LAUNCH_ORIGIN) < 1.0
                        && player.isAlive()
                        && !LaunchSequence.isActive(server.overworld(), GANTRY_POS);
            });
            require(safelyAborted,
                    "A missed PROFESSIONAL burn must return the living player to the launch position: "
                            + abortState);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
