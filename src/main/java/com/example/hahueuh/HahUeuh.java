package com.example.hahueuh;

import com.example.hahueuh.api.AbilityCooldowns;
import com.example.hahueuh.api.AbilityRegistry;
import com.example.hahueuh.api.AuthorityRegistry;
import com.example.hahueuh.api.OwnershipState;
import com.example.hahueuh.api.event.RegisterAbilitiesEvent;
import com.example.hahueuh.api.event.RegisterAuthoritiesEvent;
import com.example.hahueuh.command.RezeroCommand;
import com.example.hahueuh.network.AbilityCooldownPayload;
import com.example.hahueuh.network.ActivateAuthorityPayload;
import com.example.hahueuh.network.ClientSlothState;
import com.example.hahueuh.network.DeathFadePayload;
import com.example.hahueuh.network.DeathFadeState;
import com.example.hahueuh.network.DomainRenderState;
import com.example.hahueuh.network.DomainStatePayload;
import com.example.hahueuh.network.PlayerAuthoritiesPayload;
import com.example.hahueuh.network.RemoteUnseenHands;
import com.example.hahueuh.network.UnseenHandGrabSyncPayload;
import com.example.hahueuh.network.UnseenHandPayload;
import com.example.hahueuh.network.UnseenHandSyncPayload;
import com.example.hahueuh.snapshot.SnapshotManager;
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

    public HahUeuh(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);

        NeoForge.EVENT_BUS.register(SNAPSHOT_MANAGER);
        NeoForge.EVENT_BUS.register(SLOTH_COMPAT);

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

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
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
                });

        registrar.playToClient(
                AbilityCooldownPayload.TYPE,
                AbilityCooldownPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (AbilityRegistry.get(payload.abilityId()).isPresent()) {
                        AbilityCooldowns.startCooldown(payload.abilityId(), payload.remainingTicks() / 20.0);
                    } else {
                        LOGGER.warn("Received AbilityCooldownPayload for unknown ability id {}", payload.abilityId());
                    }
                });

        registrar.playToServer(
                ActivateAuthorityPayload.TYPE,
                ActivateAuthorityPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        SNAPSHOT_MANAGER.toggleDomain(sp);
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
