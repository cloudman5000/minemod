package com.example.examplemod.entity;

import com.example.examplemod.worldgen.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;

public class SignerEntity extends Monster {
    private int ambushTimer = 0;
    private Player targetPlayer;

    public SignerEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public void setTargetPlayer(Player player) {
        this.targetPlayer = player;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getRandomX(0.5D), this.getRandomY(),
                    this.getRandomZ(0.5D), 0.0D, 0.0D, 0.0D);
            return;
        }

        if (targetPlayer != null) {
            this.lookAt(targetPlayer, 180.0F, 180.0F);
            this.setYRot(this.yHeadRot);
            this.yBodyRot = this.getYRot();

            if (ambushTimer == 0) {
                this.playSound(SoundEvents.ENDERMAN_SCREAM, 3.0F, 0.1F);
                this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 3.0F, 0.5F);
            }

            ambushTimer++;

            if (ambushTimer >= 30) { // 1.5 seconds of intense staring
                if (targetPlayer.level() instanceof ServerLevel serverLevel) {
                    ServerLevel nightmareLevel = serverLevel.getServer().getLevel(ModDimensions.NIGHTMARE_LEVEL_KEY);
                    if (nightmareLevel != null) {
                        // Forcibly change dimension (using 1.21.1 DimensionTransition)
                        DimensionTransition transition = new DimensionTransition(
                                nightmareLevel,
                                targetPlayer.position().add(0, 50, 0), // Drop them in from the sky slightly
                                targetPlayer.getDeltaMovement(),
                                targetPlayer.getYRot(),
                                targetPlayer.getXRot(),
                                DimensionTransition.DO_NOTHING);
                        targetPlayer.changeDimension(transition);
                    }
                }
                this.discard();
            }
        } else if (ambushTimer > 0) { // If target disconnected or died
            this.discard();
        }
    }
}
