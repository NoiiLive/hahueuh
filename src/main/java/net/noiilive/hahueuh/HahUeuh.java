package net.noiilive.hahueuh;

import net.noiilive.hahueuh.api.AbilityCooldowns;
import net.noiilive.hahueuh.api.AbilityRegistry;
import net.noiilive.hahueuh.api.AuthorityRegistry;
import net.noiilive.hahueuh.api.OwnershipState;
import net.noiilive.hahueuh.api.event.RegisterAbilitiesEvent;
import net.noiilive.hahueuh.api.event.RegisterAuthoritiesEvent;
import net.noiilive.hahueuh.command.RezeroCommand;
import net.noiilive.hahueuh.client.AbilitySlots;
import net.noiilive.hahueuh.network.AbilityCooldownPayload;
import net.noiilive.hahueuh.network.AbilitySlotsSyncPayload;
import net.noiilive.hahueuh.network.AllyBurdenUpdatePayload;
import net.noiilive.hahueuh.network.AllyDataPayload;
import net.noiilive.hahueuh.network.AllyTrackerActivatePayload;
import net.noiilive.hahueuh.network.AllyTrackerData;
import net.noiilive.hahueuh.network.AllyTrackerRefreshPayload;
import net.noiilive.hahueuh.network.BaseShiftStatePayload;
import net.noiilive.hahueuh.network.BaseShiftTogglePayload;
import net.noiilive.hahueuh.network.ClientBaseShiftState;
import net.noiilive.hahueuh.network.ClientSecondShiftState;
import net.noiilive.hahueuh.network.SecondShiftStatePayload;
import net.noiilive.hahueuh.network.SecondShiftTogglePayload;
import net.noiilive.hahueuh.network.AbilitySlotsUpdatePayload;
import net.noiilive.hahueuh.network.ActivateAuthorityPayload;
import net.noiilive.hahueuh.network.ClientGreedState;
import net.noiilive.hahueuh.network.ClientSlothState;
import net.noiilive.hahueuh.network.DeathFadePayload;
import net.noiilive.hahueuh.network.DeathFadeState;
import net.noiilive.hahueuh.network.DomainRenderState;
import net.noiilive.hahueuh.network.DomainStatePayload;
import net.noiilive.hahueuh.network.ClientLionsHeartState;
import net.noiilive.hahueuh.network.ClientLittleKingState;
import net.noiilive.hahueuh.network.LionsHeartStatePayload;
import net.noiilive.hahueuh.network.LionsHeartTogglePayload;
import net.noiilive.hahueuh.network.LittleKingHighlightPayload;
import net.noiilive.hahueuh.network.LittleKingImplantPayload;
import net.noiilive.hahueuh.network.ClientMaterialPhaseState;
import net.noiilive.hahueuh.network.MaterialPhaseStatePayload;
import net.noiilive.hahueuh.network.MaterialPhaseTogglePayload;
import net.noiilive.hahueuh.network.ObjectFreezeActivatePayload;
import net.noiilive.hahueuh.network.PlayerAuthoritiesPayload;
import net.noiilive.hahueuh.network.RemoteUnseenHands;
import net.noiilive.hahueuh.network.UnseenHandGrabSyncPayload;
import net.noiilive.hahueuh.network.UnseenHandPayload;
import net.noiilive.hahueuh.network.UnseenHandSyncPayload;
import net.noiilive.hahueuh.snapshot.SnapshotManager;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(HahUeuh.MODID)
public class HahUeuh {
    public static final String MODID = "hahueuh";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final SnapshotManager SNAPSHOT_MANAGER = new SnapshotManager();
    public static final SlothCompatibility SLOTH_COMPAT = new SlothCompatibility();
    public static final GreedCompatibility GREED_COMPAT = new GreedCompatibility();
    public static final LionsHeart LIONS_HEART = new LionsHeart();
    public static final LittleKing LITTLE_KING = new LittleKing();
    public static final MaterialPhase MATERIAL_PHASE = new MaterialPhase();
    public static final ObjectFreeze OBJECT_FREEZE = new ObjectFreeze();
    public static final AllyTracker ALLY_TRACKER = new AllyTracker();
    public static final BaseShift BASE_SHIFT = new BaseShift();
    public static final SecondShift SECOND_SHIFT = new SecondShift();
    public static final PlayerAllies PLAYER_ALLIES = new PlayerAllies();

