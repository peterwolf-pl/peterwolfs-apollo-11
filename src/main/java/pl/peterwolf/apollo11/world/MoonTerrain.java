package pl.peterwolf.apollo11.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class MoonTerrain {
    public static final BlockPos FIRST_CRATER_CENTER = new BlockPos(12, 5, 0);

    private static final int SURFACE_Y = 5;
    private static final Crater[] LANDING_CRATERS = {
            new Crater(12, 0, 4),
            new Crater(-18, 10, 7),
            new Crater(20, 25, 9)
    };

    private MoonTerrain() {
    }

    public static void prepareLandingArea(ServerLevel level) {
        for (Crater crater : LANDING_CRATERS) {
            int reach = crater.radius + 1;
            for (int x = crater.centerX - reach; x <= crater.centerX + reach; x++) {
                for (int z = crater.centerZ - reach; z <= crater.centerZ + reach; z++) {
                    double distance = Math.sqrt(square(x - crater.centerX) + square(z - crater.centerZ));
                    BlockPos column = new BlockPos(x, SURFACE_Y, z);
                    if (!hasNaturalColumn(level, x, z)) {
                        continue;
                    }

                    if (distance < crater.radius) {
                        int depth = (int) Math.round(2.0 * (1.0 - square(distance / crater.radius)));
                        if (depth <= 0) {
                            continue;
                        }
                        int floorY = SURFACE_Y - depth;
                        for (int y = floorY + 1; y <= SURFACE_Y; y++) {
                            level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                        }
                        level.setBlock(new BlockPos(x, floorY, z), Blocks.TUFF.defaultBlockState(), 2);
                    } else if (distance <= reach) {
                        level.setBlock(column.above(), Blocks.TUFF.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static boolean hasNaturalColumn(ServerLevel level, int x, int z) {
        for (int y = SURFACE_Y - 2; y <= SURFACE_Y + 1; y++) {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            if (!state.isAir() && !state.is(Blocks.CONCRETE.gray()) && !state.is(Blocks.TUFF)
                    && !state.is(Blocks.STONE) && !state.is(Blocks.BEDROCK)) {
                return false;
            }
        }
        return true;
    }

    private static double square(double value) {
        return value * value;
    }

    private record Crater(int centerX, int centerZ, int radius) {
    }
}
