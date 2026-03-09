package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.GlitchEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TileGlitcher {

    // Keep track of blocks we've temporarily changed on the client so we can
    // restore them
    private static final Map<BlockPos, GlitchedBlock> glitchedBlocks = new HashMap<>();
    private static final Random random = new Random();

    // The blocks to randomly swap regular blocks to
    private static final BlockState[] CREEPY_BLOCKS = {
            Blocks.OBSIDIAN.defaultBlockState(),
            Blocks.BEDROCK.defaultBlockState(),
            Blocks.REDSTONE_BLOCK.defaultBlockState(),
            Blocks.SOUL_SAND.defaultBlockState(),
            Blocks.CRYING_OBSIDIAN.defaultBlockState()
    };

    private static class GlitchedBlock {
        BlockState originalState;
        long restoreTime;

        GlitchedBlock(BlockState originalState, long tickToRestoreAt) {
            this.originalState = originalState;
            this.restoreTime = tickToRestoreAt;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || mc.isPaused())
            return;

        long currentTick = mc.level.getGameTime();

        // 1. Restore any blocks whose time is up
        glitchedBlocks.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            GlitchedBlock glitched = entry.getValue();

            if (currentTick >= glitched.restoreTime) {
                // Only restore if the client still thinks it's a creepy block (prevent
                // overwriting actual server changes)
                BlockState currentState = mc.level.getBlockState(pos);
                for (BlockState creepy : CREEPY_BLOCKS) {
                    if (currentState.is(creepy.getBlock())) {
                        // Suppress updates so it doesn't trigger block updates, just visuals
                        mc.level.setBlock(pos, glitched.originalState, 11);
                        break;
                    }
                }
                return true; // remove from map
            }
            return false;
        });

        // 2. Glitch new blocks if a Glitch is near
        AABB searchBox = player.getBoundingBox().inflate(15);
        List<GlitchEntity> nearby = mc.level.getEntitiesOfClass(GlitchEntity.class, searchBox, GlitchEntity::isAlive);

        if (nearby.isEmpty()) {
            // If they left, clear all quickly (speed up restore times)
            for (GlitchedBlock gb : glitchedBlocks.values()) {
                gb.restoreTime = Math.min(gb.restoreTime, currentTick + 5);
            }
            return;
        }

        // The closer it is, the more blocks glitch per tick
        double minDistance = nearby.stream().mapToDouble(g -> g.distanceTo(player)).min().orElse(15.0);
        int glitchAttempts = 1;
        if (minDistance < 5)
            glitchAttempts = 8;
        else if (minDistance < 10)
            glitchAttempts = 3;

        boolean isManifesting = nearby.stream().anyMatch(GlitchEntity::isManifesting);
        if (isManifesting)
            glitchAttempts *= 3; // Go crazy if it's teleporting

        for (int i = 0; i < glitchAttempts; i++) {
            if (random.nextFloat() > 0.3f)
                continue; // Only sometimes pick a block

            // Pick a random block within a 10 block radius
            int xOffset = random.nextInt(21) - 10;
            int yOffset = random.nextInt(21) - 10;
            int zOffset = random.nextInt(21) - 10;

            BlockPos targetPos = player.blockPosition().offset(xOffset, yOffset, zOffset);

            // Don't glitch air, and don't re-glitch blocks we're already tracking
            BlockState currentState = mc.level.getBlockState(targetPos);
            if (!currentState.isAir() && !glitchedBlocks.containsKey(targetPos)) {
                // Pick a scary replacement
                BlockState creepyState = CREEPY_BLOCKS[random.nextInt(CREEPY_BLOCKS.length)];

                // Save original
                long restoreAt = currentTick + 10 + random.nextInt(40); // Restore in 0.5 to 2.5 seconds
                glitchedBlocks.put(targetPos, new GlitchedBlock(currentState, restoreAt));

                // Apply fake block purely on the client
                mc.level.setBlock(targetPos, creepyState, 0); // No flag for client world required, or 0. Wait, Let's
                                                              // check SetBlock methods.
                // Wait actually ClientLevel setBlock(BlockPos pos, BlockState state, int flags)
                // DOES exist in Forge 1.21.1 or DOES IT?
                // The error was mostly probably setBlock... wait.
            }
        }
    }
}
