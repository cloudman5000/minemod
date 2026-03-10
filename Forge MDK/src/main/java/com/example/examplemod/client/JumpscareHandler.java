package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.GlitchEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class JumpscareHandler {

    private static final ResourceLocation JUMPSCARE_TEXTURE = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID,
            "textures/entity/glitch.png");
    private static long jumpscareEndTime = 0;

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        // Trigger only on client and if configured to allow loud jumpscares
        if (!event.getEntity().level().isClientSide
                || !com.example.examplemod.config.ExampleModConfig.ENABLE_JUMPSCARES.get())
            return;

        if (event.getEntity() instanceof Player player) {
            Minecraft mc = Minecraft.getInstance();
            if (player == mc.player) {
                // If killed by a Glitch, trigger jumpscare
                if (event.getSource().getEntity() instanceof GlitchEntity) {
                    jumpscareEndTime = System.currentTimeMillis() + 3000; // 3 seconds of jumpscare
                    mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENDERMAN_SCREAM, // Scarier sound
                            SoundSource.HOSTILE, 3.0f, 0.1f, false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent.Post event) {
        if (System.currentTimeMillis() < jumpscareEndTime) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null)
                return;
            GuiGraphics graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            int width = mc.getWindow().getGuiScaledWidth();
            int height = mc.getWindow().getGuiScaledHeight();

            RenderSystem.enableBlend();

            // Aggressive jittering for the image
            int offsetX = (mc.player.tickCount % 2 == 0) ? (15 + (int) (Math.random() * 20))
                    : -(15 + (int) (Math.random() * 20));
            int offsetY = (mc.player.tickCount % 3 == 0) ? (15 + (int) (Math.random() * 20))
                    : -(15 + (int) (Math.random() * 20));

            // Occasionally invert colors or tint red
            if (Math.random() > 0.7) {
                graphics.setColor(1.0f, 0.0f, 0.0f, 1.0f); // Fast red flashes
            } else {
                graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            }

            // Scale and map the face UVs across the whole screen!
            // The face we painted earlier is at x:8, y:8, w:8, h:8 in a 64x64 texture

            graphics.blit(JUMPSCARE_TEXTURE,
                    offsetX, offsetY,
                    width + Math.abs(offsetX) * 2, height + Math.abs(offsetY) * 2, // draw larger than screen to cover
                                                                                   // jitter gaps
                    8, 8, // face U, V
                    8, 8, // face width, height in texture
                    64, 64 // total texture dims
            );

            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f); // Reset color
            graphics.flush();
            RenderSystem.disableBlend();
        }
    }
}
