package net.noiilive.hahueuh;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.noiilive.hahueuh.api.Authority;
import net.noiilive.hahueuh.api.event.RegisterAuthoritiesEvent;

@Mod.EventBusSubscriber(modid = HahUeuh.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HahUeuhAbilities {
    private HahUeuhAbilities() {}

    public static final ResourceLocation RETURN_BY_DEATH_AUTHORITY =
            new ResourceLocation(HahUeuh.MODID, "return_by_death");
    public static final ResourceLocation DOMAIN_AUTHORITY =
            new ResourceLocation(HahUeuh.MODID, "domain");
    public static final ResourceLocation FINGER_AUTHORITY =
            new ResourceLocation(HahUeuh.MODID, "finger");
    public static final ResourceLocation SLOTH_AUTHORITY =
            new ResourceLocation(HahUeuh.MODID, "sloth");
    public static final ResourceLocation GREED_AUTHORITY =
            new ResourceLocation(HahUeuh.MODID, "greed");

    public static final ResourceLocation RETURN_BY_DEATH_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "return_by_death");
    public static final ResourceLocation DOMAIN_VICTIM_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "domain_victim");
    public static final ResourceLocation DOMAIN_AGGRESSOR_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "domain_aggressor");
    public static final ResourceLocation FINGER_GRANT_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "finger_grant");
    public static final ResourceLocation SUMMON_HAND_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "summon_hand");
    public static final ResourceLocation FINGER_SUMMON_HAND_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "finger_summon_hand");
    public static final ResourceLocation QUICK_STRIKE_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "quick_strike");
    public static final ResourceLocation QUICK_GRASP_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "quick_grasp");
    public static final ResourceLocation HIDDEN_INTERACTION_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "hidden_interaction");
    public static final ResourceLocation SELF_PROPEL_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "self_propel");
    public static final ResourceLocation FINGER_QUICK_STRIKE_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "finger_quick_strike");
    public static final ResourceLocation FINGER_QUICK_GRASP_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "finger_quick_grasp");
    public static final ResourceLocation FINGER_SELF_PROPEL_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "finger_self_propel");
    public static final ResourceLocation ALLY_TRACKER_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "ally_tracker");
    public static final ResourceLocation BASE_SHIFT_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "base_shift");
    public static final ResourceLocation SECOND_SHIFT_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "second_shift");
    public static final ResourceLocation LIONS_HEART_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "lions_heart");
    public static final ResourceLocation LITTLE_KING_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "little_king");
    public static final ResourceLocation BOOK_OF_WISDOM_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "book_of_wisdom");
    public static final ResourceLocation MENTAL_OVERLOAD_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "mental_overload");
    public static final ResourceLocation VISION_OF_DANGER_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "vision_of_danger");
    public static final ResourceLocation VISION_OF_LIFE_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "vision_of_life");
    public static final ResourceLocation MATERIAL_PHASE_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "material_phase");
    public static final ResourceLocation OBJECT_FREEZE_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "object_freeze");
    public static final ResourceLocation SLOTH_COOLDOWN_KEY =
            new ResourceLocation(HahUeuh.MODID, "sloth_hand_cooldown");
    public static final ResourceLocation QUICK_ACTION_COOLDOWN_KEY =
            new ResourceLocation(HahUeuh.MODID, "quick_action_cooldown");
    public static final ResourceLocation SHAMAK_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "shamak");
    public static final ResourceLocation EL_SHAMAK_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "el_shamak");
    public static final ResourceLocation UL_SHAMAK_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "ul_shamak");
    public static final ResourceLocation AL_SHAMAK_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "al_shamak");
    public static final ResourceLocation MINYA_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "minya");
    public static final ResourceLocation EL_MINYA_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "el_minya");
    public static final ResourceLocation UL_MINYA_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "ul_minya");
    public static final ResourceLocation MURAK_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "murak");
    public static final ResourceLocation AL_KARUM_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "al_karum");
    public static final ResourceLocation VITA_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "vita");
    public static final ResourceLocation EL_VITA_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "el_vita");
    public static final ResourceLocation TELEPORTATION_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "teleportation");
    public static final ResourceLocation OL_SHAMAK_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "ol_shamak");
    public static final ResourceLocation DOOR_CROSSING_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "door_crossing");
    public static final ResourceLocation EMM_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "emm");
    public static final ResourceLocation EMT_ABILITY =
            new ResourceLocation(HahUeuh.MODID, "emt");
    public static final ResourceLocation YIN_AUTHORITY = MagicSchool.YIN.authorityId;

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
