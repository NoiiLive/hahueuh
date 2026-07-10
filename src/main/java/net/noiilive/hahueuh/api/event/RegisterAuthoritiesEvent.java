package net.noiilive.hahueuh.api.event;

import net.noiilive.hahueuh.api.Authority;
import net.noiilive.hahueuh.api.AuthorityRegistry;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public final class RegisterAuthoritiesEvent extends Event implements IModBusEvent {
    public void register(Authority authority) {
        AuthorityRegistry.register(authority);
    }
}
