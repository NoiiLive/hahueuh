package net.noiilive.hahueuh.api;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class OwnershipState {
    private static final Set<ResourceLocation> ownedAuthorities = new HashSet<>();
    private static final Set<ResourceLocation> deniedAbilities = new HashSet<>();

    private OwnershipState() {}

    public static void setAuthorityOwned(ResourceLocation authorityId, boolean owned) {
        if (owned) ownedAuthorities.add(authorityId);
        else ownedAuthorities.remove(authorityId);
    }

    public static boolean isAuthorityOwned(ResourceLocation authorityId) {
        return ownedAuthorities.contains(authorityId);
    }

    public static void setAbilityDenied(ResourceLocation abilityId, boolean denied) {
        if (denied) deniedAbilities.add(abilityId);
        else deniedAbilities.remove(abilityId);
    }

    public static boolean isAbilityUsable(ResourceLocation abilityId) {
        return AbilityRegistry.get(abilityId)
                .map(ability -> isAuthorityOwned(ability.authorityId()) && !deniedAbilities.contains(abilityId))
                .orElse(false);
    }

    public static List<Authority> ownedAuthorities() {
        List<Authority> list = new ArrayList<>();
        for (Authority authority : AuthorityRegistry.all()) {
            if (ownedAuthorities.contains(authority.id())) list.add(authority);
        }
        return list;
    }
}
