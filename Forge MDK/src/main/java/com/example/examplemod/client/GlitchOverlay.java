package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.GlitchEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GlitchOverlay {

    // Use the actual glitch texture as static
    private static final ResourceLocation STATIC_TEXTURE = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID,
            "textures/entity/glitch.png");

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null)
            return;

        // Only show static if a glitch is nearby
        AABB searchBox = player.getBoundingBox().inflate(15);
        List<GlitchEntity> nearby = mc.level.getEntitiesOfClass(GlitchEntity.class, searchBox,
                GlitchEntity::isAlive);

        if (nearby.isEmpty())
            return;

        double minDistance = nearby.stream().mapToDouble(g -> g.distanceTo(player)).min().orElse(15.0);

        // The closer it is, the more intense the static. Max intensity at distance <= 5
        float intensity = (float) Math.max(0, 1.0 - (minDistance - 5) / 10.0);
        if (intensity <= 0)
            return;

        // Optional: If the glitch is actively manifesting/teleporting, spike the
        // intensity
        boolean anyManifesting = nearby.stream().anyMatch(GlitchEntity::isManifesting);
        if (anyManifesting) {
            intensity = Math.min(1.0f, intensity + 0.6f); // Spike intensity heavily
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        GuiGraphics guiGraphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());

        RenderSystem.enableBlend();

        // Occasional full blackouts if very close or manifesting
        if ((minDistance < 4 || anyManifesting) && Math.random() < 0.1) {
            guiGraphics.fill(0, 0, screenWidth, screenHeight, 0xFF000000); // Black screen
        }

        // Draw random horizontal "tears" or static blocks
        long time = mc.level.getGameTime();
        int seed = (int) (time % 100);

        // Draw multiple thin strips to simulate screen tearing/static
        int numTears = (int) (intensity * 35); // Many more tears
        for (int i = 0; i < numTears; i++) {
            int y = (seed * 17 + i * 31) % screenHeight;
            int height = 2 + (seed * i % 12);

            // Randomly offset the X texture coordinate
            float uOffset = ((seed * i * 7) % 64) / 64.0f;
            float vOffset = ((time * 3 + i * 11) % 64) / 64.0f;

            guiGraphics.setColor(1.0f, 1.0f, 1.0f, intensity * 0.5f);

            guiGraphics.blit(STATIC_TEXTURE,
                    0, y, // screen X, Y
                    screenWidth, height, // width, height on screen
                    uOffset * 64, vOffset * 64, // texture U, V
                    screenWidth, height, // width, height of texture region
                    64, 64 // total texture width/height
            );
        }
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        guiGraphics.flush();
        RenderSystem.disableBlend();
    }
}
