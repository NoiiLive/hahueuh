package net.noiilive.hahueuh;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, HahUeuh.MODID);

    public static final RegistryObject<SoundEvent> HAHH = SOUND_EVENTS.register("hahh",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "hahh")));

    public static final RegistryObject<SoundEvent> UEUH = SOUND_EVENTS.register("ueuh",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "ueuh")));

    public static final RegistryObject<SoundEvent> EUHEUH = SOUND_EVENTS.register("euheuh",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "euheuh")));

    public static final RegistryObject<SoundEvent> MATERIAL_FREEZE_AIR = SOUND_EVENTS.register("material_freeze_air",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "material_freeze_air")));

    public static final RegistryObject<SoundEvent> MATERIAL_FREEZE_SOLID = SOUND_EVENTS.register("material_freeze_solid",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "material_freeze_solid")));

    public static final RegistryObject<SoundEvent> VISION_OF_LIFE = SOUND_EVENTS.register("vision_of_life",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "vision_of_life")));

    public static final RegistryObject<SoundEvent> VISION_OF_DANGER = SOUND_EVENTS.register("vision_of_danger",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "vision_of_danger")));

    public static final RegistryObject<SoundEvent> MENTAL_OVERLOAD = SOUND_EVENTS.register("mental_overload",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "mental_overload")));

    public static final RegistryObject<SoundEvent> MEMORIES_TOGGLE_OFF = SOUND_EVENTS.register("memories_toggle_off",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "memories_toggle_off")));

    public static final RegistryObject<SoundEvent> LIONSHEART_ACTIVATE = SOUND_EVENTS.register("lionsheart_activate",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "lionsheart_activate")));

    public static final RegistryObject<SoundEvent> LIONSHEART_DEACTIVATE = SOUND_EVENTS.register("lionsheart_deactivate",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "lionsheart_deactivate")));

    public static final RegistryObject<SoundEvent> DOMAIN_OPEN = SOUND_EVENTS.register("domain_open",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "domain_open")));

    public static final RegistryObject<SoundEvent> DOMAIN_CLOSE = SOUND_EVENTS.register("domain_close",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "domain_close")));

    public static final RegistryObject<SoundEvent> EL_SHAMAK_USE = SOUND_EVENTS.register("el_shamak_use",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "el_shamak_use")));

    public static final RegistryObject<SoundEvent> AL_SHAMAK_USE = SOUND_EVENTS.register("al_shamak_use",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "al_shamak_use")));

    public static final RegistryObject<SoundEvent> AL_SHAMAK_RELEASE = SOUND_EVENTS.register("al_shamak_release",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "al_shamak_release")));

    public static final RegistryObject<SoundEvent> MINYA_SUMMON = SOUND_EVENTS.register("minya_summon",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "minya_summon")));

    public static final RegistryObject<SoundEvent> MINYA_SHOOT = SOUND_EVENTS.register("minya_shoot",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "minya_shoot")));

    public static final RegistryObject<SoundEvent> MINYA_EXPLODE = SOUND_EVENTS.register("minya_explode",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "minya_explode")));

    public static final RegistryObject<SoundEvent> BLACKHOLE_SUMMON = SOUND_EVENTS.register("blackhole_summon",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "blackhole_summon")));

    public static final RegistryObject<SoundEvent> BLACKHOLE_EXPLODE = SOUND_EVENTS.register("blackhole_explode",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "blackhole_explode")));

    public static final RegistryObject<SoundEvent> SHAMAK_USE = SOUND_EVENTS.register("shamak_use",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "shamak_use")));

    public static final RegistryObject<SoundEvent> SLOTH_HAND_SUMMON = SOUND_EVENTS.register("sloth_hand_summon",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "sloth_hand_summon")));

    public static final RegistryObject<SoundEvent> SLOTH_HAND_USE = SOUND_EVENTS.register("sloth_hand_use",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "sloth_hand_use")));

    public static final RegistryObject<SoundEvent> UNSEEN_HAND_SUMMON = SOUND_EVENTS.register("unseen_hand_summon",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "unseen_hand_summon")));

    public static final RegistryObject<SoundEvent> UNSEEN_HAND_USE = SOUND_EVENTS.register("unseen_hand_use",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "unseen_hand_use")));

    public static final RegistryObject<SoundEvent> INVISIBLE_PROVIDENCE_SUMMON = SOUND_EVENTS.register("invisible_providence_summon",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "invisible_providence_summon")));

    public static final RegistryObject<SoundEvent> INVISIBLE_PROVIDENCE_USE = SOUND_EVENTS.register("invisible_providence_use",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "invisible_providence_use")));

    public static final RegistryObject<SoundEvent> SEKHMET_SUMMON = SOUND_EVENTS.register("sekhmet_summon",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "sekhmet_summon")));

    public static final RegistryObject<SoundEvent> SEKHMET_USE = SOUND_EVENTS.register("sekhmet_use",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(HahUeuh.MODID, "sekhmet_use")));

    private ModSounds() {}
}
