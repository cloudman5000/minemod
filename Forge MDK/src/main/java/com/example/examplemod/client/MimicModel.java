package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.MimicEntity;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MimicModel extends PigModel<MimicEntity> {

    // The Mimic is disguised as a pig. We use the Pig model layer.
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("minecraft", "pig"), "main");

    public MimicModel(ModelPart root) {
        super(root);
    }
}
