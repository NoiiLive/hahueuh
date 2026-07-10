package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.HahUeuhAbilities;
import net.noiilive.hahueuh.api.Ability;
import net.noiilive.hahueuh.api.event.RegisterAbilitiesEvent;
import net.noiilive.hahueuh.network.ActivateAuthorityPayload;
import net.noiilive.hahueuh.network.ClientGreedState;
import net.noiilive.hahueuh.network.ClientSlothState;
import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.LionsHeartTogglePayload;
import net.noiilive.hahueuh.network.LittleKingImplantPayload;
import net.noiilive.hahueuh.network.SlothVariant;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = HahUeuh.MODID, value = Dist.CLIENT)
public final class HahUeuhClientAbilities {
    private HahUeuhClientAbilities() {}

    @SubscribeEvent
    static void onRegisterAbilities(RegisterAbilitiesEvent event) {
        event.register(Ability.builder(HahUeuhAbilities.DOMAIN_VICTIM_ABILITY, HahUeuhAbilities.DOMAIN_AUTHORITY)
                .translationKey("hahueuh.ability.domain_victim")
                .shortLabel(() -> "VIC")
                .onActivate(ctx -> PacketDistributor.sendToServer(new ActivateAuthorityPayload(false)))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.DOMAIN_AGGRESSOR_ABILITY, HahUeuhAbilities.DOMAIN_AUTHORITY)
                .translationKey("hahueuh.ability.domain_aggressor")
                .shortLabel(() -> "AGR")
                .onActivate(ctx -> PacketDistributor.sendToServer(new ActivateAuthorityPayload(true)))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.RETURN_BY_DEATH_ABILITY, HahUeuhAbilities.RETURN_BY_DEATH_AUTHORITY)
                .translationKey("hahueuh.ability.return")
                .shortLabel(() -> "RET")
                .onActivate(ctx -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.getConnection() != null) {
                        mc.getConnection().sendChat("I can use Return by Death");
                    }
                })
                .build());

        event.register(Ability.builder(HahUeuhAbilities.SUMMON_HAND_ABILITY, HahUeuhAbilities.SLOTH_AUTHORITY)
                .holdBased()
                .translationKey("hahueuh.ability.summon_hand")
                .shortLabel(() -> "SUM")
                .sharesCooldownWith(HahUeuhAbilities.SLOTH_COOLDOWN_KEY)
                .onHeldTick(new SummonHandBehavior())
                .build());

        event.register(Ability.builder(HahUeuhAbilities.QUICK_STRIKE_ABILITY, HahUeuhAbilities.SLOTH_AUTHORITY)
                .holdBased()
                .translationKey("hahueuh.ability.quick_strike")
                .shortLabel(() -> "QST")
                .sharesCooldownWith(HahUeuhAbilities.SLOTH_COOLDOWN_KEY)
                .onHeldTick(new QuickStrikeBehavior())
                .build());

        event.register(Ability.builder(HahUeuhAbilities.QUICK_GRASP_ABILITY, HahUeuhAbilities.SLOTH_AUTHORITY)
                .holdBased()
                .translationKey("hahueuh.ability.quick_grasp")
                .shortLabel(() -> "QGR")
                .sharesCooldownWith(HahUeuhAbilities.SLOTH_COOLDOWN_KEY)
                .onHeldTick(new QuickGraspBehavior())
                .build());

        event.register(Ability.builder(HahUeuhAbilities.HIDDEN_INTERACTION_ABILITY, HahUeuhAbilities.SLOTH_AUTHORITY)
                .holdBased()
                .translationKey("hahueuh.ability.hidden_interaction")
                .shortLabel(() -> "HID")
                .sharesCooldownWith(HahUeuhAbilities.SLOTH_COOLDOWN_KEY)
                .availableWhen(() -> ClientSlothState.slothVariant() == SlothVariant.INVISIBLE_PROVIDENCE)
                .onHeldTick(new HiddenInteractionBehavior())
                .build());

        event.register(Ability.builder(HahUeuhAbilities.SELF_PROPEL_ABILITY, HahUeuhAbilities.SLOTH_AUTHORITY)
                .holdBased()
                .translationKey("hahueuh.ability.self_propel")
                .shortLabel(() -> "PRO")
                .sharesCooldownWith(HahUeuhAbilities.SLOTH_COOLDOWN_KEY)
                .availableWhen(() -> ClientSlothState.slothVariant() == SlothVariant.UNSEEN_HANDS)
                .onHeldTick(new SelfPropelBehavior())
                .build());

        event.register(Ability.builder(HahUeuhAbilities.LIONS_HEART_ABILITY, HahUeuhAbilities.GREED_AUTHORITY)
                .translationKey("hahueuh.ability.lions_heart")
                .shortLabel(() -> "LIO")
                .availableWhen(() -> ClientGreedState.greedVariant() == GreedVariant.LIONSHEART)
                .onActivate(ctx -> PacketDistributor.sendToServer(LionsHeartTogglePayload.INSTANCE))
                .build());

        event.register(Ability.builder(HahUeuhAbilities.LITTLE_KING_ABILITY, HahUeuhAbilities.GREED_AUTHORITY)
                .translationKey("hahueuh.ability.little_king")
                .shortLabel(() -> "KIN")
                .availableWhen(() -> ClientGreedState.greedVariant() == GreedVariant.LIONSHEART)
                .onActivate(ctx -> PacketDistributor.sendToServer(LittleKingImplantPayload.INSTANCE))
                .build());
    }
}
