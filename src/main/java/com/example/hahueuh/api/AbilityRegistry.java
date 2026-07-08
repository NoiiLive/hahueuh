package com.example.hahueuh.api;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AbilityRegistry {
    private static final Map<ResourceLocation, Ability> REGISTRY = new LinkedHashMap<>();
    private static boolean frozen;

    private AbilityRegistry() {}

    public static void register(Ability ability) {
        if (frozen) {
            throw new IllegalStateException("Cannot register ability " + ability.id()
                    + " — AbilityRegistry is already frozen. Register during RegisterAbilitiesEvent.");
        }
        if (REGISTRY.containsKey(ability.id())) {
            throw new IllegalArgumentException("Duplicate ability id: " + ability.id());
        }
        if (AuthorityRegistry.get(ability.authorityId()).isEmpty()) {
            throw new IllegalArgumentException("Ability " + ability.id() + " references unknown authority "
                    + ability.authorityId() + " — register authorities before abilities");
        }
        REGISTRY.put(ability.id(), ability);
    }

    public static Optional<Ability> get(ResourceLocation id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    public static List<Ability> forAuthority(ResourceLocation authorityId) {
        List<Ability> list = new ArrayList<>();
        for (Ability ability : REGISTRY.values()) {
            if (ability.authorityId().equals(authorityId)) list.add(ability);
        }
        return list;
    }

    public static Collection<Ability> all() {
        return Collections.unmodifiableList(new ArrayList<>(REGISTRY.values()));
    }

    public static void freeze() {
        frozen = true;
    }
}
