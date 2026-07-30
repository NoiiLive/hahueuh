package net.noiilive.hahueuh;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, HahUeuh.MODID);

    public static final RegistryObject<MobEffect> WITCH_SCENT = MOB_EFFECTS.register("witch_scent",
            () -> new WitchScentEffect(MobEffectCategory.HARMFUL, 0x7B4FA0));

    public static final RegistryObject<MobEffect> BODILY_DISCONNECT = MOB_EFFECTS.register("bodily_disconnect",
            () -> new BodilyDisconnectEffect(MobEffectCategory.HARMFUL, 0x3A2D5C));

    public static final RegistryObject<MobEffect> SENSORY_DEPRIVATION = MOB_EFFECTS.register("sensory_deprivation",
            () -> new SensoryDeprivationEffect(MobEffectCategory.HARMFUL, 0x0A0A12));

    public static final RegistryObject<MobEffect> CRYSTALLIZED = MOB_EFFECTS.register("crystallized",
            () -> new CrystallizedEffect(MobEffectCategory.HARMFUL, 0x9B5FD0));

    public static final RegistryObject<MobEffect> REDUCED_GRAVITY = MOB_EFFECTS.register("reduced_gravity",
            () -> new ReducedGravityEffect(MobEffectCategory.NEUTRAL, 0x9FD8FF));

    public static final RegistryObject<MobEffect> INCREASED_GRAVITY = MOB_EFFECTS.register("increased_gravity",
            () -> new IncreasedGravityEffect(MobEffectCategory.HARMFUL, 0x6B4A2A));

    public static final RegistryObject<MobEffect> INSANITY = MOB_EFFECTS.register("insanity",
            () -> new InsanityEffect(MobEffectCategory.HARMFUL, 0x6E2C8F));

    public static final RegistryObject<MobEffect> LEASHED = MOB_EFFECTS.register("leashed",
            () -> new LeashedEffect(MobEffectCategory.HARMFUL, 0x6A3FA0));

    private ModEffects() {}
}
