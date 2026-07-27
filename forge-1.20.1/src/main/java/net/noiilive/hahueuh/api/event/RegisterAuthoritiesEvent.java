package net.noiilive.hahueuh.api.event;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import net.noiilive.hahueuh.api.Authority;
import net.noiilive.hahueuh.api.AuthorityRegistry;

public final class RegisterAuthoritiesEvent extends Event implements IModBusEvent {
    public void register(Authority authority) {
        AuthorityRegistry.register(authority);
    }
}
