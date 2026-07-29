package net.noiilive.hahueuh;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.noiilive.hahueuh.api.AbilityRegistry;
import net.noiilive.hahueuh.api.AuthorityRegistry;
import net.noiilive.hahueuh.api.event.RegisterAbilitiesEvent;
import net.noiilive.hahueuh.api.event.RegisterAuthoritiesEvent;
import net.noiilive.hahueuh.command.RezeroCommand;
import net.noiilive.hahueuh.network.ModNetworking;
import net.noiilive.hahueuh.snapshot.SnapshotManager;

@Mod(HahUeuh.MODID)
public class HahUeuh {
    public static final String MODID = "hahueuh";

    public static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    public static final SnapshotManager SNAPSHOT_MANAGER = new SnapshotManager();
    public static final ManaCharging MANA_CHARGING = new ManaCharging();
    public static final CrippledState CRIPPLED_STATE = new CrippledState();
    public static final net.noiilive.hahueuh.magic.SpellCasting SPELL_CASTING = new net.noiilive.hahueuh.magic.SpellCasting();
    public static final SlothCompatibility SLOTH_COMPAT = new SlothCompatibility();
    public static final GreedCompatibility GREED_COMPAT = new GreedCompatibility();
    public static final AllyTracker ALLY_TRACKER = new AllyTracker();
    public static final DragonSwordReid DRAGON_SWORD_REID = new DragonSwordReid();
    public static final DualWield DUAL_WIELD = new DualWield();
    public static final MobAbilityAI MOB_ABILITY_AI = new MobAbilityAI();
    public static final SpikedClubHandler SPIKED_CLUB_HANDLER = new SpikedClubHandler();
    public static final StatEffects STAT_EFFECTS = new StatEffects();
    public static final BaseShift BASE_SHIFT = new BaseShift();
    public static final SecondShift SECOND_SHIFT = new SecondShift();
    public static final LionsHeart LIONS_HEART = new LionsHeart();
    public static final LittleKing LITTLE_KING = new LittleKing();
    public static final BookOfWisdom BOOK_OF_WISDOM = new BookOfWisdom();
    public static final BookOfWisdomCopy BOOK_OF_WISDOM_COPY = new BookOfWisdomCopy();
    public static final EfficientEnchanting EFFICIENT_ENCHANTING = new EfficientEnchanting();
    public static final MentalOverload MENTAL_OVERLOAD = new MentalOverload();
    public static final VisionOfDanger VISION_OF_DANGER = new VisionOfDanger();
    public static final VisionOfLife VISION_OF_LIFE = new VisionOfLife();
    public static final VisionOfInformation VISION_OF_INFORMATION = new VisionOfInformation();
    public static final FootprintTracker FOOTPRINT_TRACKER = new FootprintTracker();
    public static final MaterialPhase MATERIAL_PHASE = new MaterialPhase();
    public static final ObjectFreeze OBJECT_FREEZE = new ObjectFreeze();
    public static final BodilyDisconnect BODILY_DISCONNECT = new BodilyDisconnect();
    public static final PocketDimension POCKET_DIMENSION = new PocketDimension();
    public static final Crystallize CRYSTALLIZE = new Crystallize();
    public static final ElMinyaChain EL_MINYA_CHAIN = new ElMinyaChain();
    public static final Murak MURAK = new Murak();
    public static final Vita VITA = new Vita();
    public static final ElVita EL_VITA = new ElVita();
    public static final IncreasedGravity INCREASED_GRAVITY = new IncreasedGravity();
    public static final Teleportation TELEPORTATION = new Teleportation();
    public static final OlShamak OL_SHAMAK = new OlShamak();
    public static final DoorCrossing DOOR_CROSSING = new DoorCrossing();
    public static final Emm EMM = new Emm();
    public static final Emt EMT = new Emt();
    public static final BookOfLifeAging BOOK_OF_LIFE_AGING = new BookOfLifeAging();
    public static final MiasmaTick MIASMA_TICK = new MiasmaTick();
    public static final MiasmaContamination MIASMA_CONTAMINATION = new MiasmaContamination();
    public static final MobWitchFactor MOB_WITCH_FACTOR = new MobWitchFactor();
    public static final PlayerAllies PLAYER_ALLIES = new PlayerAllies();
    public static final FingerGrant FINGER_GRANT = new FingerGrant();