    public HahUeuh(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);

        NeoForge.EVENT_BUS.register(SNAPSHOT_MANAGER);
        NeoForge.EVENT_BUS.register(SLOTH_COMPAT);
        NeoForge.EVENT_BUS.register(GREED_COMPAT);
        NeoForge.EVENT_BUS.register(LIONS_HEART);
        NeoForge.EVENT_BUS.register(LITTLE_KING);
        NeoForge.EVENT_BUS.register(MATERIAL_PHASE);
        NeoForge.EVENT_BUS.register(OBJECT_FREEZE);
        NeoForge.EVENT_BUS.register(ALLY_TRACKER);
        NeoForge.EVENT_BUS.register(BASE_SHIFT);
        NeoForge.EVENT_BUS.register(SECOND_SHIFT);
        NeoForge.EVENT_BUS.register(PLAYER_ALLIES);

        NeoForge.EVENT_BUS.addListener(RezeroCommand::register);

        modEventBus.addListener(HahUeuh::registerPayloads);

        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(ModGameRules::register));

        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> {
            modEventBus.post(new RegisterAuthoritiesEvent());
            AuthorityRegistry.freeze();
            modEventBus.post(new RegisterAbilitiesEvent());
            AbilityRegistry.freeze();
        });

        modContainer.registerConfig(ModConfig.Type.COMMON, ConfigReturnByDeath.SPEC, "hahueuh/server/return_by_death.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, ConfigDomain.SPEC, "hahueuh/server/domain.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, ConfigSloth.SPEC, "hahueuh/server/sloth.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, ConfigGreed.SPEC, "hahueuh/server/greed.toml");
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        registrar.playToClient(
                DeathFadePayload.TYPE,
                DeathFadePayload.STREAM_CODEC,
                (payload, context) -> DeathFadeState.onSignal(payload.toBlack()));

        registrar.playToClient(
                DomainStatePayload.TYPE,
                DomainStatePayload.STREAM_CODEC,
                (payload, context) -> DomainRenderState.update(payload));

        registrar.playToClient(
                PlayerAuthoritiesPayload.TYPE,
                PlayerAuthoritiesPayload.STREAM_CODEC,
                (payload, context) -> {
                    OwnershipState.setAuthorityOwned(HahUeuhAbilities.RETURN_BY_DEATH_AUTHORITY, payload.returnByDeath());
                    OwnershipState.setAuthorityOwned(HahUeuhAbilities.DOMAIN_AUTHORITY, payload.domain());
                    OwnershipState.setAuthorityOwned(HahUeuhAbilities.SLOTH_AUTHORITY, payload.sloth());
                    ClientSlothState.update(payload.sloth(), payload.slothVariant());
                    OwnershipState.setAuthorityOwned(HahUeuhAbilities.GREED_AUTHORITY, payload.greed());
                    ClientGreedState.update(payload.greed(), payload.greedVariant());
                });

        registrar.playToClient(
                AbilityCooldownPayload.TYPE,
                AbilityCooldownPayload.STREAM_CODEC,
                (payload, context) -> AbilityCooldowns.startCooldown(payload.abilityId(), payload.remainingTicks() / 20.0));

        registrar.playToServer(
                ActivateAuthorityPayload.TYPE,
                ActivateAuthorityPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        SNAPSHOT_MANAGER.toggleDomain(sp, payload.aggressor());
                    }
                });

        registrar.playToServer(
                LionsHeartTogglePayload.TYPE,
                LionsHeartTogglePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        LIONS_HEART.toggle(sp);
                    }
                });

        registrar.playToClient(
                LionsHeartStatePayload.TYPE,
                LionsHeartStatePayload.STREAM_CODEC,
                (payload, context) -> ClientLionsHeartState.setActive(payload.active()));

        registrar.playToServer(
                LittleKingImplantPayload.TYPE,
                LittleKingImplantPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        LITTLE_KING.implant(sp);
                    }
                });

        registrar.playToClient(
                LittleKingHighlightPayload.TYPE,
                LittleKingHighlightPayload.STREAM_CODEC,
                (payload, context) -> ClientLittleKingState.set(payload.entityIds()));

        registrar.playToServer(
                MaterialPhaseTogglePayload.TYPE,
                MaterialPhaseTogglePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        MATERIAL_PHASE.toggle(sp);
                    }
                });

        registrar.playToClient(
                MaterialPhaseStatePayload.TYPE,
                MaterialPhaseStatePayload.STREAM_CODEC,
                (payload, context) -> ClientMaterialPhaseState.setActive(payload.active()));

        registrar.playToServer(
                ObjectFreezeActivatePayload.TYPE,
                ObjectFreezeActivatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        OBJECT_FREEZE.activate(sp);
                    }
                });

        registrar.playToServer(
                AllyTrackerActivatePayload.TYPE,
                AllyTrackerActivatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        ALLY_TRACKER.activate(sp);
                    }
                });

        registrar.playToServer(
                AllyTrackerRefreshPayload.TYPE,
                AllyTrackerRefreshPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        ALLY_TRACKER.sendRefresh(sp);
                    }
                });

        registrar.playToServer(
                AllyBurdenUpdatePayload.TYPE,
                AllyBurdenUpdatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        ALLY_TRACKER.updateBurden(sp, payload.selfWeight(), payload.allyWeights());
                    }
                });

        registrar.playToClient(
                AllyDataPayload.TYPE,
                AllyDataPayload.STREAM_CODEC,
                (payload, context) -> AllyTrackerData.receive(payload));

        registrar.playToServer(
                BaseShiftTogglePayload.TYPE,
                BaseShiftTogglePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        BASE_SHIFT.toggle(sp);
                    }
                });

        registrar.playToClient(
                BaseShiftStatePayload.TYPE,
                BaseShiftStatePayload.STREAM_CODEC,
                (payload, context) -> ClientBaseShiftState.setActive(payload.active()));

        registrar.playToServer(
                SecondShiftTogglePayload.TYPE,
                SecondShiftTogglePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        SECOND_SHIFT.toggle(sp);
                    }
                });

        registrar.playToClient(
                SecondShiftStatePayload.TYPE,
                SecondShiftStatePayload.STREAM_CODEC,
                (payload, context) -> ClientSecondShiftState.setActive(payload.active()));

        registrar.playToClient(
                AbilitySlotsSyncPayload.TYPE,
                AbilitySlotsSyncPayload.STREAM_CODEC,
                (payload, context) -> AbilitySlots.applyFromServer(payload.data()));

        registrar.playToServer(
                AbilitySlotsUpdatePayload.TYPE,
                AbilitySlotsUpdatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        SNAPSHOT_MANAGER.getAbilitySlotsManager().update(sp.getUUID(), payload.data());
                    }
                });

        registrar.playToServer(
                UnseenHandPayload.TYPE,
                UnseenHandPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        SNAPSHOT_MANAGER.onUnseenHandUpdate(sp, payload.active(), payload.distance(), payload.mode(), payload.mobility());
                    }
                });

        registrar.playToClient(
                UnseenHandSyncPayload.TYPE,
                UnseenHandSyncPayload.STREAM_CODEC,
                (payload, context) -> RemoteUnseenHands.update(payload.owner(), payload.active(), payload.distance(), payload.mode(), payload.variant(), payload.mobility()));

        registrar.playToClient(
                UnseenHandGrabSyncPayload.TYPE,
                UnseenHandGrabSyncPayload.STREAM_CODEC,
                (payload, context) -> RemoteUnseenHands.updateGrabbed(payload.owner(), payload.grabbedIds()));
    }
}
