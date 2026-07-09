package com.example.hahueuh;

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
}
