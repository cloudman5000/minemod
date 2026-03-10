package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.CrawlerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CrawlerRenderer extends MobRenderer<CrawlerEntity, CrawlerModel> {
    private static final ResourceLocation CRAWLER_TEXTURE = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID,
            "textures/entity/crawler.png");

    public CrawlerRenderer(EntityRendererProvider.Context context) {
        super(context, new CrawlerModel(context.bakeLayer(CrawlerModel.LAYER_LOCATION)), 0.3f);
    }

    @Override
    public ResourceLocation getTextureLocation(CrawlerEntity entity) {
        return CRAWLER_TEXTURE;
    }
}
