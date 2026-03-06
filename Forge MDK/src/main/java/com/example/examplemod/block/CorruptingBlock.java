package com.example.examplemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;

/**
 * A block that slowly corrupts the world around it - spreading like a virus.
 * Converts nearby blocks into more corruption. Unstoppable. Terrifying.
 */
public class CorruptingBlock extends Block {

    // Blocks that can be corrupted - they become this block
    private static final List<Block> CORRUPTIBLE_BLOCKS = List.of(
            Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.STONE, Blocks.DEEPSLATE,
            Blocks.COBBLESTONE, Blocks.SAND, Blocks.SANDSTONE, Blocks.GRAVEL,
            Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.MYCELIUM
    );

    public CorruptingBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(2.0f, 6.0f)
                .sound(SoundType.SCULK)
                .lightLevel(state -> 3)
                .randomTicks());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isClientSide) return;

        // Spread corruption - 15% chance per tick to attempt spread
        if (random.nextFloat() < 0.15f) {
            trySpreadCorruption(level, pos, random);
        }
    }

    private void trySpreadCorruption(ServerLevel level, BlockPos center, RandomSource random) {
        // Pick a random adjacent block
        int offsetX = random.nextInt(5) - 2;  // -2 to +2
        int offsetY = random.nextInt(5) - 2;
        int offsetZ = random.nextInt(5) - 2;

        if (offsetX == 0 && offsetY == 0 && offsetZ == 0) return;

        BlockPos targetPos = center.offset(offsetX, offsetY, offsetZ);
        BlockState targetState = level.getBlockState(targetPos);

        for (Block corruptible : CORRUPTIBLE_BLOCKS) {
            if (targetState.is(corruptible)) {
                level.setBlock(targetPos, defaultBlockState(), Block.UPDATE_ALL);
                level.levelEvent(1501, targetPos, 0); // Sculk-like particles
                break;
            }
        }
    }
}
