package com.example.examplemod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.example.examplemod.entity.SignerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class SignerRenderer extends HumanoidMobRenderer<SignerEntity, HumanoidModel<SignerEntity>> {

    public SignerRenderer(EntityRendererProvider.Context context) {
        // Re-use the zombie model but scale it to look taller and creepier
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(SignerEntity entity) {
        // Use a completely black/missing texture or a custom one if available.
        // For now, we'll try to use the enderman texture to make it look dark and
        // creepy.
        return ResourceLocation.withDefaultNamespace("textures/entity/enderman/enderman.png");
    }

    @Override
    public void render(SignerEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        // Make the Signer taller and thinner
        poseStack.scale(0.9f, 1.3f, 0.9f);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
