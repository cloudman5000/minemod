package com.example.examplemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;

public class EscapeRiftBlock extends Block {

    public EscapeRiftBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            // Teleport the player back to the Overworld (ServerLevel.OVERWORLD)
            if (level instanceof ServerLevel serverLevel) {
                ServerLevel overworld = serverLevel.getServer().getLevel(Level.OVERWORLD);
                if (overworld != null) {
                    level.playSound(null, pos, SoundEvents.PORTAL_TRAVEL, SoundSource.BLOCKS, 1.0F, 1.0F);

                    BlockPos spawnPos = overworld.getSharedSpawnPos();
                    DimensionTransition transition = new DimensionTransition(
                            overworld,
                            spawnPos.getCenter(),
                            player.getDeltaMovement(),
                            player.getYRot(),
                            player.getXRot(),
                            DimensionTransition.DO_NOTHING);
                    player.changeDimension(transition);
                }
            }
        }
        super.stepOn(level, pos, state, entity);
    }
}
