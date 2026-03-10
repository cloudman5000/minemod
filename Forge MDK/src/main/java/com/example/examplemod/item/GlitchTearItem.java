package com.example.examplemod.item;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

public class GlitchTearItem extends Item {

    public static final ResourceKey<Level> NIGHTMARE_KEY = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "nightmare"));

    public GlitchTearItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            ServerLevel targetLevel = null;

            // Toggle between Nightmare and Overworld
            if (level.dimension().equals(NIGHTMARE_KEY)) {
                targetLevel = level.getServer().getLevel(Level.OVERWORLD);
            } else {
                targetLevel = level.getServer().getLevel(NIGHTMARE_KEY);
            }

            if (targetLevel != null) {
                // Play creepy sound before warping
                level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.5F);

                // Find a safe spawn point in the target level
                Vec3 spawnPos = new Vec3(targetLevel.getSharedSpawnPos().getX(), targetLevel.getSharedSpawnPos().getY(),
                        targetLevel.getSharedSpawnPos().getZ());

                if (targetLevel.dimension().equals(Level.OVERWORLD)) {
                    spawnPos = new Vec3(targetLevel.getSharedSpawnPos().getX(),
                            targetLevel.getSharedSpawnPos().getY() + 1, targetLevel.getSharedSpawnPos().getZ());
                } else {
                    // Try to find highest block if going to Nightmare (which might be falling into
                    // void otherwise)
                    spawnPos = new Vec3(player.getX(), 150, player.getZ());
                }

                // Teleport the player
                DimensionTransition transition = new DimensionTransition(
                        targetLevel,
                        spawnPos,
                        player.getDeltaMovement(),
                        player.getYRot(),
                        player.getXRot(),
                        DimensionTransition.DO_NOTHING);

                player.changeDimension(transition);

                // If consumed on use (optional)
                // itemStack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }
}
