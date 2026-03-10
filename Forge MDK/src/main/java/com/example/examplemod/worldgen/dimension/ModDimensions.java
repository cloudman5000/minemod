package com.example.examplemod.worldgen.dimension;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class ModDimensions {
    public static final ResourceKey<Level> NIGHTMARE_LEVEL_KEY = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "nightmare"));

    public static final ResourceKey<DimensionType> NIGHTMARE_DIM_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "nightmare"));
}
