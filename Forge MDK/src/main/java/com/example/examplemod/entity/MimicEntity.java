package com.example.examplemod.entity;

import com.example.examplemod.effect.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class MimicEntity extends Monster {

    private static final EntityDataAccessor<Boolean> IS_AGGRESSIVE = SynchedEntityData.defineId(MimicEntity.class,
            EntityDataSerializers.BOOLEAN);
    private int revealTicks = 0; // Animation timer for reveal

    public MimicEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D) // Slow like a pig initially
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_AGGRESSIVE, false);
    }

    public boolean isAggressive() {
        return this.entityData.get(IS_AGGRESSIVE);
    }

    public void setAggressive(boolean aggressive) {
        this.entityData.set(IS_AGGRESSIVE, aggressive);
        if (aggressive) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.4D); // Speed up
            // Jumpscare sound
            this.playSound(SoundEvents.ENDERMAN_SCREAM, 1.5F, 0.5F);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // Passive goals (when not aggressive)
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));

        // Aggressive goals
        this.goalSelector.addGoal(1, new MimicAttackGoal(this, 1.2D, false));
        this.targetSelector.addGoal(1, new MimicTargetGoal(this, Player.class, true));
    }

    class MimicAttackGoal extends MeleeAttackGoal {
        public MimicAttackGoal(MimicEntity pMob, double pSpeedModifier, boolean pFollowingTargetEvenIfNotSeen) {
            super(pMob, pSpeedModifier, pFollowingTargetEvenIfNotSeen);
        }

        @Override
        public boolean canUse() {
            return super.canUse() && MimicEntity.this.isAggressive();
        }
    }

    class MimicTargetGoal extends NearestAttackableTargetGoal<Player> {
        public MimicTargetGoal(MimicEntity pMob, Class<Player> pTargetType, boolean pMustSee) {
            super(pMob, pTargetType, pMustSee);
        }

        @Override
        public boolean canUse() {
            return super.canUse() && MimicEntity.this.isAggressive();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && !this.isAggressive()) {
            // Check for nearby players to trigger the trap
            AABB scanArea = this.getBoundingBox().inflate(3.5D); // 3.5 block radius
            List<Player> players = this.level().getEntitiesOfClass(Player.class, scanArea);
            if (!players.isEmpty()) {
                this.setAggressive(true);
            }
        }

        if (this.level().isClientSide() && this.isAggressive()) {
            // Spooky particles when aggressive
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.LARGE_SMOKE,
                        this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D),
                        0.0D, 0.05D, 0.0D);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity livingTarget) {
            // Applies paranoia
            livingTarget.addEffect(new MobEffectInstance(ModEffects.PARANOIA.getHolder().get(), 200, 0));
        }
        return hurt;
    }

    // Dynamic sounds based on state
    @Override
    protected SoundEvent getAmbientSound() {
        return this.isAggressive() ? SoundEvents.ZOMBIE_AMBIENT : SoundEvents.PIG_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return this.isAggressive() ? SoundEvents.ZOMBIE_HURT : SoundEvents.PIG_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isAggressive() ? SoundEvents.ZOMBIE_DEATH : SoundEvents.PIG_DEATH;
    }
}
