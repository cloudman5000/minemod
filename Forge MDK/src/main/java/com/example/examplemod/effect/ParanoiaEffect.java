package com.example.examplemod.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class ParanoiaEffect extends MobEffect {
    public ParanoiaEffect() {
        // Neutral effect, color doesn't matter since it's invisible
        super(MobEffectCategory.NEUTRAL, 0x000000);
    }

    // Modern way to make effects completely invisible from HUD and particles
    @Override
    public boolean isInstantenous() {
        return false;
    }
}
