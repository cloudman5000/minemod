package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.MimicEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MimicRenderer extends MobRenderer<MimicEntity, MimicModel> {

    private static final ResourceLocation NORMAL_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/entity/pig/pig.png");
    private static final ResourceLocation AGGRESSIVE_TEXTURE = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID,
            "textures/entity/mimic_aggressive.png");

    public MimicRenderer(EntityRendererProvider.Context context) {
        super(context, new MimicModel(context.bakeLayer(MimicModel.LAYER_LOCATION)), 0.7f);
    }

    @Override
    public ResourceLocation getTextureLocation(MimicEntity entity) {
        // Swap texture dynamically based on its state
        return entity.isAggressive() ? AGGRESSIVE_TEXTURE : NORMAL_TEXTURE;
    }
}
