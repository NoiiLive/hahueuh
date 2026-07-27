package net.noiilive.hahueuh.api.event;

import net.noiilive.hahueuh.api.Ability;
import net.noiilive.hahueuh.api.AbilityRegistry;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public final class RegisterAbilitiesEvent extends Event implements IModBusEvent {
    public void register(Ability ability) {
        AbilityRegistry.register(ability);
    }
}
