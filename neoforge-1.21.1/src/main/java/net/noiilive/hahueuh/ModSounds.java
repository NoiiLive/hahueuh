package net.noiilive.hahueuh;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, HahUeuh.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> HAHH = SOUND_EVENTS.register("hahh",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "hahh")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UEUH = SOUND_EVENTS.register("ueuh",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "ueuh")));

    public static final DeferredHolder<SoundEvent, SoundEvent> EUHEUH = SOUND_EVENTS.register("euheuh",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "euheuh")));

    public static final DeferredHolder<SoundEvent, SoundEvent> LIONSHEART_ACTIVATE = SOUND_EVENTS.register("lionsheart_activate",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "lionsheart_activate")));

    public static final DeferredHolder<SoundEvent, SoundEvent> LIONSHEART_DEACTIVATE = SOUND_EVENTS.register("lionsheart_deactivate",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "lionsheart_deactivate")));

    public static final DeferredHolder<SoundEvent, SoundEvent> SLOTH_HAND_SUMMON = SOUND_EVENTS.register("sloth_hand_summon",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "sloth_hand_summon")));

    public static final DeferredHolder<SoundEvent, SoundEvent> SLOTH_HAND_USE = SOUND_EVENTS.register("sloth_hand_use",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "sloth_hand_use")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MINYA_SUMMON = SOUND_EVENTS.register("minya_summon",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "minya_summon")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MINYA_SHOOT = SOUND_EVENTS.register("minya_shoot",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "minya_shoot")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MINYA_EXPLODE = SOUND_EVENTS.register("minya_explode",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "minya_explode")));

    public static final DeferredHolder<SoundEvent, SoundEvent> UNSEEN_HAND_SUMMON = SOUND_EVENTS.register("unseen_hand_summon",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "unseen_hand_summon")));
    public static final DeferredHolder<SoundEvent, SoundEvent> UNSEEN_HAND_USE = SOUND_EVENTS.register("unseen_hand_use",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "unseen_hand_use")));

    public static final DeferredHolder<SoundEvent, SoundEvent> INVISIBLE_PROVIDENCE_SUMMON = SOUND_EVENTS.register("invisible_providence_summon",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "invisible_providence_summon")));
    public static final DeferredHolder<SoundEvent, SoundEvent> INVISIBLE_PROVIDENCE_USE = SOUND_EVENTS.register("invisible_providence_use",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "invisible_providence_use")));

    public static final DeferredHolder<SoundEvent, SoundEvent> SEKHMET_SUMMON = SOUND_EVENTS.register("sekhmet_summon",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "sekhmet_summon")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SEKHMET_USE = SOUND_EVENTS.register("sekhmet_use",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "sekhmet_use")));

    public static final DeferredHolder<SoundEvent, SoundEvent> MATERIAL_FREEZE_AIR = SOUND_EVENTS.register("material_freeze_air",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "material_freeze_air")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MATERIAL_FREEZE_SOLID = SOUND_EVENTS.register("material_freeze_solid",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "material_freeze_solid")));

    public static final DeferredHolder<SoundEvent, SoundEvent> VISION_OF_DANGER = SOUND_EVENTS.register("vision_of_danger",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "vision_of_danger")));
    public static final DeferredHolder<SoundEvent, SoundEvent> VISION_OF_LIFE = SOUND_EVENTS.register("vision_of_life",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "vision_of_life")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MENTAL_OVERLOAD = SOUND_EVENTS.register("mental_overload",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "mental_overload")));
    public static final DeferredHolder<SoundEvent, SoundEvent> MEMORIES_TOGGLE_OFF = SOUND_EVENTS.register("memories_toggle_off",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "memories_toggle_off")));

    public static final DeferredHolder<SoundEvent, SoundEvent> SHAMAK_USE = SOUND_EVENTS.register("shamak_use",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "shamak_use")));
    public static final DeferredHolder<SoundEvent, SoundEvent> EL_SHAMAK_USE = SOUND_EVENTS.register("el_shamak_use",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "el_shamak_use")));
    public static final DeferredHolder<SoundEvent, SoundEvent> BLACKHOLE_SUMMON = SOUND_EVENTS.register("blackhole_summon",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "blackhole_summon")));
    public static final DeferredHolder<SoundEvent, SoundEvent> BLACKHOLE_EXPLODE = SOUND_EVENTS.register("blackhole_explode",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "blackhole_explode")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AL_SHAMAK_USE = SOUND_EVENTS.register("al_shamak_use",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "al_shamak_use")));
    public static final DeferredHolder<SoundEvent, SoundEvent> AL_SHAMAK_RELEASE = SOUND_EVENTS.register("al_shamak_release",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "al_shamak_release")));

    public static final DeferredHolder<SoundEvent, SoundEvent> DOMAIN_OPEN = SOUND_EVENTS.register("domain_open",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "domain_open")));
    public static final DeferredHolder<SoundEvent, SoundEvent> DOMAIN_CLOSE = SOUND_EVENTS.register("domain_close",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "domain_close")));
}
