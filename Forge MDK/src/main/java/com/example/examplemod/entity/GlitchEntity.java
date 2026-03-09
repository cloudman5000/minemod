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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * The Glitch - a horror entity that teleports erratically, stalks players,
 * and appears in flashes before striking. Unsettling and unpredictable.
 */
public class GlitchEntity extends Monster {

    private static final EntityDataAccessor<Integer> DATA_GLITCH_PHASE = SynchedEntityData.defineId(GlitchEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_MANIFESTING = SynchedEntityData
            .defineId(GlitchEntity.class, EntityDataSerializers.BOOLEAN);

    private int glitchCooldown = 0;
    private int manifestTimer = 0;
    private Vec3 lastTeleportPos = Vec3.ZERO;

    public GlitchEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0) // Was 30. Much harder to kill.
                .add(Attributes.MOVEMENT_SPEED, 0.45) // Was 0.28. Very fast.
                .add(Attributes.ATTACK_DAMAGE, 12.0) // Was 6. Hurts a lot.
                .add(Attributes.ATTACK_KNOCKBACK, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 64.0); // Will track you from very far away
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
            if (glitchCooldown > 0)
                glitchCooldown--;

            if (getEntityData().get(DATA_IS_MANIFESTING)) {
                manifestTimer++;
                if (manifestTimer >= 20) {
                    getEntityData().set(DATA_IS_MANIFESTING, false);
                    manifestTimer = 0;
                }
            }

            // Random glitch sounds - distorted, unsettling
            if (this.random.nextInt(60) == 0 && getTarget() != null) {
                playSound(SoundEvents.PORTAL_AMBIENT, 0.4f, 0.1f + this.random.nextFloat() * 0.4f);
            }
            if (this.random.nextInt(90) == 0) {
                playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 0.1f + this.random.nextFloat() * 0.3f);
            }
            if (this.random.nextInt(200) == 0) {
                playSound(SoundEvents.WARDEN_HEARTBEAT, 1.5f, 0.5f);
            }

            // Stalking clues (only when they do not have a target yet)
            if (getTarget() == null && this.random.nextInt(800) == 0) {
                Player nearest = this.level().getNearestPlayer(this, 100);
                if (nearest != null) {
                    // Play a distant, scary sound directly to them
                    if (this.random.nextBoolean()) {
                        nearest.playSound(SoundEvents.AMBIENT_CAVE.value(), 1.0f, 0.1f);
                    } else {
                        nearest.playSound(SoundEvents.MUSIC_DISC_11.value(), 0.5f, 0.5f);
                    }

                    // Send a corrupted chat message
                    if (this.random.nextInt(3) == 0) {
                        String[] messages = {
                                "§kHe is coming§r...",
                                "§cdon't look back§r",
                                "§k10010101§r W§kH§rY §k01010101§r",
                                "i see you"
                        };
                        nearest.sendSystemMessage(net.minecraft.network.chat.Component
                                .literal(messages[this.random.nextInt(messages.length)]));
                    }
                }
            }

            // Occasional terrifying micro-teleport jittering
            if (this.random.nextInt(40) == 0 && getTarget() != null && !isManifesting() && canGlitch()) {
                double jX = this.getX() + (this.random.nextDouble() - 0.5) * 2.5;
                double jY = this.getY() + (this.random.nextDouble() - 0.5) * 1.5;
                double jZ = this.getZ() + (this.random.nextDouble() - 0.5) * 2.5;
                this.teleportTo(jX, jY, jZ);
                this.setGlitchPhase(this.random.nextInt(4));
            }
        } else {
            // CLIENT SIDE LOGIC
            Player player = Minecraft.getInstance().player;
            if (player != null && this.distanceTo(player) < 25 && this.random.nextInt(1200) == 0) {
                // Randomly open the fake permission screen if they are somewhat close but not
                // in immediate combat
                if (Minecraft.getInstance().screen == null) {
                    Minecraft.getInstance().setScreen(new com.example.examplemod.client.FakePermissionScreen());
                }
            }
        }

        // SERVER SIDE NAUSEA EFFECT (Wavy Screen)
        if (!this.level().isClientSide && this.isAlive()) {
            Player nearest = this.level().getNearestPlayer(this, 8); // Pretty close
            if (nearest != null) {
                // Apply a strong nausea effect that refreshes to keep the screen wavy and
                // distorted
                nearest.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.CONFUSION, 100, 1, true, false, false));
            }
        }
    }

    public static boolean checkGlitchSpawnRules(EntityType<GlitchEntity> glitch, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        // Can spawn anywhere monsters normally spawn, but disregards light level for
        // maximum creepiness.
        return checkMobSpawnRules(glitch, level, spawnType, pos, random);
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
        if (manifesting)
            manifestTimer = 0;
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

            if (target instanceof LivingEntity living) {
                // Inflict intense blindness for 5 seconds when it hits you
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.BLINDNESS, 100, 0, false, true));
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DARKNESS, 100, 0, false, true));
            }

            setGlitchCooldown(10 + this.random.nextInt(15)); // Teleport away much faster after a hit
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
     * Custom goal - teleports erratically toward the target, creating an unsettling
     * chase
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
            if (target == null || !target.isAlive())
                return;
            if (!glitch.canGlitch())
                return;
            if (attemptCounter++ > 20)
                return;

            Level level = glitch.level();
            if (!(level instanceof ServerLevel serverLevel))
                return;

            // Store current pos before "vanishing"
            glitch.setLastTeleportPos(glitch.position());
            glitch.setManifesting(true);
            glitch.setGlitchPhase(glitch.random.nextInt(4));

            // Teleport to a position near the target - heavily favoring directly behind
            // them!
            Vec3 targetPos = target.position();
            double angle;
            if (glitch.random.nextFloat() < 0.6f) {
                // 60% chance to teleport in the 180 degree arc behind the player
                angle = target.getYRot() * (Math.PI / 180) + Math.PI
                        + (glitch.random.nextDouble() - 0.5) * (Math.PI / 2);
            } else {
                angle = target.getYRot() * (Math.PI / 180) + (glitch.random.nextBoolean() ? Math.PI : 0);
            }

            double distance = 1.5 + glitch.random.nextDouble() * 3.5; // Closer! Was 3 to 7, now 1.5 to 5.
            double newX = targetPos.x + Mth.sin((float) angle) * distance;
            double newZ = targetPos.z + Mth.cos((float) angle) * distance;
            double newY = targetPos.y;

            BlockPos tryPos = BlockPos.containing(newX, newY, newZ);
            for (int i = 0; i < 10; i++) {
                BlockPos checkPos = tryPos.above(i);
                if (level.getBlockState(checkPos).isAir() && level.getBlockState(checkPos.above()).isAir()) {
                    if (glitch.random.nextFloat() < 0.85f) { // Increased chance to successfully port
                        glitch.teleportTo(newX, checkPos.getY(), newZ);
                        // Face the player immediately
                        glitch.lookAt(target, 180.0f, 180.0f);
                        glitch.setYHeadRot(glitch.getYRot());
                        glitch.setYBodyRot(glitch.getYRot());
                        glitch.setGlitchCooldown(15 + glitch.random.nextInt(25)); // Much shorter cooldown. Relentless.
                        glitch.level().playSound(null, glitch.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                                glitch.getSoundSource(), 1.0f, 0.3f);
                        break;
                    }
                }
            }
        }
    }
}
