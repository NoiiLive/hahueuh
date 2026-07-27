package net.noiilive.hahueuh.api.event;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import net.noiilive.hahueuh.api.Ability;
import net.noiilive.hahueuh.api.AbilityRegistry;

public final class RegisterAbilitiesEvent extends Event implements IModBusEvent {
    public void register(Ability ability) {
        AbilityRegistry.register(ability);
    }
}
