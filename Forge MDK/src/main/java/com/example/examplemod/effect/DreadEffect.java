package com.example.examplemod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class DreadEffect extends MobEffect {
    public DreadEffect() {
        super(MobEffectCategory.NEUTRAL, 0x000000);
    }

    @Override
    public boolean isInstantenous() {
        return false;
    }
}
