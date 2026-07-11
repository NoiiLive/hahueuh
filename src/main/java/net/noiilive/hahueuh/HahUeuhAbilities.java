package net.noiilive.hahueuh;

import net.noiilive.hahueuh.api.Authority;
import net.noiilive.hahueuh.api.event.RegisterAuthoritiesEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = HahUeuh.MODID)
public final class HahUeuhAbilities {
    private HahUeuhAbilities() {}

    public static final ResourceLocation DOMAIN_AUTHORITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "domain");
    public static final ResourceLocation RETURN_BY_DEATH_AUTHORITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "return_by_death");
    public static final ResourceLocation SLOTH_AUTHORITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "sloth");
    public static final ResourceLocation GREED_AUTHORITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "greed");

    public static final ResourceLocation DOMAIN_VICTIM_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "domain_victim");
    public static final ResourceLocation DOMAIN_AGGRESSOR_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "domain_aggressor");
    public static final ResourceLocation RETURN_BY_DEATH_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "return_by_death");
    public static final ResourceLocation SLOTH_HAND_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "sloth_hand");

    public static final ResourceLocation SUMMON_HAND_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "summon_hand");
    public static final ResourceLocation QUICK_STRIKE_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "quick_strike");
    public static final ResourceLocation QUICK_GRASP_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "quick_grasp");
    public static final ResourceLocation HIDDEN_INTERACTION_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "hidden_interaction");
    public static final ResourceLocation SELF_PROPEL_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "self_propel");
    public static final ResourceLocation SLOTH_COOLDOWN_KEY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "sloth_hand_cooldown");
    public static final ResourceLocation QUICK_ACTION_COOLDOWN_KEY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "quick_action_cooldown");

    public static final ResourceLocation LIONS_HEART_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "lions_heart");
    public static final ResourceLocation LITTLE_KING_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "little_king");
    public static final ResourceLocation MATERIAL_PHASE_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "material_phase");
    public static final ResourceLocation OBJECT_FREEZE_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "object_freeze");
    public static final ResourceLocation ALLY_TRACKER_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "ally_tracker");
    public static final ResourceLocation BASE_SHIFT_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "base_shift");
    public static final ResourceLocation SECOND_SHIFT_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "second_shift");

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
        event.register(Authority.builder(GREED_AUTHORITY)
                .translationKey("hahueuh.authority.greed")
                .build());
    }
}
