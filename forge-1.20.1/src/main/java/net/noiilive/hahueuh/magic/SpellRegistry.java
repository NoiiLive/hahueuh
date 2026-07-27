package net.noiilive.hahueuh.magic;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class SpellRegistry {
    private static final Map<ResourceLocation, Spell> REGISTRY = new LinkedHashMap<>();

    private SpellRegistry() {}

    public static void register(Spell spell) {
        if (REGISTRY.putIfAbsent(spell.id(), spell) != null) {
            throw new IllegalArgumentException("Duplicate spell id: " + spell.id());
        }
    }

    public static Optional<Spell> get(ResourceLocation id) {
        return Optional.ofNullable(REGISTRY.get(id));
    }

    public static Collection<Spell> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }
}
