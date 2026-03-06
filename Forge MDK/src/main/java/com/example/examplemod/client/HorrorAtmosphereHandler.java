package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.GlitchEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Client-side horror atmosphere - makes the experience deeply unsettling
 * when the Glitch is nearby. You can feel it watching.
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class HorrorAtmosphereHandler {

    private static int ambientCooldown = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;

        Player player = mc.player;
        ambientCooldown--;

        // Find Glitches within 20 blocks
        AABB searchBox = player.getBoundingBox().inflate(20);
        List<GlitchEntity> nearbyGlitches = mc.level.getEntitiesOfClass(GlitchEntity.class, searchBox,
                glitch -> glitch.isAlive());

        if (!nearbyGlitches.isEmpty()) {
            // Unsettling ambient sounds when being watched
            if (ambientCooldown <= 0) {
                double closestDist = nearbyGlitches.stream()
                        .mapToDouble(g -> g.distanceTo(player))
                        .min()
                        .orElse(20);

                // Closer = more frequent, more intense
                int baseChance = (int) (200 - closestDist * 5);
                if (baseChance > 20 && player.getRandom().nextInt(Math.max(20, baseChance)) == 0) {
                    float volume = (float) (0.15 + 0.25 * (1 - closestDist / 20));
                    float pitch = 0.3f + player.getRandom().nextFloat() * 0.4f;

                    mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                            SoundEvents.SCULK_CATALYST_BLOOM,
                            SoundSource.AMBIENT, volume, pitch, false);

                    ambientCooldown = 60 + player.getRandom().nextInt(80);
                }
            }

            // Very rare - distorted whisper when very close
            if (ambientCooldown <= -100) {
                double closestDist = nearbyGlitches.stream()
                        .mapToDouble(g -> g.distanceTo(player))
                        .min()
                        .orElse(20);

                if (closestDist < 8 && player.getRandom().nextInt(500) == 0) {
                    mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                            SoundEvents.AMETHYST_BLOCK_CHIME,
                            SoundSource.AMBIENT, 0.3f, 0.1f, false);
                    ambientCooldown = 200;
                }
            }
        }
    }
}
