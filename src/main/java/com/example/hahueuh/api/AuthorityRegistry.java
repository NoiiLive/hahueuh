package com.example.hahueuh.api;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AuthorityRegistry {
    private static final Map<ResourceLocation, Authority> REGISTRY = new LinkedHashMap<>();
    private static boolean frozen;

    private AuthorityRegistry() {}

    public static void register(Authority authority) {
        if (frozen) {
            throw new IllegalStateException("Cannot register authority " + authority.id()
                    + " — AuthorityRegistry is already frozen. Register during RegisterAuthoritiesEvent.");
        }
        if (REGISTRY.containsKey(authority.id())) {
            throw new IllegalArgumentException("Duplicate authority id: " + authority.id());
        }
        REGISTRY.put(authority.id(), authority);
    }

    public static Optional<Authority> get(ResourceLocation id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    public static Collection<Authority> all() {
        List<Authority> list = new ArrayList<>(REGISTRY.values());
        list.sort((a, b) -> Integer.compare(a.sortPriority(), b.sortPriority()));
        return Collections.unmodifiableList(list);
    }

    public static void freeze() {
        frozen = true;
    }
}
