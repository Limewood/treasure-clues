package com.buncord.treasureclues;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldHeightHelper {
    private static final Logger LOGGER = LogManager.getLogger();
    public static BlockPos getOverworldSafePositionAt(ServerLevel serverLevel, int x, int z, boolean waterIsSafe) {
        LevelChunk levelchunk = serverLevel.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        LOGGER.error("Min build height: " + serverLevel.getMinBuildHeight());
        int motionBlockingHeight = levelchunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15);
        LOGGER.error("Motion blocking height: " + motionBlockingHeight);
        if (motionBlockingHeight >= serverLevel.getMinBuildHeight()) {
            int worldSurfaceHeight = levelchunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
            LOGGER.error("World surface height: " + worldSurfaceHeight);
            int oceanFloorHeight = levelchunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x & 15, z & 15);
            LOGGER.error("Ocean floor height: " + oceanFloorHeight);
            if (worldSurfaceHeight >= motionBlockingHeight || waterIsSafe) {
                BlockPos.MutableBlockPos mutableblockpos = new BlockPos.MutableBlockPos();

                for (int k = motionBlockingHeight + 1; k >= serverLevel.getMinBuildHeight(); --k) {
                    mutableblockpos.set(x, k, z);
                    BlockState blockstate = serverLevel.getBlockState(mutableblockpos);
                    if (!waterIsSafe && !blockstate.getFluidState().isEmpty()) {
                        break;
                    }

                    if (Block.isFaceFull(blockstate.getCollisionShape(serverLevel, mutableblockpos), Direction.UP)) {
                        return mutableblockpos.above().immutable();
                    }
                }
            }
        }
        return null;
    }
}
