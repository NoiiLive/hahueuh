package net.noiilive.hahueuh;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, HahUeuh.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> WITCH_SCENT = MOB_EFFECTS.register("witch_scent",
            () -> new WitchScentEffect(MobEffectCategory.HARMFUL, 0x7B4FA0));

    public static final DeferredHolder<MobEffect, MobEffect> SENSORY_DEPRIVATION = MOB_EFFECTS.register("sensory_deprivation",
            () -> new SensoryDeprivationEffect(MobEffectCategory.HARMFUL, 0x0A0A12));

    public static final DeferredHolder<MobEffect, MobEffect> BODILY_DISCONNECT = MOB_EFFECTS.register("bodily_disconnect",
            () -> new BodilyDisconnectEffect(MobEffectCategory.HARMFUL, 0x3A2D5C));

    public static final DeferredHolder<MobEffect, MobEffect> CRYSTALLIZED = MOB_EFFECTS.register("crystallized",
            () -> new CrystallizedEffect(MobEffectCategory.HARMFUL, 0x9B5FD0));

    public static final DeferredHolder<MobEffect, MobEffect> INSANITY = MOB_EFFECTS.register("insanity",
            () -> new InsanityEffect(MobEffectCategory.HARMFUL, 0x6E2C8F));

    public static final DeferredHolder<MobEffect, MobEffect> LEASHED = MOB_EFFECTS.register("leashed",
            () -> new LeashedEffect(MobEffectCategory.HARMFUL, 0x6A3FA0));

    public static final DeferredHolder<MobEffect, MobEffect> REDUCED_GRAVITY = MOB_EFFECTS.register("reduced_gravity",
            () -> new ReducedGravityEffect(MobEffectCategory.NEUTRAL, 0x9FD8FF));

    public static final DeferredHolder<MobEffect, MobEffect> INCREASED_GRAVITY = MOB_EFFECTS.register("increased_gravity",
            () -> new IncreasedGravityEffect(MobEffectCategory.HARMFUL, 0x6B4A2A));

    private ModEffects() {}
}
