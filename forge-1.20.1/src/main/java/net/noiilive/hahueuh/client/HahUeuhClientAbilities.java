package net.noiilive.hahueuh.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.HahUeuhAbilities;
import net.noiilive.hahueuh.api.Ability;
import net.noiilive.hahueuh.api.event.RegisterAbilitiesEvent;
import net.noiilive.hahueuh.network.ActivateAuthorityPacket;
import net.noiilive.hahueuh.network.CastSpellPacket;
import net.noiilive.hahueuh.network.FingerGrantPacket;
import net.noiilive.hahueuh.network.AllyTrackerActivatePacket;
import net.noiilive.hahueuh.network.BaseShiftTogglePacket;
import net.noiilive.hahueuh.network.SecondShiftTogglePacket;
import net.noiilive.hahueuh.network.BookOfWisdomTogglePacket;
import net.noiilive.hahueuh.network.MentalOverloadActivatePacket;
import net.noiilive.hahueuh.network.VisionOfDangerTogglePacket;
import net.noiilive.hahueuh.network.VisionOfLifeTogglePacket;
import net.noiilive.hahueuh.network.LionsHeartTogglePacket;
import net.noiilive.hahueuh.network.LittleKingImplantPacket;
import net.noiilive.hahueuh.network.MaterialPhaseTogglePacket;
import net.noiilive.hahueuh.network.ObjectFreezeActivatePacket;
import net.noiilive.hahueuh.network.ClientGreedState;
import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.ClientFingerState;
import net.noiilive.hahueuh.network.ClientSlothState;
import net.noiilive.hahueuh.network.SlothVariant;
import net.noiilive.hahueuh.network.ModNetworking;
import net.noiilive.hahueuh.network.ReturnByDeathActivatePacket;

