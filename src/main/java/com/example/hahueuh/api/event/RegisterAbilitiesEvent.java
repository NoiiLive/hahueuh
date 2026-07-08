package com.example.hahueuh.api.event;

import com.example.hahueuh.api.Ability;
import com.example.hahueuh.api.AbilityRegistry;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public final class RegisterAbilitiesEvent extends Event implements IModBusEvent {
    public void register(Ability ability) {
        AbilityRegistry.register(ability);
    }
}
