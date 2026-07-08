package com.example.hahueuh.api.event;

import com.example.hahueuh.api.Authority;
import com.example.hahueuh.api.AuthorityRegistry;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public final class RegisterAuthoritiesEvent extends Event implements IModBusEvent {
    public void register(Authority authority) {
        AuthorityRegistry.register(authority);
    }
}
