package com.example.examplemod.item;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.GlitchEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Spawn egg that looks up the entity type from registry when used,
 * avoiding the chicken-egg problem (items register before entity types).
 */
public class GlitchSpawnEggItem extends Item {

    private static final ResourceLocation GLITCH_ID = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "glitch");

    public GlitchSpawnEggItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        EntityType<?> rawType = BuiltInRegistries.ENTITY_TYPE.getOptional(GLITCH_ID).orElse(null);
        if (rawType == null) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        @SuppressWarnings("unchecked")
        EntityType<? extends Mob> type = (EntityType<? extends Mob>) rawType;
        BlockPos pos = context.getClickedPos();
        Vec3 spawnPos = Vec3.atCenterOf(pos.above());
        Mob mob = type.create(level);
        if (mob != null) {
            mob.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, 0, 0);
            mob.finalizeSpawn((ServerLevel) level, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWN_EGG, (SpawnGroupData) null);
            level.addFreshEntity(mob);
            context.getItemInHand().shrink(1);
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
}
