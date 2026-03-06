package com.example.examplemod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * The Glitch - a horror entity that teleports erratically, stalks players,
 * and appears in flashes before striking. Unsettling and unpredictable.
 */
public class GlitchEntity extends Monster {

    private static final EntityDataAccessor<Integer> DATA_GLITCH_PHASE = SynchedEntityData.defineId(GlitchEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_MANIFESTING = SynchedEntityData.defineId(GlitchEntity.class, EntityDataSerializers.BOOLEAN);

    private int glitchCooldown = 0;
    private int manifestTimer = 0;
    private Vec3 lastTeleportPos = Vec3.ZERO;

    public GlitchEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_GLITCH_PHASE, 0);
        builder.define(DATA_IS_MANIFESTING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new GlitchTeleportGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (glitchCooldown > 0) glitchCooldown--;

            if (getEntityData().get(DATA_IS_MANIFESTING)) {
                manifestTimer++;
                if (manifestTimer >= 20) {
                    getEntityData().set(DATA_IS_MANIFESTING, false);
                    manifestTimer = 0;
                }
            }

            // Random glitch sounds - distorted, unsettling
            if (this.random.nextInt(80) == 0 && getTarget() != null) {
                playSound(SoundEvents.PORTAL_AMBIENT, 0.4f, 0.3f + this.random.nextFloat() * 0.5f);
            }
            if (this.random.nextInt(120) == 0) {
                playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5f, 0.2f);
            }
        }
    }

    public int getGlitchPhase() {
        return getEntityData().get(DATA_GLITCH_PHASE);
    }

    public void setGlitchPhase(int phase) {
        getEntityData().set(DATA_GLITCH_PHASE, Mth.clamp(phase, 0, 3));
    }

    public boolean isManifesting() {
        return getEntityData().get(DATA_IS_MANIFESTING);
    }

    public void setManifesting(boolean manifesting) {
        getEntityData().set(DATA_IS_MANIFESTING, manifesting);
        if (manifesting) manifestTimer = 0;
    }

    public boolean canGlitch() {
        return glitchCooldown <= 0;
    }

    public void setGlitchCooldown(int cooldown) {
        this.glitchCooldown = cooldown;
    }

    public Vec3 getLastTeleportPos() {
        return lastTeleportPos;
    }

    public void setLastTeleportPos(Vec3 pos) {
        this.lastTeleportPos = pos;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (super.doHurtTarget(target)) {
            playSound(SoundEvents.SCULK_SHRIEKER_HIT, 1.0f, 0.5f);
            setGlitchCooldown(40);
            return true;
        }
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SCULK_CATALYST_HIT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.AMETHYST_BLOCK_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SCULK_SHRIEKER_SHRIEK;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GlitchCooldown", glitchCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        glitchCooldown = tag.getInt("GlitchCooldown");
    }

    /**
     * Custom goal - teleports erratically toward the target, creating an unsettling chase
     */
    public static class GlitchTeleportGoal extends Goal {
        private final GlitchEntity glitch;
        private int attemptCounter;

        public GlitchTeleportGoal(GlitchEntity glitch) {
            this.glitch = glitch;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = glitch.getTarget();
            return target != null && target.isAlive() && glitch.canGlitch()
                    && glitch.distanceToSqr(target) > 9.0 && glitch.distanceToSqr(target) < 900.0
                    && glitch.random.nextInt(5) == 0;
        }

        @Override
        public void start() {
            attemptCounter = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = glitch.getTarget();
            if (target == null || !target.isAlive()) return;
            if (!glitch.canGlitch()) return;
            if (attemptCounter++ > 20) return;

            Level level = glitch.level();
            if (!(level instanceof ServerLevel serverLevel)) return;

            // Store current pos before "vanishing"
            glitch.setLastTeleportPos(glitch.position());
            glitch.setManifesting(true);
            glitch.setGlitchPhase(glitch.random.nextInt(4));

            // Teleport to a position near the target - sometimes behind them!
            Vec3 targetPos = target.position();
            double angle = target.getYRot() * (Math.PI / 180) + (glitch.random.nextBoolean() ? Math.PI : 0);
            double distance = 3 + glitch.random.nextDouble() * 4;
            double newX = targetPos.x + Mth.sin((float) angle) * distance;
            double newZ = targetPos.z + Mth.cos((float) angle) * distance;
            double newY = targetPos.y;

            BlockPos tryPos = BlockPos.containing(newX, newY, newZ);
            for (int i = 0; i < 10; i++) {
                BlockPos checkPos = tryPos.above(i);
                if (level.getBlockState(checkPos).isAir() && level.getBlockState(checkPos.above()).isAir()) {
                    if (glitch.random.nextFloat() < 0.7f) {
                        glitch.teleportTo(newX, checkPos.getY(), newZ);
                        glitch.setGlitchCooldown(30 + glitch.random.nextInt(40));
                        glitch.level().playSound(null, glitch.blockPosition(), SoundEvents.PORTAL_TRIGGER, 
                                glitch.getSoundSource(), 0.6f, 0.4f);
                        break;
                    }
                }
            }
        }
    }
}