    public HahUeuh(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModEffects.MOB_EFFECTS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(net.noiilive.hahueuh.capability.ModCapabilities::register);
        modEventBus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent e) ->
                e.put(ModEntities.WITCH_FACTOR.get(), WitchFactorEntity.createAttributes().build()));

        MinecraftForge.EVENT_BUS.register(SNAPSHOT_MANAGER);
        MinecraftForge.EVENT_BUS.register(MANA_CHARGING);
        MinecraftForge.EVENT_BUS.register(CRIPPLED_STATE);
        MinecraftForge.EVENT_BUS.register(SPELL_CASTING);
        MinecraftForge.EVENT_BUS.register(SLOTH_COMPAT);
        MinecraftForge.EVENT_BUS.register(GREED_COMPAT);
        MinecraftForge.EVENT_BUS.register(ALLY_TRACKER);
        MinecraftForge.EVENT_BUS.register(DRAGON_SWORD_REID);
        MinecraftForge.EVENT_BUS.register(DUAL_WIELD);
        MinecraftForge.EVENT_BUS.register(MOB_ABILITY_AI);
        MinecraftForge.EVENT_BUS.register(SPIKED_CLUB_HANDLER);
        MinecraftForge.EVENT_BUS.register(STAT_EFFECTS);
        MinecraftForge.EVENT_BUS.register(BASE_SHIFT);
        MinecraftForge.EVENT_BUS.register(SECOND_SHIFT);
        MinecraftForge.EVENT_BUS.register(LIONS_HEART);
        MinecraftForge.EVENT_BUS.register(LITTLE_KING);
        MinecraftForge.EVENT_BUS.register(BOOK_OF_WISDOM);
        MinecraftForge.EVENT_BUS.register(BOOK_OF_WISDOM_COPY);
        MinecraftForge.EVENT_BUS.register(EFFICIENT_ENCHANTING);
        MinecraftForge.EVENT_BUS.register(MENTAL_OVERLOAD);
        MinecraftForge.EVENT_BUS.register(VISION_OF_DANGER);
        MinecraftForge.EVENT_BUS.register(VISION_OF_LIFE);
        MinecraftForge.EVENT_BUS.register(VISION_OF_INFORMATION);
        MinecraftForge.EVENT_BUS.register(FOOTPRINT_TRACKER);
        MinecraftForge.EVENT_BUS.register(MATERIAL_PHASE);
        MinecraftForge.EVENT_BUS.register(OBJECT_FREEZE);
        MinecraftForge.EVENT_BUS.register(BODILY_DISCONNECT);
        MinecraftForge.EVENT_BUS.register(POCKET_DIMENSION);
        MinecraftForge.EVENT_BUS.register(CRYSTALLIZE);
        MinecraftForge.EVENT_BUS.register(EL_MINYA_CHAIN);
        MinecraftForge.EVENT_BUS.register(MURAK);
        MinecraftForge.EVENT_BUS.register(EL_VITA);
        MinecraftForge.EVENT_BUS.register(INCREASED_GRAVITY);
        MinecraftForge.EVENT_BUS.register(TELEPORTATION);
        MinecraftForge.EVENT_BUS.register(OL_SHAMAK);
        MinecraftForge.EVENT_BUS.register(DOOR_CROSSING);
        MinecraftForge.EVENT_BUS.register(EMM);
        MinecraftForge.EVENT_BUS.register(EMT);
        MinecraftForge.EVENT_BUS.register(BOOK_OF_LIFE_AGING);
        MinecraftForge.EVENT_BUS.register(MIASMA_TICK);
        MinecraftForge.EVENT_BUS.register(MIASMA_CONTAMINATION);
        MinecraftForge.EVENT_BUS.register(MOB_WITCH_FACTOR);
        MinecraftForge.EVENT_BUS.register(PLAYER_ALLIES);
        MinecraftForge.EVENT_BUS.register(FINGER_GRANT);
        MinecraftForge.EVENT_BUS.addListener(RezeroCommand::register);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ConfigMain.SPEC,
                "hahueuh/server/authority_main.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ConfigReturnByDeath.SPEC,
                "hahueuh/server/return_by_death.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ConfigDomain.SPEC,
                "hahueuh/server/domain.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ConfigMagic.SPEC,
                "hahueuh/server/magic_main.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ConfigPlayer.SPEC,
                "hahueuh/server/player_main.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ConfigMagicYin.SPEC,
                "hahueuh/server/magic_yin.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ConfigSloth.SPEC,
                "hahueuh/server/sloth.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ConfigGreed.SPEC,
                "hahueuh/server/greed.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetworking.register();
            ModGameRules.register();

            net.minecraftforge.fml.ModLoader.get().postEvent(new RegisterAuthoritiesEvent());
            AuthorityRegistry.freeze();
            net.minecraftforge.fml.ModLoader.get().postEvent(new RegisterAbilitiesEvent());
            AbilityRegistry.freeze();
            net.noiilive.hahueuh.magic.Spells.registerAll();
        });
    }
}
