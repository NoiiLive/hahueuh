package com.example.hahueuh;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, HahUeuh.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> WITCH_MIASMA = MOB_EFFECTS.register("witch_miasma",
            () -> new WitchMiasmaEffect(MobEffectCategory.HARMFUL, 0x7B4FA0));

    private ModEffects() {}
}
