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
import net.noiilive.hahueuh.network.BookOfWisdomTogglePayload;
import net.noiilive.hahueuh.network.MentalOverloadActivatePayload;
import net.noiilive.hahueuh.network.VisionOfDangerTogglePayload;
import net.noiilive.hahueuh.network.VisionOfDangerStatePayload;
import net.noiilive.hahueuh.network.VisionOfDangerHighlightPayload;
import net.noiilive.hahueuh.network.ClientVisionOfDangerState;
import net.noiilive.hahueuh.network.ClientVisionOfDangerHighlightState;
import net.noiilive.hahueuh.network.VisionOfLifeTogglePayload;
import net.noiilive.hahueuh.network.VisionOfLifeStatePayload;
import net.noiilive.hahueuh.network.VisionOfLifeGlowPayload;
import net.noiilive.hahueuh.network.ClientVisionOfLifeState;
import net.noiilive.hahueuh.network.ClientVisionOfLifeGlowState;
import net.noiilive.hahueuh.network.FootprintSyncPayload;
import net.noiilive.hahueuh.network.ClientFootprintState;
import net.noiilive.hahueuh.network.VisionInfoQueryPayload;
import net.noiilive.hahueuh.network.VisionInfoResultPayload;
import net.noiilive.hahueuh.network.VisionInfoClientData;
import net.noiilive.hahueuh.network.ClientBaseShiftState;
import net.noiilive.hahueuh.network.ClientSecondShiftState;
import net.noiilive.hahueuh.network.SecondShiftStatePayload;
import net.noiilive.hahueuh.network.SecondShiftTogglePayload;
import net.noiilive.hahueuh.network.AbilitySlotsUpdatePayload;
import net.noiilive.hahueuh.network.ActivateAuthorityPayload;
import net.noiilive.hahueuh.network.ClientDualWieldState;
import net.noiilive.hahueuh.network.ClientGreedState;
import net.noiilive.hahueuh.network.ClientSlothState;
import net.noiilive.hahueuh.network.ClientFingerState;
import net.noiilive.hahueuh.network.DeathFadePayload;
import net.noiilive.hahueuh.network.DeathFadeState;
import net.noiilive.hahueuh.network.DomainRenderState;
import net.noiilive.hahueuh.network.AttackStrengthSyncPayload;
import net.noiilive.hahueuh.network.DomainStatePayload;
import net.noiilive.hahueuh.network.ClientLionsHeartState;
import net.noiilive.hahueuh.network.ClientLittleKingState;
import net.noiilive.hahueuh.network.ClientFingerHighlightState;
import net.noiilive.hahueuh.network.LionsHeartStatePayload;
import net.noiilive.hahueuh.network.LionsHeartTogglePayload;
import net.noiilive.hahueuh.network.LittleKingHighlightPayload;
import net.noiilive.hahueuh.network.FingerHighlightPayload;
import net.noiilive.hahueuh.network.LittleKingImplantPayload;
import net.noiilive.hahueuh.network.FingerGrantPayload;
import net.noiilive.hahueuh.network.ClientMaterialPhaseState;
import net.noiilive.hahueuh.network.MaterialPhaseStatePayload;
import net.noiilive.hahueuh.network.MaterialPhaseTogglePayload;
import net.noiilive.hahueuh.network.ObjectFreezeActivatePayload;
import net.noiilive.hahueuh.network.PlayerAuthoritiesPayload;
import net.noiilive.hahueuh.network.RemoteUnseenHands;
import net.noiilive.hahueuh.network.UnseenHandGrabSyncPayload;
import net.noiilive.hahueuh.network.OpenEfficientEnchantingPayload;
import net.noiilive.hahueuh.network.BackToEnchantingPayload;
import net.noiilive.hahueuh.network.OpenBookOfWisdomBindPayload;
import net.noiilive.hahueuh.network.ActivateBookOfWisdomVisionPayload;
import net.noiilive.hahueuh.network.BindVisionAbilityPayload;
import net.noiilive.hahueuh.network.EfficientEnchantSelectPayload;
import net.noiilive.hahueuh.network.EfficientEnchantOptionsPayload;
import net.noiilive.hahueuh.client.EfficientEnchantOptionsData;
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
    public static final Insanity INSANITY = new Insanity();
    public static final Morningstar MORNINGSTAR = new Morningstar();
    public static final SlothCompatibility SLOTH_COMPAT = new SlothCompatibility();
    public static final GreedCompatibility GREED_COMPAT = new GreedCompatibility();
    public static final LionsHeart LIONS_HEART = new LionsHeart();
    public static final DualWield DUAL_WIELD = new DualWield();
    public static final LittleKing LITTLE_KING = new LittleKing();
    public static final MaterialPhase MATERIAL_PHASE = new MaterialPhase();
    public static final ObjectFreeze OBJECT_FREEZE = new ObjectFreeze();
    public static final AllyTracker ALLY_TRACKER = new AllyTracker();
    public static final BaseShift BASE_SHIFT = new BaseShift();
    public static final SecondShift SECOND_SHIFT = new SecondShift();
    public static final PlayerAllies PLAYER_ALLIES = new PlayerAllies();
    public static final FingerGrant FINGER_GRANT = new FingerGrant();
    public static final BookOfWisdom BOOK_OF_WISDOM = new BookOfWisdom();
    public static final BookOfWisdomCopy BOOK_OF_WISDOM_COPY = new BookOfWisdomCopy();
    public static final MentalOverload MENTAL_OVERLOAD = new MentalOverload();
    public static final VisionOfDanger VISION_OF_DANGER = new VisionOfDanger();
    public static final VisionOfLife VISION_OF_LIFE = new VisionOfLife();
    public static final FootprintTracker FOOTPRINT_TRACKER = new FootprintTracker();
    public static final BookOfLifeAging BOOK_OF_LIFE_AGING = new BookOfLifeAging();
    public static final ManaCharging MANA_CHARGING = new ManaCharging();
    public static final MiasmaTick MIASMA_TICK = new MiasmaTick();
    public static final MiasmaContamination MIASMA_CONTAMINATION = new MiasmaContamination();
    public static final Murak MURAK = new Murak();
    public static final Vita VITA = new Vita();
    public static final ElVita EL_VITA = new ElVita();
    public static final OlShamak OL_SHAMAK = new OlShamak();
    public static final Teleportation TELEPORTATION = new Teleportation();
    public static final DoorCrossing DOOR_CROSSING = new DoorCrossing();
    public static final Emm EMM = new Emm();
    public static final Emt EMT = new Emt();
    public static final StatEffects STAT_EFFECTS = new StatEffects();
    public static final IncreasedGravity INCREASED_GRAVITY = new IncreasedGravity();
    public static final VisionOfInformation VISION_OF_INFO = new VisionOfInformation();
    public static final EfficientEnchanting EFFICIENT_ENCHANTING = new EfficientEnchanting();
    public static final MobWitchFactor MOB_WITCH_FACTOR = new MobWitchFactor();
    public static final MobAbilityAI MOB_ABILITY_AI = new MobAbilityAI();
    public static final DragonSwordReid DRAGON_SWORD_REID = new DragonSwordReid();
    public static final net.noiilive.hahueuh.magic.SpellCasting SPELL_CASTING = new net.noiilive.hahueuh.magic.SpellCasting();
    public static final CrippledState CRIPPLED_STATE = new CrippledState();
    public static final BodilyDisconnect BODILY_DISCONNECT = new BodilyDisconnect();
    public static final PocketDimension POCKET_DIMENSION = new PocketDimension();
    public static final Crystallize CRYSTALLIZE = new Crystallize();
    public static final ElMinyaChain EL_MINYA_CHAIN = new ElMinyaChain();

    public HahUeuh(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ModBlocks.BLOCK_ENTITIES.register(modEventBus);
        ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);
        ModDataComponents.DATA_COMPONENT_TYPES.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);

        NeoForge.EVENT_BUS.register(SNAPSHOT_MANAGER);
        NeoForge.EVENT_BUS.register(INSANITY);
        NeoForge.EVENT_BUS.register(MORNINGSTAR);
        NeoForge.EVENT_BUS.register(MOB_WITCH_FACTOR);
        NeoForge.EVENT_BUS.register(MOB_ABILITY_AI);
        NeoForge.EVENT_BUS.register(DRAGON_SWORD_REID);
        NeoForge.EVENT_BUS.register(SLOTH_COMPAT);
        NeoForge.EVENT_BUS.register(GREED_COMPAT);
        NeoForge.EVENT_BUS.register(LIONS_HEART);
        NeoForge.EVENT_BUS.register(DUAL_WIELD);
        NeoForge.EVENT_BUS.register(LITTLE_KING);
        NeoForge.EVENT_BUS.register(MATERIAL_PHASE);
        NeoForge.EVENT_BUS.register(OBJECT_FREEZE);
        NeoForge.EVENT_BUS.register(ALLY_TRACKER);
        NeoForge.EVENT_BUS.register(BASE_SHIFT);
        NeoForge.EVENT_BUS.register(SECOND_SHIFT);
        NeoForge.EVENT_BUS.register(PLAYER_ALLIES);
        NeoForge.EVENT_BUS.register(FINGER_GRANT);
        NeoForge.EVENT_BUS.register(BOOK_OF_WISDOM);
        NeoForge.EVENT_BUS.register(BOOK_OF_WISDOM_COPY);
        NeoForge.EVENT_BUS.register(MENTAL_OVERLOAD);
        NeoForge.EVENT_BUS.register(VISION_OF_DANGER);
        NeoForge.EVENT_BUS.register(VISION_OF_LIFE);
        NeoForge.EVENT_BUS.register(FOOTPRINT_TRACKER);
        NeoForge.EVENT_BUS.register(VISION_OF_INFO);
        NeoForge.EVENT_BUS.register(BOOK_OF_LIFE_AGING);
        NeoForge.EVENT_BUS.register(MANA_CHARGING);
        NeoForge.EVENT_BUS.register(MIASMA_TICK);
        NeoForge.EVENT_BUS.register(MIASMA_CONTAMINATION);
        NeoForge.EVENT_BUS.register(MURAK);
        NeoForge.EVENT_BUS.register(EL_VITA);
        NeoForge.EVENT_BUS.register(OL_SHAMAK);
        NeoForge.EVENT_BUS.register(TELEPORTATION);
        NeoForge.EVENT_BUS.register(DOOR_CROSSING);
        NeoForge.EVENT_BUS.register(EMM);
        NeoForge.EVENT_BUS.register(EMT);
        NeoForge.EVENT_BUS.register(STAT_EFFECTS);
        NeoForge.EVENT_BUS.register(INCREASED_GRAVITY);
        NeoForge.EVENT_BUS.register(SPELL_CASTING);
        NeoForge.EVENT_BUS.register(CRIPPLED_STATE);
        NeoForge.EVENT_BUS.register(BODILY_DISCONNECT);
        NeoForge.EVENT_BUS.register(POCKET_DIMENSION);
        NeoForge.EVENT_BUS.register(CRYSTALLIZE);
        NeoForge.EVENT_BUS.register(EL_MINYA_CHAIN);

        NeoForge.EVENT_BUS.addListener(RezeroCommand::register);

        modEventBus.addListener(HahUeuh::registerPayloads);

        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(ModGameRules::register));

        modEventBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) ->
                event.put(ModEntities.WITCH_FACTOR.get(), WitchFactorEntity.createAttributes().build()));

        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> {
            modEventBus.post(new RegisterAuthoritiesEvent());
            AuthorityRegistry.freeze();
            modEventBus.post(new RegisterAbilitiesEvent());
            AbilityRegistry.freeze();
            net.noiilive.hahueuh.magic.Spells.registerAll();
        });

        modContainer.registerConfig(ModConfig.Type.SERVER, ConfigMain.SPEC, "hahueuh/server/authority_main.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, ConfigReturnByDeath.SPEC, "hahueuh/server/return_by_death.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, ConfigDomain.SPEC, "hahueuh/server/domain.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, ConfigSloth.SPEC, "hahueuh/server/sloth.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, ConfigGreed.SPEC, "hahueuh/server/greed.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, ConfigPlayer.SPEC, "hahueuh/server/player_main.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, ConfigMagic.SPEC, "hahueuh/server/magic_main.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, ConfigMagicYin.SPEC, "hahueuh/server/magic_yin.toml");
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
                AttackStrengthSyncPayload.TYPE,
                AttackStrengthSyncPayload.STREAM_CODEC,
                (payload, context) -> {
                    ClientDualWieldState.setOffhandNext(payload.offhandNext());
                    DualWield.applyIndicator(context.player(), payload.scale());
                });

        registrar.playToClient(
                PlayerAuthoritiesPayload.TYPE,
                PlayerAuthoritiesPayload.STREAM_CODEC,
                (payload, context) -> {
                    OwnershipState.setAuthorityOwned(HahUeuhAbilities.RETURN_BY_DEATH_AUTHORITY, payload.returnByDeath());
                    OwnershipState.setAuthorityOwned(HahUeuhAbilities.DOMAIN_AUTHORITY, payload.domain());
                    OwnershipState.setAuthorityOwned(HahUeuhAbilities.SLOTH_AUTHORITY, payload.sloth());
                    ClientSlothState.update(payload.sloth(), payload.slothVariant(), payload.slothHandCount());
                    OwnershipState.setAuthorityOwned(HahUeuhAbilities.GREED_AUTHORITY, payload.greed());
                    ClientGreedState.update(payload.greed(), payload.greedVariant());
                    ClientFingerState.setHands(payload.fingerHandCount());
                    OwnershipState.setAuthorityOwned(HahUeuhAbilities.FINGER_AUTHORITY, payload.fingerHandCount() > 0);
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
                net.noiilive.hahueuh.network.ReturnByDeathActivatePayload.TYPE,
                net.noiilive.hahueuh.network.ReturnByDeathActivatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        SNAPSHOT_MANAGER.handleReturnByDeathActivate(sp);
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

        registrar.playToServer(
                net.noiilive.hahueuh.network.MorningstarSwingPayload.TYPE,
                net.noiilive.hahueuh.network.MorningstarSwingPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        MORNINGSTAR.handleSwing(sp, payload.spin());
                    }
                });

        registrar.playToClient(
                net.noiilive.hahueuh.network.MorningstarSwingSyncPayload.TYPE,
                net.noiilive.hahueuh.network.MorningstarSwingSyncPayload.STREAM_CODEC,
                (payload, context) -> net.noiilive.hahueuh.client.MorningstarClient.applyRemoteSwing(payload.owner(), payload.spin()));

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
                        Miasma.addSingleUse(sp);
                    }
                });

        registrar.playToServer(
                FingerGrantPayload.TYPE,
                FingerGrantPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        FINGER_GRANT.grant(sp);
                        Miasma.addSingleUse(sp);
                    }
                });

        registrar.playToServer(
                net.noiilive.hahueuh.network.ManaChargePayload.TYPE,
                net.noiilive.hahueuh.network.ManaChargePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        MANA_CHARGING.setCharging(sp, payload.charging());
                    }
                });

        registrar.playToServer(
                net.noiilive.hahueuh.network.CastSpellPayload.TYPE,
                net.noiilive.hahueuh.network.CastSpellPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        net.noiilive.hahueuh.magic.SpellRegistry.get(payload.spellId())
                                .ifPresent(spell -> SPELL_CASTING.tryStart(sp, spell));
                    }
                });

        registrar.playToServer(
                net.noiilive.hahueuh.network.AlShamakActivatePayload.TYPE,
                net.noiilive.hahueuh.network.AlShamakActivatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        if (payload.mode() == net.noiilive.hahueuh.network.AlShamakActivatePayload.RELEASE) {
                            SPELL_CASTING.releaseStoredSpell(sp, payload.targetEntityId());
                        } else if (payload.mode() == net.noiilive.hahueuh.network.AlShamakActivatePayload.DISCARD) {
                            SPELL_CASTING.discardAlShamak(sp);
                        } else {
                            SPELL_CASTING.beginBanish(sp, payload.targetEntityId());
                        }
                    }
                });

        registrar.playToServer(
                net.noiilive.hahueuh.network.StoreSpellPayload.TYPE,
                net.noiilive.hahueuh.network.StoreSpellPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        SPELL_CASTING.beginStoreCast(sp, payload.spellId());
                    }
                });

        registrar.playToServer(
                net.noiilive.hahueuh.network.UlMinyaActivatePayload.TYPE,
                net.noiilive.hahueuh.network.UlMinyaActivatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        SPELL_CASTING.beginUlMinya(sp, payload.targetEntityId());
                    }
                });

        registrar.playToClient(
                LittleKingHighlightPayload.TYPE,
                LittleKingHighlightPayload.STREAM_CODEC,
                (payload, context) -> ClientLittleKingState.set(payload.entityIds()));

        registrar.playToClient(
                FingerHighlightPayload.TYPE,
                FingerHighlightPayload.STREAM_CODEC,
                (payload, context) -> ClientFingerHighlightState.set(payload.entityIds()));

        registrar.playToServer(
                net.noiilive.hahueuh.network.MurakActivatePayload.TYPE,
                net.noiilive.hahueuh.network.MurakActivatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        MURAK.tryCast(sp);
                    }
                });

        registrar.playToServer(
                net.noiilive.hahueuh.network.VitaActivatePayload.TYPE,
                net.noiilive.hahueuh.network.VitaActivatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        VITA.tryCast(sp);
                    }
                });

        registrar.playToServer(
                net.noiilive.hahueuh.network.ElVitaActivatePayload.TYPE,
                net.noiilive.hahueuh.network.ElVitaActivatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        EL_VITA.tryCast(sp);
                    }
                });

        registrar.playToServer(
                net.noiilive.hahueuh.network.EmtActivatePayload.TYPE,
                net.noiilive.hahueuh.network.EmtActivatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        EMT.tryCast(sp);
                    }
                });

        registrar.playToClient(
                net.noiilive.hahueuh.network.EmtStatePayload.TYPE,
                net.noiilive.hahueuh.network.EmtStatePayload.STREAM_CODEC,
                (payload, context) -> net.noiilive.hahueuh.network.EmtRenderState.update(payload));

        registrar.playToServer(
                net.noiilive.hahueuh.network.EmmActivatePayload.TYPE,
                net.noiilive.hahueuh.network.EmmActivatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        EMM.tryCast(sp);
                    }
                });

        registrar.playToServer(
                net.noiilive.hahueuh.network.TeleportCastPayload.TYPE,
                net.noiilive.hahueuh.network.TeleportCastPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        TELEPORTATION.request(sp, payload.x(), payload.y(), payload.z(), payload.portal());
                    }
                });

        registrar.playToServer(
                net.noiilive.hahueuh.network.DoorCrossingActivatePayload.TYPE,
                net.noiilive.hahueuh.network.DoorCrossingActivatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        DOOR_CROSSING.tryCast(sp);
                    }
                });

        registrar.playToServer(
                net.noiilive.hahueuh.network.MurakFlightTogglePayload.TYPE,
                net.noiilive.hahueuh.network.MurakFlightTogglePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        MURAK.setFlying(sp, payload.wantsFlight());
                    }
                });

        registrar.playToClient(
                net.noiilive.hahueuh.network.MurakStatePayload.TYPE,
                net.noiilive.hahueuh.network.MurakStatePayload.STREAM_CODEC,
                (payload, context) -> net.noiilive.hahueuh.network.ClientMurakState.update(
                        payload.reducedGravity(), payload.flying()));

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
                        Miasma.addSingleUse(sp);
                    }
                });

        registrar.playToServer(
                AllyTrackerActivatePayload.TYPE,
                AllyTrackerActivatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        ALLY_TRACKER.activate(sp);
                        Miasma.addSingleUse(sp);
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

        registrar.playToServer(
                BookOfWisdomTogglePayload.TYPE,
                BookOfWisdomTogglePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        BOOK_OF_WISDOM.toggle(sp);
                    }
                });

        registrar.playToServer(
                MentalOverloadActivatePayload.TYPE,
                MentalOverloadActivatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        MENTAL_OVERLOAD.activate(sp);
                        Miasma.addSingleUse(sp);
                    }
                });

        registrar.playToServer(
                VisionOfDangerTogglePayload.TYPE,
                VisionOfDangerTogglePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        VISION_OF_DANGER.toggle(sp);
                    }
                });

        registrar.playToClient(
                VisionOfDangerStatePayload.TYPE,
                VisionOfDangerStatePayload.STREAM_CODEC,
                (payload, context) -> ClientVisionOfDangerState.setActive(payload.active()));

        registrar.playToClient(
                VisionOfDangerHighlightPayload.TYPE,
                VisionOfDangerHighlightPayload.STREAM_CODEC,
                (payload, context) -> ClientVisionOfDangerHighlightState.set(payload.entityIds()));

        registrar.playToServer(
                VisionOfLifeTogglePayload.TYPE,
                VisionOfLifeTogglePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        VISION_OF_LIFE.toggle(sp);
                    }
                });

        registrar.playToClient(
                VisionOfLifeStatePayload.TYPE,
                VisionOfLifeStatePayload.STREAM_CODEC,
                (payload, context) -> ClientVisionOfLifeState.setActive(payload.active()));

        registrar.playToClient(
                VisionOfLifeGlowPayload.TYPE,
                VisionOfLifeGlowPayload.STREAM_CODEC,
                (payload, context) -> ClientVisionOfLifeGlowState.set(payload.hostileIds(), payload.passiveIds(), payload.playerIds(), payload.witchFactorIds()));

        registrar.playToClient(
                FootprintSyncPayload.TYPE,
                FootprintSyncPayload.STREAM_CODEC,
                (payload, context) -> ClientFootprintState.set(payload.maxAgeTicks(), payload.footprints()));

        registrar.playToServer(
                VisionInfoQueryPayload.TYPE,
                VisionInfoQueryPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        VISION_OF_INFO.handleQuery(sp, payload);
                    }
                });

        registrar.playToClient(
                VisionInfoResultPayload.TYPE,
                VisionInfoResultPayload.STREAM_CODEC,
                (payload, context) -> VisionInfoClientData.set(payload));

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
                        SNAPSHOT_MANAGER.onUnseenHandUpdate(sp, payload.active(), payload.distance(), payload.mode(), payload.mobility(), payload.quickSession());
                    }
                });

        registrar.playToClient(
                UnseenHandSyncPayload.TYPE,
                UnseenHandSyncPayload.STREAM_CODEC,
                (payload, context) -> RemoteUnseenHands.update(payload.owner(), payload.entityId(), payload.active(), payload.distance(), payload.mode(), payload.variant(), payload.mobility(), payload.count()));

        registrar.playToClient(
                UnseenHandGrabSyncPayload.TYPE,
                UnseenHandGrabSyncPayload.STREAM_CODEC,
                (payload, context) -> RemoteUnseenHands.updateGrabbed(payload.owner(), payload.grabbedIds()));

        registrar.playToServer(
                OpenEfficientEnchantingPayload.TYPE,
                OpenEfficientEnchantingPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        EFFICIENT_ENCHANTING.open(sp);
                    }
                });

        registrar.playToServer(
                EfficientEnchantSelectPayload.TYPE,
                EfficientEnchantSelectPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        EFFICIENT_ENCHANTING.select(sp, payload.enchantmentId());
                    }
                });

        registrar.playToClient(
                EfficientEnchantOptionsPayload.TYPE,
                EfficientEnchantOptionsPayload.STREAM_CODEC,
                (payload, context) -> EfficientEnchantOptionsData.set(payload.options()));

        registrar.playToServer(
                BackToEnchantingPayload.TYPE,
                BackToEnchantingPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        EFFICIENT_ENCHANTING.goBack(sp);
                    }
                });

        registrar.playToServer(
                OpenBookOfWisdomBindPayload.TYPE,
                OpenBookOfWisdomBindPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        BOOK_OF_WISDOM_COPY.openBindMenu(sp);
                    }
                });

        registrar.playToServer(
                ActivateBookOfWisdomVisionPayload.TYPE,
                ActivateBookOfWisdomVisionPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        BOOK_OF_WISDOM_COPY.activateBoundAbility(sp);
                    }
                });

        registrar.playToServer(
                BindVisionAbilityPayload.TYPE,
                BindVisionAbilityPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        BOOK_OF_WISDOM_COPY.bind(sp, payload.abilityOrdinal());
                    }
                });
    }
}
