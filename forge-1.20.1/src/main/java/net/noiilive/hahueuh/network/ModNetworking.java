package net.noiilive.hahueuh.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.noiilive.hahueuh.HahUeuh;

public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(HahUeuh.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(packetId++,
                ReturnByDeathActivatePacket.class,
                ReturnByDeathActivatePacket::encode,
                ReturnByDeathActivatePacket::new,
                ReturnByDeathActivatePacket::handle);

        CHANNEL.registerMessage(packetId++,
                DeathFadePacket.class,
                DeathFadePacket::encode,
                DeathFadePacket::new,
                DeathFadePacket::handle);

        CHANNEL.registerMessage(packetId++,
                PlayerAuthoritiesPacket.class,
                PlayerAuthoritiesPacket::encode,
                PlayerAuthoritiesPacket::new,
                PlayerAuthoritiesPacket::handle);

        CHANNEL.registerMessage(packetId++,
                AbilitySlotsSyncPacket.class,
                AbilitySlotsSyncPacket::encode,
                AbilitySlotsSyncPacket::new,
                AbilitySlotsSyncPacket::handle);

        CHANNEL.registerMessage(packetId++,
                AbilitySlotsUpdatePacket.class,
                AbilitySlotsUpdatePacket::encode,
                AbilitySlotsUpdatePacket::new,
                AbilitySlotsUpdatePacket::handle);

        CHANNEL.registerMessage(packetId++,
                AbilityCooldownPacket.class,
                AbilityCooldownPacket::encode,
                AbilityCooldownPacket::new,
                AbilityCooldownPacket::handle);

        CHANNEL.registerMessage(packetId++,
                ActivateAuthorityPacket.class,
                ActivateAuthorityPacket::encode,
                ActivateAuthorityPacket::new,
                ActivateAuthorityPacket::handle);

        CHANNEL.registerMessage(packetId++,
                DomainStatePacket.class,
                DomainStatePacket::encode,
                DomainStatePacket::new,
                DomainStatePacket::handle);

        CHANNEL.registerMessage(packetId++,
                PlayerDataSyncPacket.class,
                PlayerDataSyncPacket::encode,
                PlayerDataSyncPacket::new,
                PlayerDataSyncPacket::handle);

        CHANNEL.registerMessage(packetId++,
                ManaChargePacket.class,
                ManaChargePacket::encode,
                ManaChargePacket::new,
                ManaChargePacket::handle);

        CHANNEL.registerMessage(packetId++,
                CastSpellPacket.class,
                CastSpellPacket::encode,
                CastSpellPacket::new,
                CastSpellPacket::handle);

        CHANNEL.registerMessage(packetId++,
                FingerHighlightPacket.class,
                FingerHighlightPacket::encode,
                FingerHighlightPacket::new,
                FingerHighlightPacket::handle);

        CHANNEL.registerMessage(packetId++,
                FingerGrantPacket.class,
                FingerGrantPacket::encode,
                FingerGrantPacket::new,
                FingerGrantPacket::handle);

        CHANNEL.registerMessage(packetId++,
                UnseenHandPacket.class,
                UnseenHandPacket::encode,
                UnseenHandPacket::new,
                UnseenHandPacket::handle);

        CHANNEL.registerMessage(packetId++,
                UnseenHandSyncPacket.class,
                UnseenHandSyncPacket::encode,
                UnseenHandSyncPacket::new,
                UnseenHandSyncPacket::handle);

        CHANNEL.registerMessage(packetId++,
                UnseenHandGrabSyncPacket.class,
                UnseenHandGrabSyncPacket::encode,
                UnseenHandGrabSyncPacket::new,
                UnseenHandGrabSyncPacket::handle);

        CHANNEL.registerMessage(packetId++,
                AllyDataPacket.class,
                AllyDataPacket::encode,
                AllyDataPacket::new,
                AllyDataPacket::handle);

        CHANNEL.registerMessage(packetId++,
                AllyTrackerActivatePacket.class,
                AllyTrackerActivatePacket::encode,
                AllyTrackerActivatePacket::new,
                AllyTrackerActivatePacket::handle);

        CHANNEL.registerMessage(packetId++,
                AllyTrackerRefreshPacket.class,
                AllyTrackerRefreshPacket::encode,
                AllyTrackerRefreshPacket::new,
                AllyTrackerRefreshPacket::handle);

        CHANNEL.registerMessage(packetId++,
                AllyBurdenUpdatePacket.class,
                AllyBurdenUpdatePacket::encode,
                AllyBurdenUpdatePacket::new,
                AllyBurdenUpdatePacket::handle);

        CHANNEL.registerMessage(packetId++,
                BaseShiftStatePacket.class,
                BaseShiftStatePacket::encode,
                BaseShiftStatePacket::new,
                BaseShiftStatePacket::handle);

        CHANNEL.registerMessage(packetId++,
                BaseShiftTogglePacket.class,
                BaseShiftTogglePacket::encode,
                BaseShiftTogglePacket::new,
                BaseShiftTogglePacket::handle);

        CHANNEL.registerMessage(packetId++,
                SecondShiftStatePacket.class,
                SecondShiftStatePacket::encode,
                SecondShiftStatePacket::new,
                SecondShiftStatePacket::handle);

        CHANNEL.registerMessage(packetId++,
                SecondShiftTogglePacket.class,
                SecondShiftTogglePacket::encode,
                SecondShiftTogglePacket::new,
                SecondShiftTogglePacket::handle);

        CHANNEL.registerMessage(packetId++,
                GreedStatePacket.class,
                GreedStatePacket::encode,
                GreedStatePacket::new,
                GreedStatePacket::handle);

        CHANNEL.registerMessage(packetId++,
                LionsHeartStatePacket.class,
                LionsHeartStatePacket::encode,
                LionsHeartStatePacket::new,
                LionsHeartStatePacket::handle);

        CHANNEL.registerMessage(packetId++,
                LionsHeartTogglePacket.class,
                LionsHeartTogglePacket::encode,
                LionsHeartTogglePacket::new,
                LionsHeartTogglePacket::handle);

        CHANNEL.registerMessage(packetId++,
                LittleKingHighlightPacket.class,
                LittleKingHighlightPacket::encode,
                LittleKingHighlightPacket::new,
                LittleKingHighlightPacket::handle);

        CHANNEL.registerMessage(packetId++,
                MaterialPhaseStatePacket.class,
                MaterialPhaseStatePacket::encode,
                MaterialPhaseStatePacket::new,
                MaterialPhaseStatePacket::handle);

        CHANNEL.registerMessage(packetId++,
                MaterialPhaseTogglePacket.class,
                MaterialPhaseTogglePacket::encode,
                MaterialPhaseTogglePacket::new,
                MaterialPhaseTogglePacket::handle);

        CHANNEL.registerMessage(packetId++,
                ObjectFreezeActivatePacket.class,
                ObjectFreezeActivatePacket::encode,
                ObjectFreezeActivatePacket::new,
                ObjectFreezeActivatePacket::handle);

        CHANNEL.registerMessage(packetId++,
                LittleKingImplantPacket.class,
                LittleKingImplantPacket::encode,
                LittleKingImplantPacket::new,
                LittleKingImplantPacket::handle);

        CHANNEL.registerMessage(packetId++,
                AttackStrengthSyncPacket.class,
                AttackStrengthSyncPacket::encode,
                AttackStrengthSyncPacket::new,
                AttackStrengthSyncPacket::handle);

        CHANNEL.registerMessage(packetId++,
                BookOfWisdomTogglePacket.class,
                BookOfWisdomTogglePacket::encode,
                BookOfWisdomTogglePacket::new,
                BookOfWisdomTogglePacket::handle);

        CHANNEL.registerMessage(packetId++,
                MentalOverloadActivatePacket.class,
                MentalOverloadActivatePacket::encode,
                MentalOverloadActivatePacket::new,
                MentalOverloadActivatePacket::handle);

        CHANNEL.registerMessage(packetId++,
                VisionOfDangerStatePacket.class,
                VisionOfDangerStatePacket::encode,
                VisionOfDangerStatePacket::new,
                VisionOfDangerStatePacket::handle);

        CHANNEL.registerMessage(packetId++,
                VisionOfDangerHighlightPacket.class,
                VisionOfDangerHighlightPacket::encode,
                VisionOfDangerHighlightPacket::new,
                VisionOfDangerHighlightPacket::handle);

        CHANNEL.registerMessage(packetId++,
                VisionOfDangerTogglePacket.class,
                VisionOfDangerTogglePacket::encode,
                VisionOfDangerTogglePacket::new,
                VisionOfDangerTogglePacket::handle);

        CHANNEL.registerMessage(packetId++,
                VisionOfLifeStatePacket.class,
                VisionOfLifeStatePacket::encode,
                VisionOfLifeStatePacket::new,
                VisionOfLifeStatePacket::handle);

        CHANNEL.registerMessage(packetId++,
                VisionOfLifeGlowPacket.class,
                VisionOfLifeGlowPacket::encode,
                VisionOfLifeGlowPacket::new,
                VisionOfLifeGlowPacket::handle);

        CHANNEL.registerMessage(packetId++,
                VisionOfLifeTogglePacket.class,
                VisionOfLifeTogglePacket::encode,
                VisionOfLifeTogglePacket::new,
                VisionOfLifeTogglePacket::handle);

        CHANNEL.registerMessage(packetId++,
                FootprintSyncPacket.class,
                FootprintSyncPacket::encode,
                FootprintSyncPacket::new,
                FootprintSyncPacket::handle);

        CHANNEL.registerMessage(packetId++,
                VisionInfoQueryPacket.class,
                VisionInfoQueryPacket::encode,
                VisionInfoQueryPacket::new,
                VisionInfoQueryPacket::handle);

        CHANNEL.registerMessage(packetId++,
                VisionInfoResultPacket.class,
                VisionInfoResultPacket::encode,
                VisionInfoResultPacket::new,
                VisionInfoResultPacket::handle);

        CHANNEL.registerMessage(packetId++,
                OpenBookOfWisdomBindPacket.class,
                OpenBookOfWisdomBindPacket::encode,
                OpenBookOfWisdomBindPacket::new,
                OpenBookOfWisdomBindPacket::handle);

        CHANNEL.registerMessage(packetId++,
                ActivateBookOfWisdomVisionPacket.class,
                ActivateBookOfWisdomVisionPacket::encode,
                ActivateBookOfWisdomVisionPacket::new,
                ActivateBookOfWisdomVisionPacket::handle);

        CHANNEL.registerMessage(packetId++,
                BindVisionAbilityPacket.class,
                BindVisionAbilityPacket::encode,
                BindVisionAbilityPacket::new,
                BindVisionAbilityPacket::handle);

        CHANNEL.registerMessage(packetId++,
                OpenEfficientEnchantingPacket.class,
                OpenEfficientEnchantingPacket::encode,
                OpenEfficientEnchantingPacket::new,
                OpenEfficientEnchantingPacket::handle);

        CHANNEL.registerMessage(packetId++,
                EfficientEnchantSelectPacket.class,
                EfficientEnchantSelectPacket::encode,
                EfficientEnchantSelectPacket::new,
                EfficientEnchantSelectPacket::handle);

        CHANNEL.registerMessage(packetId++,
                EfficientEnchantOptionsPacket.class,
                EfficientEnchantOptionsPacket::encode,
                EfficientEnchantOptionsPacket::new,
                EfficientEnchantOptionsPacket::handle);

        CHANNEL.registerMessage(packetId++,
                BackToEnchantingPacket.class,
                BackToEnchantingPacket::encode,
                BackToEnchantingPacket::new,
                BackToEnchantingPacket::handle);

        CHANNEL.registerMessage(packetId++,
                SlothStatePacket.class,
                SlothStatePacket::encode,
                SlothStatePacket::new,
                SlothStatePacket::handle);

        CHANNEL.registerMessage(packetId++,
                AlShamakActivatePacket.class,
                AlShamakActivatePacket::encode,
                AlShamakActivatePacket::new,
                AlShamakActivatePacket::handle);

        CHANNEL.registerMessage(packetId++,
                StoreSpellPacket.class,
                StoreSpellPacket::encode,
                StoreSpellPacket::new,
                StoreSpellPacket::handle);

        CHANNEL.registerMessage(packetId++,
                UlMinyaActivatePacket.class,
                UlMinyaActivatePacket::encode,
                UlMinyaActivatePacket::new,
                UlMinyaActivatePacket::handle);
    }

    public static void sendToAll(Object packet) {
        CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player, Object packet) {
        CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private ModNetworking() {}
}
