package com.example.hahueuh;

import com.example.hahueuh.api.Authority;
import com.example.hahueuh.api.event.RegisterAuthoritiesEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = HahUeuh.MODID)
public final class HahUeuhAbilities {
    private HahUeuhAbilities() {}

    public static final ResourceLocation DOMAIN_AUTHORITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "domain");
    public static final ResourceLocation RETURN_BY_DEATH_AUTHORITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "return_by_death");
    public static final ResourceLocation SLOTH_AUTHORITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "sloth");

    public static final ResourceLocation DOMAIN_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "domain");
    public static final ResourceLocation RETURN_BY_DEATH_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "return_by_death");
    public static final ResourceLocation SLOTH_HAND_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "sloth_hand");

    @SubscribeEvent
    static void onRegisterAuthorities(RegisterAuthoritiesEvent event) {
        event.register(Authority.builder(DOMAIN_AUTHORITY)
                .translationKey("hahueuh.authority.domain")
                .build());
        event.register(Authority.builder(RETURN_BY_DEATH_AUTHORITY)
                .translationKey("hahueuh.authority.return_by_death")
                .build());
        event.register(Authority.builder(SLOTH_AUTHORITY)
                .translationKey("hahueuh.authority.sloth")
                .build());
    }
}
