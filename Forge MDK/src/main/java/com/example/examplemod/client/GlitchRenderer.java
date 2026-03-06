package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.GlitchEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Glitch entity with unsettling visual effects:
 * - Flickering/glitching appearance
 * - Slight position jitter
 * - Distorted proportions when manifesting
 */
public class GlitchRenderer extends MobRenderer<GlitchEntity, HumanoidModel<GlitchEntity>> {

    private static final ResourceLocation GLITCH_TEXTURE = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "textures/entity/glitch.png");

    public GlitchRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(GlitchEntity entity) {
        return GLITCH_TEXTURE;
    }

    @Override
    public void render(GlitchEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Glitch effect: random jitter in position (unsettling movement)
        long seed = (long) (entity.getId() * 4989512) + (entity.tickCount * 31);
        double jitterX = ((seed % 100) / 100.0 - 0.5) * 0.08;
        double jitterZ = (((seed / 100) % 100) / 100.0 - 0.5) * 0.08;
        double jitterY = (((seed / 10000) % 100) / 100.0 - 0.5) * 0.05;

        poseStack.pushPose();
        poseStack.translate(jitterX, jitterY, jitterZ);

        // When manifesting, scale up slightly then settle - "materializing" effect
        if (entity.isManifesting()) {
            float manifestProgress = (entity.tickCount % 20 + partialTick) / 20.0f;
            float scale = 0.8f + 0.4f * (float) java.lang.Math.sin(manifestProgress * java.lang.Math.PI);
            poseStack.scale(scale, scale, scale);
        }

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    @Override
    protected void scale(GlitchEntity entity, PoseStack poseStack, float partialTickTime) {
        // Slightly elongated - unnatural proportions
        poseStack.scale(0.95f, 1.15f, 0.95f);
    }
}
