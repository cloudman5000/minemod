package com.example.examplemod.effect;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT,
            ExampleMod.MODID);

    public static final RegistryObject<MobEffect> PARANOIA = MOB_EFFECTS.register("paranoia", ParanoiaEffect::new);
    public static final RegistryObject<MobEffect> DREAD = MOB_EFFECTS.register("dread", DreadEffect::new);
    public static final RegistryObject<MobEffect> DELUSION = MOB_EFFECTS.register("delusion", DelusionEffect::new);

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
