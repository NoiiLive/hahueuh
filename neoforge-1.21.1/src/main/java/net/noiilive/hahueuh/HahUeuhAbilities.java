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
    public static final ResourceLocation FINGER_AUTHORITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "finger");
    public static final ResourceLocation YIN_AUTHORITY = MagicSchool.YIN.authorityId;

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

    public static final ResourceLocation FINGER_GRANT_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "finger_grant");
    public static final ResourceLocation FINGER_SUMMON_HAND_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "finger_summon_hand");
    public static final ResourceLocation FINGER_QUICK_STRIKE_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "finger_quick_strike");
    public static final ResourceLocation FINGER_QUICK_GRASP_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "finger_quick_grasp");
    public static final ResourceLocation FINGER_SELF_PROPEL_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "finger_self_propel");

    public static final ResourceLocation LIONS_HEART_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "lions_heart");
    public static final ResourceLocation LITTLE_KING_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "little_king");
    public static final ResourceLocation MATERIAL_PHASE_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "material_phase");
    public static final ResourceLocation OBJECT_FREEZE_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "object_freeze");
    public static final ResourceLocation ALLY_TRACKER_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "ally_tracker");
    public static final ResourceLocation BASE_SHIFT_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "base_shift");
    public static final ResourceLocation SECOND_SHIFT_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "second_shift");
    public static final ResourceLocation BOOK_OF_WISDOM_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "book_of_wisdom");
    public static final ResourceLocation MENTAL_OVERLOAD_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "mental_overload");
    public static final ResourceLocation VISION_OF_DANGER_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "vision_of_danger");
    public static final ResourceLocation VISION_OF_LIFE_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "vision_of_life");

    public static final ResourceLocation SHAMAK_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "shamak");
    public static final ResourceLocation EL_SHAMAK_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "el_shamak");
    public static final ResourceLocation UL_SHAMAK_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "ul_shamak");
    public static final ResourceLocation AL_SHAMAK_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "al_shamak");
    public static final ResourceLocation MINYA_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "minya");
    public static final ResourceLocation EL_MINYA_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "el_minya");
    public static final ResourceLocation UL_MINYA_ABILITY = ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "ul_minya");

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
        event.register(Authority.builder(FINGER_AUTHORITY)
                .translationKey("hahueuh.authority.finger")
                .sortPriority(10)
                .build());
        event.register(Authority.builder(MagicSchool.GENERAL_AUTHORITY)
                .translationKey("hahueuh.authority.magic_general")
                .build());
        for (MagicSchool school : MagicSchool.values()) {
            event.register(Authority.builder(school.authorityId)
                    .translationKey(school.translationKey)
                    .build());
        }
    }
}