@Mod.EventBusSubscriber(modid = HahUeuh.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HahUeuhClientAbilities {
    private HahUeuhClientAbilities() {}

    @SubscribeEvent
    static void onRegisterAbilities(RegisterAbilitiesEvent event) {
        event.register(Ability.builder(HahUeuhAbilities.DOMAIN_VICTIM_ABILITY,
                        HahUeuhAbilities.DOMAIN_AUTHORITY)
                .translationKey("hahueuh.ability.domain_victim")
                .shortLabel(() -> "VIC")
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(new ActivateAuthorityPacket(false)))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.DOMAIN_AGGRESSOR_ABILITY,
                        HahUeuhAbilities.DOMAIN_AUTHORITY)
                .translationKey("hahueuh.ability.domain_aggressor")
                .shortLabel(() -> "AGR")
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(new ActivateAuthorityPacket(true)))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.SUMMON_HAND_ABILITY, HahUeuhAbilities.SLOTH_AUTHORITY)
                .translationKey("hahueuh.ability.summon_hand")
                .shortLabel(() -> "SUM")
                .sharesCooldownWith(HahUeuhAbilities.SLOTH_COOLDOWN_KEY)
                .onActivate(new SummonHandBehavior())
                .build());

        event.register(Ability.builder(HahUeuhAbilities.QUICK_STRIKE_ABILITY, HahUeuhAbilities.SLOTH_AUTHORITY)
                .holdBased()
                .translationKey("hahueuh.ability.quick_strike")
                .shortLabel(() -> "QST")
                .sharesCooldownWith(HahUeuhAbilities.QUICK_ACTION_COOLDOWN_KEY)
                .onHeldTick(new QuickStrikeBehavior())
                .build());

        event.register(Ability.builder(HahUeuhAbilities.QUICK_GRASP_ABILITY, HahUeuhAbilities.SLOTH_AUTHORITY)
                .holdBased()
                .translationKey("hahueuh.ability.quick_grasp")
                .shortLabel(() -> "QGR")
                .sharesCooldownWith(HahUeuhAbilities.QUICK_ACTION_COOLDOWN_KEY)
                .onHeldTick(new QuickGraspBehavior())
                .build());

        event.register(Ability.builder(HahUeuhAbilities.HIDDEN_INTERACTION_ABILITY, HahUeuhAbilities.SLOTH_AUTHORITY)
                .holdBased()
                .translationKey("hahueuh.ability.hidden_interaction")
                .shortLabel(() -> "HID")
                .sharesCooldownWith(HahUeuhAbilities.QUICK_ACTION_COOLDOWN_KEY)
                .availableWhen(() -> ClientSlothState.slothVariant() == SlothVariant.INVISIBLE_PROVIDENCE)
                .onHeldTick(new HiddenInteractionBehavior())
                .build());

        event.register(Ability.builder(HahUeuhAbilities.SELF_PROPEL_ABILITY, HahUeuhAbilities.SLOTH_AUTHORITY)
                .holdBased()
                .translationKey("hahueuh.ability.self_propel")
                .shortLabel(() -> "PRO")
                .sharesCooldownWith(HahUeuhAbilities.QUICK_ACTION_COOLDOWN_KEY)
                .availableWhen(() -> ClientSlothState.slothVariant() == SlothVariant.UNSEEN_HANDS)
                .onHeldTick(new SelfPropelBehavior())
                .build());

        event.register(Ability.builder(HahUeuhAbilities.FINGER_QUICK_STRIKE_ABILITY, HahUeuhAbilities.FINGER_AUTHORITY)
                .holdBased()
                .translationKey("hahueuh.ability.finger_quick_strike")
                .shortLabel(() -> "QST")
                .sharesCooldownWith(HahUeuhAbilities.QUICK_ACTION_COOLDOWN_KEY)
                .availableWhen(ClientFingerState::hasHands)
                .onHeldTick(new QuickStrikeBehavior())
                .build());

        event.register(Ability.builder(HahUeuhAbilities.FINGER_QUICK_GRASP_ABILITY, HahUeuhAbilities.FINGER_AUTHORITY)
                .holdBased()
                .translationKey("hahueuh.ability.finger_quick_grasp")
                .shortLabel(() -> "QGR")
                .sharesCooldownWith(HahUeuhAbilities.QUICK_ACTION_COOLDOWN_KEY)
                .availableWhen(ClientFingerState::hasHands)
                .onHeldTick(new QuickGraspBehavior())
                .build());

        event.register(Ability.builder(HahUeuhAbilities.FINGER_SELF_PROPEL_ABILITY, HahUeuhAbilities.FINGER_AUTHORITY)
                .holdBased()
                .translationKey("hahueuh.ability.finger_self_propel")
                .shortLabel(() -> "PRO")
                .sharesCooldownWith(HahUeuhAbilities.QUICK_ACTION_COOLDOWN_KEY)
                .availableWhen(() -> ClientFingerState.hands() >= 2)
                .onHeldTick(new SelfPropelBehavior())
                .build());

        event.register(Ability.builder(HahUeuhAbilities.FINGER_SUMMON_HAND_ABILITY, HahUeuhAbilities.FINGER_AUTHORITY)
                .translationKey("hahueuh.ability.finger_summon_hand")
                .shortLabel(() -> "SUM")
                .sharesCooldownWith(HahUeuhAbilities.SLOTH_COOLDOWN_KEY)
                .availableWhen(net.noiilive.hahueuh.network.ClientFingerState::hasHands)
                .onActivate(new SummonHandBehavior())
                .build());

        event.register(Ability.builder(HahUeuhAbilities.FINGER_GRANT_ABILITY, HahUeuhAbilities.SLOTH_AUTHORITY)
                .translationKey("hahueuh.ability.finger_grant")
                .shortLabel(() -> "FIN")
                .availableWhen(() -> net.noiilive.hahueuh.network.ClientSlothState.slothVariant()
                        == net.noiilive.hahueuh.network.SlothVariant.UNSEEN_HANDS)
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(FingerGrantPacket.INSTANCE))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.ALLY_TRACKER_ABILITY, HahUeuhAbilities.GREED_AUTHORITY)
                .translationKey("hahueuh.ability.ally_tracker")
                .shortLabel(() -> "ALY")
                .availableWhen(() -> ClientGreedState.greedVariant() == GreedVariant.CORLEONIS)
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(AllyTrackerActivatePacket.INSTANCE))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.BASE_SHIFT_ABILITY, HahUeuhAbilities.GREED_AUTHORITY)
                .translationKey("hahueuh.ability.base_shift")
                .shortLabel(() -> "SHF")
                .availableWhen(() -> ClientGreedState.greedVariant() == GreedVariant.CORLEONIS)
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(BaseShiftTogglePacket.INSTANCE))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.SECOND_SHIFT_ABILITY, HahUeuhAbilities.GREED_AUTHORITY)
                .translationKey("hahueuh.ability.second_shift")
                .shortLabel(() -> "SH2")
                .availableWhen(() -> ClientGreedState.greedVariant() == GreedVariant.CORLEONIS)
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(SecondShiftTogglePacket.INSTANCE))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.BOOK_OF_WISDOM_ABILITY, HahUeuhAbilities.GREED_AUTHORITY)
                .translationKey("hahueuh.ability.book_of_wisdom")
                .shortLabel(() -> "BOW")
                .availableWhen(() -> ClientGreedState.greedVariant() == GreedVariant.ECHIDNA)
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(BookOfWisdomTogglePacket.INSTANCE))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.MENTAL_OVERLOAD_ABILITY, HahUeuhAbilities.GREED_AUTHORITY)
                .translationKey("hahueuh.ability.mental_overload")
                .shortLabel(() -> "MOV")
                .availableWhen(() -> ClientGreedState.greedVariant() == GreedVariant.ECHIDNA)
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(MentalOverloadActivatePacket.INSTANCE))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.VISION_OF_DANGER_ABILITY, HahUeuhAbilities.GREED_AUTHORITY)
                .translationKey("hahueuh.ability.vision_of_danger")
                .shortLabel(() -> "VOD")
                .availableWhen(() -> ClientGreedState.greedVariant() == GreedVariant.ECHIDNA)
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(VisionOfDangerTogglePacket.INSTANCE))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.VISION_OF_LIFE_ABILITY, HahUeuhAbilities.GREED_AUTHORITY)
                .translationKey("hahueuh.ability.vision_of_life")
                .shortLabel(() -> "VOL")
                .availableWhen(() -> ClientGreedState.greedVariant() == GreedVariant.ECHIDNA)
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(VisionOfLifeTogglePacket.INSTANCE))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.LIONS_HEART_ABILITY, HahUeuhAbilities.GREED_AUTHORITY)
                .translationKey("hahueuh.ability.lions_heart")
                .shortLabel(() -> "LIO")
                .availableWhen(() -> ClientGreedState.greedVariant() == GreedVariant.LIONSHEART)
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(LionsHeartTogglePacket.INSTANCE))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.LITTLE_KING_ABILITY, HahUeuhAbilities.GREED_AUTHORITY)
                .translationKey("hahueuh.ability.little_king")
                .shortLabel(() -> "KIN")
                .availableWhen(() -> ClientGreedState.greedVariant() == GreedVariant.LIONSHEART)
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(LittleKingImplantPacket.INSTANCE))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.MATERIAL_PHASE_ABILITY, HahUeuhAbilities.GREED_AUTHORITY)
                .translationKey("hahueuh.ability.material_phase")
                .shortLabel(() -> "PHA")
                .availableWhen(() -> ClientGreedState.greedVariant() == GreedVariant.LIONSHEART)
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(MaterialPhaseTogglePacket.INSTANCE))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.OBJECT_FREEZE_ABILITY, HahUeuhAbilities.GREED_AUTHORITY)
                .translationKey("hahueuh.ability.object_freeze")
                .shortLabel(() -> "FRZ")
                .availableWhen(() -> ClientGreedState.greedVariant() == GreedVariant.LIONSHEART)
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(ObjectFreezeActivatePacket.INSTANCE))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.SHAMAK_ABILITY, HahUeuhAbilities.YIN_AUTHORITY)
                .translationKey("hahueuh.ability.shamak")
                .shortLabel(() -> "SHA")
                .availableWhen(net.noiilive.hahueuh.client.ClientMagicState::hasYin)
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(
                        new CastSpellPacket(net.noiilive.hahueuh.magic.Spells.SHAMAK)))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.EL_SHAMAK_ABILITY, HahUeuhAbilities.YIN_AUTHORITY)
                .translationKey("hahueuh.ability.el_shamak")
                .shortLabel(() -> "ELS")
                .availableWhen(net.noiilive.hahueuh.client.ClientMagicState::hasYin)
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(
                        new CastSpellPacket(net.noiilive.hahueuh.magic.Spells.EL_SHAMAK)))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.UL_SHAMAK_ABILITY, HahUeuhAbilities.YIN_AUTHORITY)
                .translationKey("hahueuh.ability.ul_shamak")
                .shortLabel(() -> "ULS")
                .availableWhen(net.noiilive.hahueuh.client.ClientMagicState::hasYin)
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(
                        new CastSpellPacket(net.noiilive.hahueuh.magic.Spells.UL_SHAMAK)))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.AL_SHAMAK_ABILITY, HahUeuhAbilities.YIN_AUTHORITY)
                .translationKey("hahueuh.ability.al_shamak")
                .shortLabel(() -> "ALS")
                .availableWhen(net.noiilive.hahueuh.client.ClientMagicState::hasYin)
                .onActivate(ctx -> net.noiilive.hahueuh.client.AlShamakClient.activate())
                .build());

        event.register(Ability.builder(HahUeuhAbilities.MINYA_ABILITY, HahUeuhAbilities.YIN_AUTHORITY)
                .translationKey("hahueuh.ability.minya")
                .shortLabel(() -> "MIN")
                .availableWhen(net.noiilive.hahueuh.client.ClientMagicState::hasYin)
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(
                        new CastSpellPacket(net.noiilive.hahueuh.magic.Spells.MINYA)))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.EL_MINYA_ABILITY, HahUeuhAbilities.YIN_AUTHORITY)
                .translationKey("hahueuh.ability.el_minya")
                .shortLabel(() -> "ELM")
                .availableWhen(net.noiilive.hahueuh.client.ClientMagicState::hasYin)
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(
                        new CastSpellPacket(net.noiilive.hahueuh.magic.Spells.EL_MINYA)))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.UL_MINYA_ABILITY, HahUeuhAbilities.YIN_AUTHORITY)
                .translationKey("hahueuh.ability.ul_minya")
                .shortLabel(() -> "ULM")
                .availableWhen(net.noiilive.hahueuh.client.ClientMagicState::hasYin)
                .onActivate(ctx -> net.noiilive.hahueuh.client.UlMinyaClient.activate())
                .build());

        event.register(Ability.builder(HahUeuhAbilities.RETURN_BY_DEATH_ABILITY,
                        HahUeuhAbilities.RETURN_BY_DEATH_AUTHORITY)
                .translationKey("hahueuh.ability.return")
                .shortLabel(() -> "RET")
                .onActivate(ctx -> ModNetworking.CHANNEL.sendToServer(ReturnByDeathActivatePacket.INSTANCE))
                .build());
    }
}
