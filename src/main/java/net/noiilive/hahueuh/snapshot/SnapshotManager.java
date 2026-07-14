package net.noiilive.hahueuh.snapshot;

import net.noiilive.hahueuh.ConfigDomain;
import net.noiilive.hahueuh.ConfigMain;
import net.noiilive.hahueuh.ConfigReturnByDeath;
import net.noiilive.hahueuh.ConfigSloth;
import net.noiilive.hahueuh.FootprintTracker;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.ModEffects;
import net.noiilive.hahueuh.ModEntities;
import net.noiilive.hahueuh.ModGameRules;
import net.noiilive.hahueuh.ModSounds;
import net.noiilive.hahueuh.HahUeuhAbilities;
import net.noiilive.hahueuh.WitchFactorEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.noiilive.hahueuh.network.AbilityCooldownPayload;
import net.noiilive.hahueuh.network.AbilitySlotsSyncPayload;
import net.noiilive.hahueuh.network.DeathFadePayload;
import net.noiilive.hahueuh.network.DeathFadeState;
import net.noiilive.hahueuh.network.DomainStatePayload;
import net.noiilive.hahueuh.network.HandMode;
import net.noiilive.hahueuh.network.SlothVariant;
import net.noiilive.hahueuh.network.PlayerAuthoritiesPayload;
import net.noiilive.hahueuh.network.UnseenHandGrabSyncPayload;
import net.noiilive.hahueuh.network.UnseenHandSyncPayload;
import net.noiilive.hahueuh.network.WitchFactorAuthority;
import com.mojang.logging.LogUtils;
import net.minecraft.util.Mth;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SnapshotManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();

    private static final int DIMENSION_SETTLE_SECONDS = 1;

    private static final Set<String> MOD_METADATA_FILES = Set.of(
            "hahueuh_authority.json",
            "hahueuh_timer_state.txt");

    private static final Codec<PalettedContainer<BlockState>> BLOCK_STATE_CODEC = PalettedContainer.codecRW(
            Block.BLOCK_STATE_REGISTRY,
            BlockState.CODEC,
            PalettedContainer.Strategy.SECTION_STATES,
            Blocks.AIR.defaultBlockState()
    );

    private static final class CheckpointSlot {
        final String dirName;
        WorldSnapshot snapshot;
        final Map<ResourceKey<Level>, Set<Long>> modifiedChunks = new ConcurrentHashMap<>();
        final Map<ResourceKey<Level>, Set<Long>> capturedChunks = new ConcurrentHashMap<>();

        CheckpointSlot(String dirName) { this.dirName = dirName; }

        boolean isActive() { return snapshot != null; }

        void resetTracking() { modifiedChunks.clear(); capturedChunks.clear(); }

        void clear() { snapshot = null; resetTracking(); }
    }

    private final CheckpointSlot rbd = new CheckpointSlot("hahueuh_checkpoint");
    private final CheckpointSlot domain = new CheckpointSlot("hahueuh_domain_checkpoint");
    private int tickCounter;
    private int nextCheckpointIntervalTicks;
    private long rollbackAtTick = -1;
    private CheckpointSlot pendingRollbackSlot;
    private boolean rollbackInProgress;
    private MinecraftServer server;
    private UUID domainOwnerUuid;
    private UUID domainSubjectUuid;
    private Vec3 domainMatrix;
    private ResourceKey<Level> domainDimension;
    private boolean domainCasterDeadHardcore;
    private final Map<UUID, Integer> domainCooldownUntilTick = new HashMap<>();
    private final Map<UUID, UnseenHand> unseenHands = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> slothCooldownUntilTick = new HashMap<>();
    private final Map<UUID, Integer> quickCooldownUntilTick = new HashMap<>();

    private static final float INVIS_PROVIDENCE_MAX_GRAB_SIZE = 1.5f;

    private static final class UnseenHand {
        float distance;
        HandMode mode = HandMode.NONE;
        UUID[] grabbed = new UUID[0];
        BlockPos lastInteractBlock;
        boolean mobility;
        boolean quickSession;
        UUID[] lastBroadcastGrabbed = new UUID[0];
    }

    private static void ensureGrabSlots(UnseenHand hand, int n) {
        if (hand.grabbed.length != n) hand.grabbed = new UUID[n];
    }
    private final PlayerAuthorityManager authorityManager = new PlayerAuthorityManager();
    private final AbilitySlotsManager abilitySlotsManager = new AbilitySlotsManager();
    private long lastAutoCheckpointGameTick = -1;

    private long suppressAutoCheckpointsUntilTick = -1;

    private String deferredCheckpointReason;

    private final Map<UUID, ResourceKey<Level>> lastPlayerDimension = new HashMap<>();

    private volatile boolean internalSaveInProgress;

    private long ueuhPlayAtTick = -1;
    private long targetingSuppressUntilTick = -1;


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
        this.rollbackAtTick = -1;
        this.rollbackInProgress = false;
        this.rbd.clear();
        this.domain.clear();
        this.deactivateDomainState();
        this.domainCooldownUntilTick.clear();
        this.slothCooldownUntilTick.clear();
        this.quickCooldownUntilTick.clear();
        this.pendingMiasmaBump.clear();
        this.unseenHands.clear();
        this.authorityManager.load(this.server);
        this.abilitySlotsManager.load(this.server);
        this.ueuhPlayAtTick = -1;
        this.targetingSuppressUntilTick = -1;
        this.internalSaveInProgress = false;
        this.suppressAutoCheckpointsUntilTick = -1;
        this.deferredCheckpointReason = null;
        this.lastPlayerDimension.clear();
        rollNextCheckpointInterval();
        this.tickCounter = Math.min(loadPersistedTickCounter(this.server), nextCheckpointIntervalTicks);
        LOGGER.info("HahUeuh checkpoint system initialized (interval: {}s +/- {}s, resuming timer at {}/{} ticks)",
                ConfigReturnByDeath.CHECKPOINT_INTERVAL_SECONDS.getAsInt(), ConfigReturnByDeath.CHECKPOINT_INTERVAL_RANDOMNESS_SECONDS.getAsInt(),
                tickCounter, nextCheckpointIntervalTicks);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        Path checkpointDir = worldDir.resolve(rbd.dirName);

        Path domainDir = worldDir.resolve(domain.dirName);
        if (Files.exists(domainDir)) {
            try { deleteDirectory(domainDir); } catch (Exception e) {
                LOGGER.warn("Failed to delete stale domain checkpoint on startup", e);
            }
        }

        if (Files.exists(checkpointDir)) {
            LOGGER.info("Found existing checkpoint on disk — reloading metadata...");
            try {
                rebuildSnapshotFromDisk(checkpointDir);
                LOGGER.info("Checkpoint metadata reloaded successfully.");
            } catch (Exception e) {
                LOGGER.error("Failed to reload checkpoint from disk — will create a fresh one.", e);
                createSnapshot(rbd, "server-start-fallback");
            }
        } else {
            LOGGER.info("No checkpoint found — creating initial checkpoint for new world.");
            createSnapshot(rbd, "server-start");
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        persistTickCounter(this.server);
        this.server = null;
        this.rbd.clear();
        this.domain.clear();
    }

    private static final String TIMER_STATE_FILE_NAME = "hahueuh_timer_state.txt";

    private void persistTickCounter(MinecraftServer server) {
        try {
            Path path = server.getWorldPath(LevelResource.ROOT).resolve(TIMER_STATE_FILE_NAME);
            Files.writeString(path, Integer.toString(tickCounter), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.warn("Failed to persist checkpoint timer state", e);
        }
    }

    private int loadPersistedTickCounter(MinecraftServer server) {
        try {
            Path path = server.getWorldPath(LevelResource.ROOT).resolve(TIMER_STATE_FILE_NAME);
            if (!Files.exists(path)) return 0;
            return Integer.parseInt(Files.readString(path, StandardCharsets.UTF_8).trim());
        } catch (Exception e) {
            LOGGER.warn("Failed to load persisted checkpoint timer state, starting fresh", e);
            return 0;
        }
    }

    private void rebuildSnapshotFromDisk(Path checkpointDir) throws IOException {
        Map<Path, Long> fileTimestamps = new HashMap<>();
        Files.walkFileTree(checkpointDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                Path relativePath = checkpointDir.relativize(file);
                if (relativePath.toString().equals(PLAYER_SNAPSHOTS_FILE_NAME)) {
                    return FileVisitResult.CONTINUE;
                }
                fileTimestamps.put(relativePath, attrs.lastModifiedTime().toMillis());
                return FileVisitResult.CONTINUE;
            }
        });

        Map<ResourceKey<Level>, List<CompoundTag>> entityData = new HashMap<>();
        Map<ResourceKey<Level>, Set<Long>> loadedChunks = new HashMap<>();
        Map<UUID, PlayerSnapshot> playerData = loadPlayerSnapshotsFromDisk(checkpointDir);
        long gameTime = 0L, dayTime = 0L;
        boolean raining = false, thundering = false;
        int clearWeatherTime = 0, rainTime = 0, thunderTime = 0;
        Map<UUID, Integer> domainCooldownRemaining = new HashMap<>();
        Map<UUID, Integer> lionsHeartCooldownRemaining = new HashMap<>();
        Map<UUID, Integer> littleKingCooldownRemaining = new HashMap<>();
        Map<UUID, Integer> materialPhaseCooldownRemaining = new HashMap<>();
        Map<UUID, Integer> objectFreezeCooldownRemaining = new HashMap<>();
        Map<UUID, Integer> allyTrackerCooldownRemaining = new HashMap<>();
        Map<UUID, Integer> baseShiftCooldownRemaining = new HashMap<>();
        Map<UUID, Integer> secondShiftCooldownRemaining = new HashMap<>();
        Map<UUID, Integer> bookOfWisdomCooldownRemaining = new HashMap<>();
        Set<UUID> bookOfWisdomSummoned = new HashSet<>();
        Map<UUID, Integer> mentalOverloadCooldownRemaining = new HashMap<>();
        Map<UUID, Integer> visionOfDangerCooldownRemaining = new HashMap<>();
        Map<UUID, Integer> visionOfLifeCooldownRemaining = new HashMap<>();
        List<FootprintTracker.FootprintEntry> footprints = new ArrayList<>();
        Map<UUID, Integer> slothCooldownRemaining = new HashMap<>();
        Map<UUID, Integer> quickActionCooldownRemaining = new HashMap<>();
        Map<UUID, int[]> lionsHeartActive = new HashMap<>();
        Set<UUID> materialPhaseActive = new HashSet<>();
        Set<UUID> baseShiftActive = new HashSet<>();
        Set<UUID> secondShiftActive = new HashSet<>();
        Set<UUID> visionOfDangerActive = new HashSet<>();
        Set<UUID> visionOfLifeActive = new HashSet<>();

        Path metaFile = checkpointDir.resolve(PLAYER_SNAPSHOTS_FILE_NAME);
        if (Files.exists(metaFile)) {
            try {
                CompoundTag root = NbtIo.read(metaFile);
                if (root != null) {
                    CompoundTag entities = root.getCompound("Entities");
                    for (String dim : entities.getAllKeys()) {
                        ListTag list = entities.getList(dim, Tag.TAG_COMPOUND);
                        List<CompoundTag> ents = new ArrayList<>(list.size());
                        for (int i = 0; i < list.size(); i++) ents.add(list.getCompound(i));
                        entityData.put(dimensionKey(dim), ents);
                    }
                    CompoundTag loaded = root.getCompound("LoadedChunks");
                    for (String dim : loaded.getAllKeys()) {
                        Set<Long> set = new HashSet<>();
                        for (long c : loaded.getLongArray(dim)) set.add(c);
                        loadedChunks.put(dimensionKey(dim), set);
                    }
                    gameTime = root.getLong("GameTime");
                    dayTime = root.getLong("DayTime");
                    raining = root.getBoolean("Raining");
                    thundering = root.getBoolean("Thundering");
                    clearWeatherTime = root.getInt("ClearWeatherTime");
                    rainTime = root.getInt("RainTime");
                    thunderTime = root.getInt("ThunderTime");
                    CompoundTag cooldowns = root.getCompound("Cooldowns");
                    domainCooldownRemaining = cooldownsFromNbt(cooldowns.getCompound("Domain"));
                    lionsHeartCooldownRemaining = cooldownsFromNbt(cooldowns.getCompound("LionsHeart"));
                    littleKingCooldownRemaining = cooldownsFromNbt(cooldowns.getCompound("LittleKing"));
                    materialPhaseCooldownRemaining = cooldownsFromNbt(cooldowns.getCompound("MaterialPhase"));
                    objectFreezeCooldownRemaining = cooldownsFromNbt(cooldowns.getCompound("ObjectFreeze"));
                    allyTrackerCooldownRemaining = cooldownsFromNbt(cooldowns.getCompound("AllyTracker"));
                    baseShiftCooldownRemaining = cooldownsFromNbt(cooldowns.getCompound("BaseShift"));
                    secondShiftCooldownRemaining = cooldownsFromNbt(cooldowns.getCompound("SecondShift"));
                    bookOfWisdomCooldownRemaining = cooldownsFromNbt(cooldowns.getCompound("BookOfWisdom"));
                    bookOfWisdomSummoned = uuidSetFromNbt(root.getList("BookOfWisdomSummoned", Tag.TAG_STRING));
                    mentalOverloadCooldownRemaining = cooldownsFromNbt(cooldowns.getCompound("MentalOverload"));
                    visionOfDangerCooldownRemaining = cooldownsFromNbt(cooldowns.getCompound("VisionOfDanger"));
                    visionOfLifeCooldownRemaining = cooldownsFromNbt(cooldowns.getCompound("VisionOfLife"));
                    slothCooldownRemaining = cooldownsFromNbt(cooldowns.getCompound("Sloth"));
                    quickActionCooldownRemaining = cooldownsFromNbt(cooldowns.getCompound("QuickAction"));
                    footprints = footprintsFromNbt(root.getList("Footprints", Tag.TAG_COMPOUND));
                    CompoundTag activeStates = root.getCompound("ActiveStates");
                    lionsHeartActive = lionsHeartActiveFromNbt(activeStates.getList("LionsHeart", Tag.TAG_COMPOUND));
                    materialPhaseActive = uuidSetFromNbt(activeStates.getList("MaterialPhase", Tag.TAG_STRING));
                    baseShiftActive = uuidSetFromNbt(activeStates.getList("BaseShift", Tag.TAG_STRING));
                    secondShiftActive = uuidSetFromNbt(activeStates.getList("SecondShift", Tag.TAG_STRING));
                    visionOfDangerActive = uuidSetFromNbt(activeStates.getList("VisionOfDanger", Tag.TAG_STRING));
                    visionOfLifeActive = uuidSetFromNbt(activeStates.getList("VisionOfLife", Tag.TAG_STRING));
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to read snapshot metadata from {}", metaFile, e);
            }
        }

        rbd.snapshot = new WorldSnapshot(
                checkpointDir, entityData, loadedChunks, playerData,
                gameTime, dayTime, raining, thundering,
                clearWeatherTime, rainTime, thunderTime, fileTimestamps,
                domainCooldownRemaining, lionsHeartCooldownRemaining, littleKingCooldownRemaining,
                materialPhaseCooldownRemaining, objectFreezeCooldownRemaining, allyTrackerCooldownRemaining,
                baseShiftCooldownRemaining, secondShiftCooldownRemaining,
                bookOfWisdomCooldownRemaining, bookOfWisdomSummoned, mentalOverloadCooldownRemaining,
                visionOfDangerCooldownRemaining, visionOfLifeCooldownRemaining, footprints,
                slothCooldownRemaining, quickActionCooldownRemaining,
                lionsHeartActive, materialPhaseActive, baseShiftActive, secondShiftActive,
                visionOfDangerActive, visionOfLifeActive
        );
    }

    private Map<UUID, Integer> captureRemaining(Map<UUID, Integer> cooldownUntilTick) {
        Map<UUID, Integer> result = new HashMap<>();
        int tick = server.getTickCount();
        cooldownUntilTick.forEach((uuid, until) -> {
            int remaining = until - tick;
            if (remaining > 0) result.put(uuid, remaining);
        });
        return result;
    }

    private void restoreCooldowns(Map<UUID, Integer> cooldownUntilTick, Map<UUID, Integer> remainingByUuid,
                                   ResourceLocation... abilityIds) {
        cooldownUntilTick.clear();
        int tick = server.getTickCount();
        remainingByUuid.forEach((uuid, remaining) -> cooldownUntilTick.put(uuid, tick + remaining));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int remaining = remainingByUuid.getOrDefault(player.getUUID(), 0);
            for (ResourceLocation abilityId : abilityIds) {
                PacketDistributor.sendToPlayer(player, new AbilityCooldownPayload(abilityId, remaining));
            }
        }
    }

    private static CompoundTag cooldownsToNbt(Map<UUID, Integer> remaining) {
        CompoundTag tag = new CompoundTag();
        remaining.forEach((uuid, ticks) -> tag.putInt(uuid.toString(), ticks));
        return tag;
    }

    private static Map<UUID, Integer> cooldownsFromNbt(CompoundTag tag) {
        Map<UUID, Integer> result = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            try {
                result.put(UUID.fromString(key), tag.getInt(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    private static ListTag uuidSetToNbt(Set<UUID> uuids) {
        ListTag list = new ListTag();
        for (UUID id : uuids) list.add(StringTag.valueOf(id.toString()));
        return list;
    }

    private static Set<UUID> uuidSetFromNbt(ListTag list) {
        Set<UUID> result = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            try {
                result.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    private static ListTag lionsHeartActiveToNbt(Map<UUID, int[]> active) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, int[]> e : active.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("UUID", e.getKey());
            tag.putInt("Duration", e.getValue()[0]);
            tag.putInt("Elapsed", e.getValue()[1]);
            list.add(tag);
        }
        return list;
    }

    private static Map<UUID, int[]> lionsHeartActiveFromNbt(ListTag list) {
        Map<UUID, int[]> result = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (!tag.hasUUID("UUID")) continue;
            result.put(tag.getUUID("UUID"), new int[]{tag.getInt("Duration"), tag.getInt("Elapsed")});
        }
        return result;
    }

    private static ResourceKey<Level> dimensionKey(String location) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(location));
    }

    private static ListTag footprintsToNbt(List<FootprintTracker.FootprintEntry> entries) {
        ListTag list = new ListTag();
        for (FootprintTracker.FootprintEntry e : entries) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dim", e.dimension().location().toString());
            tag.putUUID("Owner", e.owner());
            tag.putDouble("X", e.x());
            tag.putDouble("Y", e.y());
            tag.putDouble("Z", e.z());
            tag.putFloat("Yaw", e.yaw());
            tag.putString("Name", e.name());
            tag.putByte("Cat", (byte) e.category());
            tag.putLong("Time", e.timestamp());
            list.add(tag);
        }
        return list;
    }

    private static List<FootprintTracker.FootprintEntry> footprintsFromNbt(ListTag list) {
        List<FootprintTracker.FootprintEntry> result = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            UUID owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : new UUID(0L, 0L);
            result.add(new FootprintTracker.FootprintEntry(
                    dimensionKey(tag.getString("Dim")), owner,
                    tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"),
                    tag.getFloat("Yaw"), tag.getString("Name"), tag.getByte("Cat"), tag.getLong("Time")));
        }
        return result;
    }

    private static final String PLAYER_SNAPSHOTS_FILE_NAME = "hahueuh_players.dat";

    private void saveSnapshotMetadataToDisk(Path checkpointDir, WorldSnapshot snapshot) {
        try {
            CompoundTag root = new CompoundTag();

            CompoundTag players = new CompoundTag();
            for (Map.Entry<UUID, PlayerSnapshot> entry : snapshot.playerData().entrySet()) {
                players.put(entry.getKey().toString(), entry.getValue().toNbt());
            }
            root.put("Players", players);

            CompoundTag entities = new CompoundTag();
            for (Map.Entry<ResourceKey<Level>, List<CompoundTag>> entry : snapshot.entityData().entrySet()) {
                ListTag list = new ListTag();
                list.addAll(entry.getValue());
                entities.put(entry.getKey().location().toString(), list);
            }
            root.put("Entities", entities);

            CompoundTag loaded = new CompoundTag();
            for (Map.Entry<ResourceKey<Level>, Set<Long>> entry : snapshot.loadedChunks().entrySet()) {
                loaded.put(entry.getKey().location().toString(),
                        new LongArrayTag(entry.getValue().stream().mapToLong(Long::longValue).toArray()));
            }
            root.put("LoadedChunks", loaded);

            root.putLong("GameTime", snapshot.gameTime());
            root.putLong("DayTime", snapshot.dayTime());
            root.putBoolean("Raining", snapshot.raining());
            root.putBoolean("Thundering", snapshot.thundering());
            root.putInt("ClearWeatherTime", snapshot.clearWeatherTime());
            root.putInt("RainTime", snapshot.rainTime());
            root.putInt("ThunderTime", snapshot.thunderTime());

            CompoundTag cooldowns = new CompoundTag();
            cooldowns.put("Domain", cooldownsToNbt(snapshot.domainCooldownRemaining()));
            cooldowns.put("LionsHeart", cooldownsToNbt(snapshot.lionsHeartCooldownRemaining()));
            cooldowns.put("LittleKing", cooldownsToNbt(snapshot.littleKingCooldownRemaining()));
            cooldowns.put("MaterialPhase", cooldownsToNbt(snapshot.materialPhaseCooldownRemaining()));
            cooldowns.put("ObjectFreeze", cooldownsToNbt(snapshot.objectFreezeCooldownRemaining()));
            cooldowns.put("AllyTracker", cooldownsToNbt(snapshot.allyTrackerCooldownRemaining()));
            cooldowns.put("BaseShift", cooldownsToNbt(snapshot.baseShiftCooldownRemaining()));
            cooldowns.put("SecondShift", cooldownsToNbt(snapshot.secondShiftCooldownRemaining()));
            cooldowns.put("BookOfWisdom", cooldownsToNbt(snapshot.bookOfWisdomCooldownRemaining()));
            cooldowns.put("MentalOverload", cooldownsToNbt(snapshot.mentalOverloadCooldownRemaining()));
            cooldowns.put("VisionOfDanger", cooldownsToNbt(snapshot.visionOfDangerCooldownRemaining()));
            cooldowns.put("VisionOfLife", cooldownsToNbt(snapshot.visionOfLifeCooldownRemaining()));
            cooldowns.put("Sloth", cooldownsToNbt(snapshot.slothCooldownRemaining()));
            cooldowns.put("QuickAction", cooldownsToNbt(snapshot.quickActionCooldownRemaining()));
            root.put("Cooldowns", cooldowns);
            root.put("BookOfWisdomSummoned", uuidSetToNbt(snapshot.bookOfWisdomSummoned()));
            root.put("Footprints", footprintsToNbt(snapshot.footprints()));

            CompoundTag activeStates = new CompoundTag();
            activeStates.put("LionsHeart", lionsHeartActiveToNbt(snapshot.lionsHeartActive()));
            activeStates.put("MaterialPhase", uuidSetToNbt(snapshot.materialPhaseActive()));
            activeStates.put("BaseShift", uuidSetToNbt(snapshot.baseShiftActive()));
            activeStates.put("SecondShift", uuidSetToNbt(snapshot.secondShiftActive()));
            activeStates.put("VisionOfDanger", uuidSetToNbt(snapshot.visionOfDangerActive()));
            activeStates.put("VisionOfLife", uuidSetToNbt(snapshot.visionOfLifeActive()));
            root.put("ActiveStates", activeStates);

            NbtIo.write(root, checkpointDir.resolve(PLAYER_SNAPSHOTS_FILE_NAME));
        } catch (Exception e) {
            LOGGER.warn("Failed to persist snapshot metadata alongside checkpoint", e);
        }
    }

    private Map<UUID, PlayerSnapshot> loadPlayerSnapshotsFromDisk(Path checkpointDir) {
        Map<UUID, PlayerSnapshot> result = new HashMap<>();
        Path file = checkpointDir.resolve(PLAYER_SNAPSHOTS_FILE_NAME);
        if (!Files.exists(file)) return result;

        try {
            CompoundTag root = NbtIo.read(file);
            if (root == null) return result;
            CompoundTag players = root.getCompound("Players");
            for (String key : players.getAllKeys()) {
                try {
                    result.put(UUID.fromString(key), PlayerSnapshot.fromNbt(players.getCompound(key)));
                } catch (Exception e) {
                    LOGGER.warn("Skipping malformed persisted player snapshot for key '{}'", key, e);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to load persisted player snapshot data from {}", file, e);
        }
        return result;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (server == null) return;

        if (ueuhPlayAtTick >= 0 && server.overworld().getGameTime() >= ueuhPlayAtTick) {
            ueuhPlayAtTick = -1;
            if (RANDOM.nextFloat() < 0.25f) {
                var sound = RANDOM.nextBoolean() ? ModSounds.UEUH : ModSounds.EUHEUH;
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    playPersonalSound(p, sound);
                }
            }
        }

        if (rollbackAtTick >= 0 && !rollbackInProgress && server.overworld().getGameTime() >= rollbackAtTick) {
            rollbackAtTick = -1;
            CheckpointSlot slot = pendingRollbackSlot != null ? pendingRollbackSlot : rbd;
            pendingRollbackSlot = null;
            performRollback(slot);
            if (slot == rbd) {
                if (isDomainActive()) deactivateDomain("rbd rollback");
            } else {
                domainCasterDeadHardcore = false;
            }
            PacketDistributor.sendToAllPlayers(new DeathFadePayload(false));
            return;
        }

        tickDomainEnforcement();

        tickUnseenHands();

        refreshDimensionSettleWindow();

        if (deferredCheckpointReason != null && server.overworld().getGameTime() >= suppressAutoCheckpointsUntilTick) {
            String reason = deferredCheckpointReason;
            deferredCheckpointReason = null;
            attemptAutoCheckpoint(reason, 100);
        }

        if (ConfigReturnByDeath.CHECKPOINT_TIMER_ENABLED.get()) {
            tickCounter++;
            if (tickCounter >= nextCheckpointIntervalTicks) {
                if (!attemptAutoCheckpoint("timer", ConfigReturnByDeath.CHECKPOINT_TIMER_CHANCE.getAsInt())) {
                    tickCounter = 0;
                    rollNextCheckpointInterval();
                }
            }
        }
    }

    private boolean attemptAutoCheckpoint(String reason, int chancePercent) {
        refreshDimensionSettleWindow();

        long gameTime = server.overworld().getGameTime();
        if (gameTime == lastAutoCheckpointGameTick) return false;

        if (gameTime < suppressAutoCheckpointsUntilTick) {
            if (deferredCheckpointReason == null) {
                LOGGER.info("Deferring checkpoint (reason: {}) — a dimension is still settling for {} more ticks",
                        reason, suppressAutoCheckpointsUntilTick - gameTime);
            }
            deferredCheckpointReason = reason + "-deferred";
            return false;
        }

        if (!rollChance(chancePercent)) return false;

        lastAutoCheckpointGameTick = gameTime;
        createSnapshot(rbd, reason);
        return true;
    }

    private void rollNextCheckpointInterval() {
        int base = ConfigReturnByDeath.CHECKPOINT_INTERVAL_SECONDS.getAsInt();
        int randomness = ConfigReturnByDeath.CHECKPOINT_INTERVAL_RANDOMNESS_SECONDS.getAsInt();
        int offsetSeconds = randomness > 0 ? RANDOM.nextInt(randomness * 2 + 1) - randomness : 0;
        int seconds = Math.max(1, base + offsetSeconds);
        nextCheckpointIntervalTicks = seconds * 20;
    }

    private boolean rollChance(int chancePercent) {
        if (chancePercent >= 100) return true;
        if (chancePercent <= 0) return false;
        return RANDOM.nextInt(100) < chancePercent;
    }

    @SubscribeEvent
    public void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (server == null) return;
        if (!ConfigReturnByDeath.CHECKPOINT_ON_ADVANCEMENT_ENABLED.get()) return;
        if (rollbackInProgress || rollbackAtTick >= 0) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!authorityManager.canReturnByDeath(player.getUUID())) return;

        attemptAutoCheckpoint("advancement:" + event.getAdvancement().id(), ConfigReturnByDeath.CHECKPOINT_ON_ADVANCEMENT_CHANCE.getAsInt());
    }

    @SubscribeEvent
    public void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (server == null) return;
        if (!ConfigReturnByDeath.CHECKPOINT_ON_SLEEP_ENABLED.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!authorityManager.canReturnByDeath(player.getUUID())) return;

        attemptAutoCheckpoint("sleep", ConfigReturnByDeath.CHECKPOINT_ON_SLEEP_CHANCE.getAsInt());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        authorityManager.ensureSageCandidateRolled(player.getUUID());
        sendDomainStateTo(player);
        sendAuthoritiesTo(player);
        sendAbilitySlotsTo(player);
        sendActiveUnseenHandsTo(player);
        HahUeuh.LIONS_HEART.restoreOnLogin(player);
        HahUeuh.MATERIAL_PHASE.restoreOnLogin(player);
        HahUeuh.BASE_SHIFT.restoreOnLogin(player);
        HahUeuh.SECOND_SHIFT.restoreOnLogin(player);
        HahUeuh.VISION_OF_DANGER.restoreOnLogin(player);
        HahUeuh.VISION_OF_LIFE.restoreOnLogin(player);
        int cooldownTicksLeft = player.isCreative() ? 0 : domainCooldownRemainingTicks(player.getUUID());
        if (cooldownTicksLeft > 0) {
            PacketDistributor.sendToPlayer(player, new AbilityCooldownPayload(HahUeuhAbilities.DOMAIN_VICTIM_ABILITY, cooldownTicksLeft));
            PacketDistributor.sendToPlayer(player, new AbilityCooldownPayload(HahUeuhAbilities.DOMAIN_AGGRESSOR_ABILITY, cooldownTicksLeft));
        }
        PlayerSnapshot joinSnapshot = null;
        for (CheckpointSlot slot : new CheckpointSlot[]{rbd, domain}) {
            if (slot.snapshot == null || slot.snapshot.playerData().containsKey(player.getUUID())) continue;
            if (joinSnapshot == null) joinSnapshot = PlayerSnapshot.capture(player);
            slot.snapshot.playerData().put(player.getUUID(), joinSnapshot);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearUnseenHand(player.getUUID());
        }
    }

    private void refreshDimensionSettleWindow() {
        int settleSeconds = DIMENSION_SETTLE_SECONDS;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ResourceKey<Level> current = p.level().dimension();
            ResourceKey<Level> last = lastPlayerDimension.put(p.getUUID(), current);
            if (last != null && !last.equals(current) && settleSeconds > 0) {
                long until = server.overworld().getGameTime() + (long) settleSeconds * 20;
                suppressAutoCheckpointsUntilTick = Math.max(suppressAutoCheckpointsUntilTick, until);
            }
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (rollbackInProgress) return;
        if (!rbd.isActive() && !domain.isActive()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getChunk() instanceof LevelChunk)) return;

        for (CheckpointSlot slot : new CheckpointSlot[]{rbd, domain}) {
            if (!slot.isActive()) continue;
            ChunkPos pos = event.getChunk().getPos();
            Set<Long> known = slot.capturedChunks.computeIfAbsent(level.dimension(), d -> ConcurrentHashMap.newKeySet());
            if (!known.add(pos.toLong())) continue;
            try {
                captureNewChunkIntoCheckpoint(slot, level, event.getChunk());
            } catch (Exception e) {
                LOGGER.warn("Failed to capture chunk {} into {} checkpoint", pos, slot.dirName, e);
            }
        }
    }

    @SubscribeEvent
    public void onChunkSave(ChunkDataEvent.Save event) {
        if (internalSaveInProgress) return;
        if (!rbd.isActive() && !domain.isActive()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long chunkKey = event.getChunk().getPos().toLong();
        for (CheckpointSlot slot : new CheckpointSlot[]{rbd, domain}) {
            if (!slot.isActive()) continue;
            slot.modifiedChunks.computeIfAbsent(level.dimension(), d -> ConcurrentHashMap.newKeySet()).add(chunkKey);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (rollbackInProgress) return;
        if (handleAuthorityDeath(event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            handleWitchFactorLossOnDeath(player);
        }
    }

    private void handleWitchFactorLossOnDeath(ServerPlayer player) {
        if (!ConfigMain.LOSE_WITCH_FACTOR_ON_DEATH.get()) return;
        UUID uuid = player.getUUID();
        ServerLevel level = player.serverLevel();
        Vec3 pos = player.position();

        if (authorityManager.hasWitchFactorSloth(uuid)) {
            authorityManager.setWitchFactorSloth(uuid, false);
            spawnWitchFactor(level, pos, WitchFactorAuthority.SLOTH);
            player.displayClientMessage(Component.translatable("hahueuh.message.witch_factor_lost_on_death",
                    Component.translatable("hahueuh.authority.sloth")).withStyle(ChatFormatting.RED), true);
        }
        if (authorityManager.hasWitchFactorGreed(uuid)) {
            authorityManager.setWitchFactorGreed(uuid, false);
            spawnWitchFactor(level, pos, WitchFactorAuthority.GREED);
            player.displayClientMessage(Component.translatable("hahueuh.message.witch_factor_lost_on_death",
                    Component.translatable("hahueuh.authority.greed")).withStyle(ChatFormatting.RED), true);
        }
    }

    private void spawnWitchFactor(ServerLevel level, Vec3 pos, WitchFactorAuthority authority) {
        WitchFactorEntity entity = new WitchFactorEntity(ModEntities.WITCH_FACTOR.get(), level);
        entity.moveTo(pos.x, pos.y, pos.z, 0.0f, 0.0f);
        entity.setAssignedAuthority(authority);
        level.addFreshEntity(entity);
        HahUeuh.MOB_WITCH_FACTOR.registerWandering(entity);
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null || rollbackInProgress) return;
        if (!authorityManager.canReturnByDeath(player.getUUID())) return;

        String msg = event.getRawText().toLowerCase(java.util.Locale.ROOT);
        if (msg.contains("return by death") || msg.matches(".*\\brbd\\b.*")) {
            handleAuthorityDeath(player);
        }
    }

    private boolean handleAuthorityDeath(LivingEntity entity) {
        if (rollbackInProgress) return false;
        UUID uuid = entity.getUUID();

        if (isDomainActive() && isDomainSubject(uuid)) {
            healAndSignal(entity);
            scheduleRollback(domain);
            return true;
        }

        if (!(entity instanceof ServerPlayer player)) return false;

        if (isAggressorDomain() && isDomainOwner(uuid)) {
            if (server.isHardcore()) {
                player.setHealth(player.getMaxHealth());
                player.setGameMode(GameType.SPECTATOR);
                domainCasterDeadHardcore = true;
                player.displayClientMessage(Component.translatable("hahueuh.message.domain_holds")
                        .withStyle(ChatFormatting.AQUA), true);
                return true;
            }
            healAndSignal(player);
            scheduleRollback(domain);
            return true;
        }

        if (authorityManager.canReturnByDeath(uuid) && rbd.isActive()) {
            healAndSignal(player);
            if (ConfigReturnByDeath.WITCH_MIASMA_ENABLED.get() && !player.isCreative() && !player.isSpectator()) {
                MobEffectInstance existing = player.getEffect(ModEffects.WITCH_MIASMA);
                int maxAmplifier = ConfigReturnByDeath.WITCH_MIASMA_MAX_LEVEL.getAsInt() - 1;
                int amplifier = existing != null ? Math.min(existing.getAmplifier() + 1, maxAmplifier) : 0;
                pendingMiasmaBump.put(uuid, amplifier);
            }
            scheduleRollback(rbd);
            return true;
        }
        return false;
    }

    private void healAndSignal(LivingEntity entity) {
        entity.setHealth(entity.getMaxHealth());
        if (entity instanceof ServerPlayer player) {
            playPersonalSound(player, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.WARDEN_HEARTBEAT));
        }
    }

    private final Map<UUID, Integer> pendingMiasmaBump = new HashMap<>();
    private static final int MIASMA_DURATION_TICKS = 5 * 60 * 20;

    private void applyMiasmaLevel(ServerPlayer player, int amplifier) {
        player.forceAddEffect(new MobEffectInstance(ModEffects.WITCH_MIASMA, MIASMA_DURATION_TICKS, amplifier, false, false, true), null);
    }

    @SubscribeEvent
    public void onMobEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || !instance.is(ModEffects.WITCH_MIASMA) || !(event.getEntity() instanceof ServerPlayer player)) return;
        int amplifier = instance.getAmplifier();
        if (amplifier <= 0) return;
        event.setCanceled(true);
        applyMiasmaLevel(player, amplifier - 1);
    }

    private void scheduleRollback(CheckpointSlot slot) {
        if (rollbackAtTick >= 0) return;
        pendingRollbackSlot = slot;
        int fadeInTicks = (int) Math.ceil(DeathFadeState.FADE_SECONDS * 20f) + 2;
        rollbackAtTick = server.overworld().getGameTime() + fadeInTicks;
        PacketDistributor.sendToAllPlayers(new DeathFadePayload(true));
    }

    public PlayerAuthorityManager getAuthorityManager() {
        return authorityManager;
    }

    public boolean isTargetingSuppressed() {
        if (targetingSuppressUntilTick < 0 || server == null) return false;
        return server.overworld().getGameTime() < targetingSuppressUntilTick;
    }

    public AbilitySlotsManager getAbilitySlotsManager() {
        return abilitySlotsManager;
    }

    private void sendAbilitySlotsTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new AbilitySlotsSyncPayload(abilitySlotsManager.get(player.getUUID())));
    }

    public void onUnseenHandUpdate(ServerPlayer owner, boolean active, float distance, int modeId, boolean mobility,
                                    boolean quickSession) {
        if (server == null) return;
        UUID uuid = owner.getUUID();
        UnseenHand existing = unseenHands.get(uuid);
        boolean wasActive = existing != null;

        boolean show = active && authorityManager.canUseSloth(uuid);
        if (show && !wasActive && !owner.isCreative()) {
            int remaining = quickSession ? quickCooldownRemainingTicks(uuid) : slothCooldownRemainingTicks(uuid);
            if (remaining > 0) {
                show = false;
                PacketDistributor.sendToPlayer(owner, new AbilityCooldownPayload(
                        quickSession ? HahUeuhAbilities.QUICK_ACTION_COOLDOWN_KEY : HahUeuhAbilities.SLOTH_COOLDOWN_KEY,
                        remaining));
            }
        }

        boolean mobilityAllowed = mobility && authorityManager.getSlothVariant(uuid) == SlothVariant.UNSEEN_HANDS;
        boolean wasMobility = existing != null && existing.mobility;
        if (show) {
            UnseenHand hand = unseenHands.computeIfAbsent(uuid, k -> new UnseenHand());
            hand.distance = distance;
            HandMode newMode = HandMode.byId(modeId);
            if (hand.mode == HandMode.GRAB && newMode != HandMode.GRAB) java.util.Arrays.fill(hand.grabbed, null);
            hand.mode = newMode;
            hand.mobility = mobilityAllowed;
            hand.quickSession = quickSession;
        } else {
            unseenHands.remove(uuid);
        }

        if (wasActive && !show && !owner.isCreative()) {
            startSlothCooldown(owner, existing.quickSession);
        }
        boolean nowMobility = show && mobilityAllowed;
        if (wasMobility && !nowMobility) owner.setForcedPose(null);
        UnseenHandSyncPayload payload = new UnseenHandSyncPayload(owner.getUUID(), owner.getId(), show, distance,
                show ? HandMode.byId(modeId).ordinal() : 0,
                authorityManager.getSlothVariant(owner.getUUID()).ordinal(),
                show && mobilityAllowed);
        ResourceKey<Level> dim = owner.level().dimension();
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (viewer == owner) continue;
            if (!authorityManager.canUseSloth(viewer.getUUID())) continue;
            if (!viewer.level().dimension().equals(dim)) continue;
            PacketDistributor.sendToPlayer(viewer, payload);
        }
    }

    private void clearUnseenHand(UUID owner) {
        if (unseenHands.remove(owner) != null && server != null) {
            UnseenHandSyncPayload off = new UnseenHandSyncPayload(owner, -1, false, 0f, 0, 0, false);
            for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
                if (!viewer.getUUID().equals(owner) && authorityManager.canUseSloth(viewer.getUUID())) {
                    PacketDistributor.sendToPlayer(viewer, off);
                }
            }
        }
    }

    private void sendActiveUnseenHandsTo(ServerPlayer viewer) {
        if (server == null || !authorityManager.canUseSloth(viewer.getUUID())) return;
        for (Map.Entry<UUID, UnseenHand> entry : unseenHands.entrySet()) {
            if (entry.getKey().equals(viewer.getUUID())) continue;
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (owner != null && owner.level().dimension().equals(viewer.level().dimension())) {
                PacketDistributor.sendToPlayer(viewer, new UnseenHandSyncPayload(entry.getKey(), owner.getId(), true,
                        entry.getValue().distance, entry.getValue().mode.ordinal(),
                        authorityManager.getSlothVariant(entry.getKey()).ordinal(),
                        entry.getValue().mobility));
            }
        }
    }

    private static boolean isRidingOn(Entity rider, Entity target) {
        for (Entity v = rider.getVehicle(); v != null; v = v.getVehicle()) {
            if (v == target) return true;
        }
        return false;
    }

    private void tickUnseenHands() {
        if (unseenHands.isEmpty()) return;
        for (Map.Entry<UUID, UnseenHand> entry : unseenHands.entrySet()) {
            UnseenHand hand = entry.getValue();
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (owner == null) { hand.grabbed = new UUID[0]; continue; }

            HahUeuh.SLOTH_COMPAT.applyDrawbacks(owner);

            switch (authorityManager.getSlothVariant(owner.getUUID())) {
                case SEKHMET -> tickSekhmet(owner, hand);
                case UNSEEN_HANDS -> tickUnseenHandsVariant(owner, hand);
                default -> tickInvisibleProvidence(owner, hand);
            }

            broadcastGrabState(owner, hand);
        }
    }

    private void broadcastGrabState(ServerPlayer owner, UnseenHand hand) {
        if (server == null) return;
        if (java.util.Arrays.equals(hand.grabbed, hand.lastBroadcastGrabbed)) return;
        hand.lastBroadcastGrabbed = hand.grabbed.clone();

        ServerLevel level = owner.serverLevel();
        List<Integer> ids = new ArrayList<>(hand.grabbed.length);
        for (UUID id : hand.grabbed) {
            Entity e = id == null ? null : level.getEntity(id);
            ids.add(e != null ? e.getId() : -1);
        }

        UnseenHandGrabSyncPayload payload = new UnseenHandGrabSyncPayload(owner.getUUID(), ids);
        PacketDistributor.sendToPlayer(owner, payload);
        ResourceKey<Level> dim = owner.level().dimension();
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (viewer == owner) continue;
            if (!authorityManager.canUseSloth(viewer.getUUID())) continue;
            if (!viewer.level().dimension().equals(dim)) continue;
            PacketDistributor.sendToPlayer(viewer, payload);
        }
    }

    private static boolean isSmallGrabbable(Entity e) {
        return e.getBbWidth() <= INVIS_PROVIDENCE_MAX_GRAB_SIZE && e.getBbHeight() <= INVIS_PROVIDENCE_MAX_GRAB_SIZE;
    }

    private void tickInvisibleProvidence(ServerPlayer owner, UnseenHand hand) {
        ensureGrabSlots(hand, 1);
        Vec3 chest = owner.position().add(0, owner.getBbHeight() * 0.72, 0);
        Vec3 tip = chest.add(owner.getViewVector(1.0f).scale(hand.distance));
        ServerLevel level = owner.serverLevel();
        AABB reach = new AABB(tip, tip).inflate(0.6);

        if (hand.mode == HandMode.ATTACK) {
            hand.grabbed[0] = null;
            float damage = Math.max(0f, 4.0f + SlothVariant.attackDamageBonus(owner));
            for (Entity e : level.getEntities(owner, reach, e -> e instanceof LivingEntity && e.isAlive() && e != owner)) {
                e.hurt(owner.damageSources().indirectMagic(owner, owner), damage);
            }
        } else if (hand.mode == HandMode.GRAB) {
            hand.grabbed[0] = dragGrab(owner, level, tip, hand.grabbed[0], reach,
                    SnapshotManager::isSmallGrabbable, Set.of());
        } else {
            hand.grabbed[0] = null;
            nudgeBlockAtHand(level, owner, tip, hand);
            nudgeArmorStandAtHand(level, owner, tip);
            crushImplantedHeartsAtHand(level, reach);
        }
    }

    private void tickUnseenHandsVariant(ServerPlayer owner, UnseenHand hand) {
        ServerLevel level = owner.serverLevel();
        int count = SlothVariant.unseenHandCount(owner.getUUID());
        ensureGrabSlots(hand, count);

        if (hand.mobility) {
            java.util.Arrays.fill(hand.grabbed, null);
            tickUnseenHandsMobility(owner, hand);
            return;
        }

        if (hand.mode == HandMode.ATTACK) {
            java.util.Arrays.fill(hand.grabbed, null);
            double timeSec = level.getGameTime() / 20.0;
            Set<Entity> targets = new HashSet<>();
            for (int i = 0; i < count; i++) {
                Vec3 tip = unseenHandFlailTip(owner, i, hand.distance, timeSec);
                targets.addAll(level.getEntities(owner, new AABB(tip, tip).inflate(0.9),
                        e -> e instanceof LivingEntity && e.isAlive() && e != owner));
            }
            float damage = Math.max(0f, 2.0f + SlothVariant.attackDamageBonus(owner));
            for (Entity e : targets) e.hurt(owner.damageSources().indirectMagic(owner, owner), damage);
        } else if (hand.mode == HandMode.GRAB) {
            Vec3[] tips = new Vec3[count];
            for (int i = 0; i < count; i++) tips[i] = unseenHandTip(owner, i, hand.distance);

            Set<UUID> claimed = new HashSet<>();
            for (int i = 0; i < count; i++) {
                if (hand.grabbed[i] == null) continue;
                Entity e = level.getEntity(hand.grabbed[i]);
                if (e == null || !e.isAlive() || isRidingOn(owner, e)) hand.grabbed[i] = null;
                else claimed.add(hand.grabbed[i]);
            }

            for (int i = 0; i < count; i++) {
                if (hand.grabbed[i] != null) continue;
                List<Entity> cands = level.getEntities(owner, new AABB(tips[i], tips[i]).inflate(0.6),
                        e -> e.isAlive() && e != owner && !e.isSpectator() && !isRidingOn(owner, e)
                                && !claimed.contains(e.getUUID()));
                cands.sort(java.util.Comparator.comparingInt(SnapshotManager::handsNeeded));
                for (Entity cand : cands) {
                    int need = handsNeeded(cand);
                    AABB box = cand.getBoundingBox();
                    List<Integer> reachers = new ArrayList<>();
                    for (int j = 0; j < count; j++) {
                        if (hand.grabbed[j] == null && box.intersects(new AABB(tips[j], tips[j]).inflate(0.6)))
                            reachers.add(j);
                    }
                    if (reachers.size() >= need) {
                        for (int k = 0; k < need; k++) hand.grabbed[reachers.get(k)] = cand.getUUID();
                        claimed.add(cand.getUUID());
                        break;
                    }
                }
            }

            Map<UUID, List<Integer>> groups = new HashMap<>();
            for (int i = 0; i < count; i++)
                if (hand.grabbed[i] != null) groups.computeIfAbsent(hand.grabbed[i], k -> new ArrayList<>()).add(i);
            for (Map.Entry<UUID, List<Integer>> g : groups.entrySet()) {
                Entity e = level.getEntity(g.getKey());
                if (e == null) continue;
                Vec3 centre = Vec3.ZERO;
                for (int i : g.getValue()) centre = centre.add(tips[i]);
                centre = centre.scale(1.0 / g.getValue().size());
                Vec3 target = new Vec3(centre.x, centre.y - e.getBbHeight() / 2.0, centre.z);
                e.setDeltaMovement(target.subtract(e.position()));
                e.hasImpulse = true;
                e.hurtMarked = true;
                if (e instanceof ServerPlayer sp) sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
            }
        } else {
            java.util.Arrays.fill(hand.grabbed, null);
        }
    }

    private static int handsNeeded(Entity e) {
        float maxDim = Math.max(e.getBbWidth(), e.getBbHeight());
        if (maxDim <= INVIS_PROVIDENCE_MAX_GRAB_SIZE) return 1;
        return 1 + (int) Math.ceil((maxDim - INVIS_PROVIDENCE_MAX_GRAB_SIZE) / 1.3f);
    }

    private Vec3 unseenHandTip(ServerPlayer owner, int i, double baseDist) {
        UUID id = owner.getUUID();
        float yaw = owner.getYRot();
        float pitch = owner.getXRot();
        double yawRad = Math.toRadians(yaw);
        Vec3 rightVec = new Vec3(-Math.cos(yawRad), 0, -Math.sin(yawRad));
        Vec3 fwdFlat = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 anchor = owner.position()
                .add(0, owner.getBbHeight() * SlothVariant.UNSEEN_ANCHOR_HEIGHT, 0)
                .subtract(fwdFlat.scale(SlothVariant.UNSEEN_HAND_BACK));
        double reach = Math.max(0.1, baseDist + SlothVariant.unseenHandDistBias(id, i));
        return anchor.add(SlothVariant.direction(yaw, pitch).scale(reach))
                .add(rightVec.scale(SlothVariant.unseenHandSideOffset(id, i)))
                .add(0, SlothVariant.unseenHandRise(id, i), 0);
    }

    private Vec3 unseenHandFlailTip(ServerPlayer owner, int i, double baseDist, double timeSec) {
        UUID id = owner.getUUID();
        float yaw = owner.getYRot();
        float pitch = owner.getXRot();
        double yawRad = Math.toRadians(yaw);
        Vec3 fwdFlat = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 anchor = owner.position()
                .add(0, owner.getBbHeight() * SlothVariant.UNSEEN_ANCHOR_HEIGHT, 0)
                .subtract(fwdFlat.scale(SlothVariant.UNSEEN_HAND_BACK));
        float flailYaw = yaw + SlothVariant.unseenHandFlailYaw(id, i, timeSec);
        float flailPitch = pitch + SlothVariant.unseenHandFlailPitch(id, i, timeSec);
        double reach = Math.max(0.3, baseDist * SlothVariant.unseenHandFlailReachMul(id, i, timeSec));
        Vec3 rawTip = anchor.add(SlothVariant.direction(flailYaw, flailPitch).scale(reach));

        double groundY = SlothVariant.findGroundY(owner.serverLevel(), rawTip.x, owner.getY(), rawTip.z,
                SlothVariant.UNSEEN_MOBILITY_GROUND_SCAN);
        return Double.isNaN(groundY) ? rawTip : new Vec3(rawTip.x, Math.max(rawTip.y, groundY), rawTip.z);
    }

    private void tickUnseenHandsMobility(ServerPlayer owner, UnseenHand hand) {
        owner.setForcedPose(Pose.STANDING);
        SlothVariant.freezeWalkAnimation(owner);

        ServerLevel level = owner.serverLevel();
        double groundY = SlothVariant.findGroundY(level, owner.getX(), owner.getY(), owner.getZ(),
                SlothVariant.UNSEEN_MOBILITY_GROUND_SCAN);
        if (Double.isNaN(groundY)) {
            return;
        }

        double targetY = groundY + hand.distance;
        double dy = targetY - owner.getY();
        double vy = Mth.clamp(dy * 0.4, -0.6, 0.6);

        Vec3 lookFlat = new Vec3(owner.getLookAngle().x, 0, owner.getLookAngle().z);
        lookFlat = lookFlat.lengthSqr() > 1.0e-6 ? lookFlat.normalize() : Vec3.ZERO;
        double speed = ConfigSloth.UNSEEN_HANDS_MOBILITY_SPEED.getAsInt() / 20.0;

        owner.setDeltaMovement(lookFlat.x * speed, vy, lookFlat.z * speed);
        owner.hasImpulse = true;
        owner.hurtMarked = true;
        owner.fallDistance = 0;
        owner.connection.send(new ClientboundSetEntityMotionPacket(owner));
    }

    private void tickSekhmet(ServerPlayer owner, UnseenHand hand) {
        ensureGrabSlots(hand, 2);
        ServerLevel level = owner.serverLevel();
        float size = SlothVariant.sekhmetSize(owner.getUUID());
        float damage = Math.max(0f, 2f * size + SlothVariant.attackDamageBonus(owner));
        double maxReach = ConfigSloth.SLOTH_MAX_DISTANCE.getAsInt() * SlothVariant.SEKHMET.reachMultiplier;
        double dist = Math.min(hand.distance, maxReach);
        double hitRadius = 0.6 + 0.35 * size;

        Vec3 look = owner.getViewVector(1.0f);
        Vec3 right = new Vec3(-look.z, 0, look.x);
        right = right.lengthSqr() > 1.0e-6 ? right.normalize() : new Vec3(1, 0, 0);
        double tipOff = SlothVariant.sekhmetShoulderOffset(size) + SlothVariant.sekhmetHandSplay(size);
        Vec3 backFlat = new Vec3(look.x, 0, look.z);
        backFlat = backFlat.lengthSqr() > 1.0e-6 ? backFlat.normalize() : new Vec3(0, 0, 1);
        Vec3 shoulders = owner.position()
                .add(0, owner.getBbHeight() * SlothVariant.SEKHMET_SHOULDER_HEIGHT, 0)
                .subtract(backFlat.scale(SlothVariant.SEKHMET_BACK_OFFSET));
        Vec3 leftTip = shoulders.subtract(right.scale(tipOff)).add(look.scale(dist));
        Vec3 rightTip = shoulders.add(right.scale(tipOff)).add(look.scale(dist));

        if (hand.mode == HandMode.ATTACK) {
            java.util.Arrays.fill(hand.grabbed, null);
            sekhmetBreakBlocks(level, leftTip, size);
            sekhmetBreakBlocks(level, rightTip, size);
            sekhmetStrike(owner, level, leftTip, hitRadius, damage, size);
            sekhmetStrike(owner, level, rightTip, hitRadius, damage, size);
        } else if (hand.mode == HandMode.GRAB) {
            hand.grabbed[0] = dragGrab(owner, level, leftTip, hand.grabbed[0],
                    new AABB(leftTip, leftTip).inflate(hitRadius), e -> true, asSet(hand.grabbed[1]));
            hand.grabbed[1] = dragGrab(owner, level, rightTip, hand.grabbed[1],
                    new AABB(rightTip, rightTip).inflate(hitRadius), e -> true, asSet(hand.grabbed[0]));
        } else {
            java.util.Arrays.fill(hand.grabbed, null);
        }
    }

    private static Set<UUID> asSet(UUID id) {
        return id == null ? Set.of() : Set.of(id);
    }

    private void sekhmetStrike(ServerPlayer owner, ServerLevel level, Vec3 tip, double radius, float damage, float size) {
        AABB reach = new AABB(tip, tip).inflate(radius);
        boolean hitAny = false;
        double kbHoriz = 0.4 + 0.125 * size;
        double kbUp = 0.225 + 0.05 * size;
        for (Entity e : level.getEntities(owner, reach, e -> e instanceof LivingEntity && e.isAlive() && e != owner)) {
            e.hurt(owner.damageSources().indirectMagic(owner, owner), damage);
            Vec3 away = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(tip);
            away = new Vec3(away.x, 0, away.z);
            away = away.lengthSqr() > 1.0e-4 ? away.normalize() : new Vec3(owner.getViewVector(1.0f).x, 0, owner.getViewVector(1.0f).z).normalize();
            e.setDeltaMovement(e.getDeltaMovement().add(away.x * kbHoriz, kbUp, away.z * kbHoriz));
            e.hasImpulse = true;
            e.hurtMarked = true;
            if (e instanceof ServerPlayer sp) sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
            hitAny = true;
        }
        if (hitAny) {
            level.sendParticles(ParticleTypes.EXPLOSION, tip.x, tip.y, tip.z,
                    (int) (1 + size), 0.2, 0.2, 0.2, 0.0);
            level.playSound(null, tip.x, tip.y, tip.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE,
                    0.9f, 1.2f + level.random.nextFloat() * 0.2f);
        }
    }

    public void mobSekhmetBreakBlocks(ServerLevel level, Vec3 center, float size) {
        sekhmetBreakBlocks(level, center, size);
    }

    private void sekhmetBreakBlocks(ServerLevel level, Vec3 tip, float size) {
        boolean full = level.getGameRules().getBoolean(ModGameRules.REZERO_BLOCK_DESTRUCTION);
        List<TagKey<Block>> allowedTags = full ? breakableTags() : List.of();
        double r = 0.25 + 0.25 * size;
        double r2 = r * r;
        int ri = (int) Math.ceil(r);
        BlockPos center = BlockPos.containing(tip);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dy = -ri; dy <= ri; dy++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r2) continue;
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.getBlock() instanceof LiquidBlock) continue;
                    if (state.getDestroySpeed(level, pos) < 0) continue;
                    boolean breakable = full ? matchesAnyTag(state, allowedTags) : isInconsequentialFoliage(state);
                    if (breakable) {
                        level.destroyBlock(pos.immutable(), ModGameRules.rollDrops(level));
                    }
                }
            }
        }
    }

    private static List<TagKey<Block>> breakableTags() {
        List<TagKey<Block>> tags = new ArrayList<>();
        for (String id : ConfigSloth.SEKHMET_BREAKABLE_TAGS.get()) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null) tags.add(TagKey.create(Registries.BLOCK, rl));
        }
        return tags;
    }

    private static boolean isInconsequentialFoliage(BlockState state) {
        Block block = state.getBlock();
        return block instanceof TallGrassBlock
                || block instanceof FlowerBlock
                || block instanceof DoublePlantBlock;
    }

    private static boolean matchesAnyTag(BlockState state, List<TagKey<Block>> tags) {
        for (TagKey<Block> tag : tags) {
            if (state.is(tag)) return true;
        }
        return false;
    }

    private UUID dragGrab(ServerPlayer owner, ServerLevel level, Vec3 tip, UUID currentGrab, AABB reach,
                          java.util.function.Predicate<Entity> extra, Set<UUID> exclude) {
        Entity grabbed = currentGrab == null ? null : level.getEntity(currentGrab);
        if (grabbed == null || !grabbed.isAlive() || isRidingOn(owner, grabbed)) {
            grabbed = null;
            for (Entity e : level.getEntities(owner, reach,
                    e -> e.isAlive() && e != owner && !e.isSpectator() && !isRidingOn(owner, e)
                            && !exclude.contains(e.getUUID()) && extra.test(e))) {
                grabbed = e;
                break;
            }
        }
        if (grabbed == null) return null;
        Vec3 target = new Vec3(tip.x, tip.y - grabbed.getBbHeight() / 2.0, tip.z);
        grabbed.setDeltaMovement(target.subtract(grabbed.position()));
        grabbed.hasImpulse = true;
        grabbed.hurtMarked = true;
        if (grabbed instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
        }
        return grabbed.getUUID();
    }

    private void nudgeBlockAtHand(ServerLevel level, ServerPlayer owner, Vec3 tip, UnseenHand hand) {
        BlockPos tipBlock = BlockPos.containing(tip);
        if (tipBlock.equals(hand.lastInteractBlock)) return;
        hand.lastInteractBlock = tipBlock;

        BlockState state = level.getBlockState(tipBlock);
        if (state.isAir() || state.getBlock() instanceof SignBlock) return;

        BlockHitResult hit = new BlockHitResult(tip, owner.getDirection(), tipBlock, false);
        boolean hadMenuOpen = owner.hasContainerOpen();
        ItemInteractionResult itemResult = state.useItemOn(ItemStack.EMPTY, level, owner, InteractionHand.MAIN_HAND, hit);
        if (itemResult == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
            state.useWithoutItem(level, owner, hit);
        }
        if (!hadMenuOpen && owner.hasContainerOpen()) {
            owner.closeContainer();
        }
    }

    private void nudgeArmorStandAtHand(ServerLevel level, ServerPlayer owner, Vec3 tip) {
        List<ArmorStand> stands = level.getEntitiesOfClass(ArmorStand.class, new AABB(tip, tip).inflate(0.6));
        if (stands.isEmpty()) return;
        ArmorStand stand = stands.get(0);
        if (stand.isMarker()) return;

        Vec3 relative = tip.subtract(stand.position());
        ItemStack realHeld = owner.getItemInHand(InteractionHand.MAIN_HAND);
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        stand.interactAt(owner, relative, InteractionHand.MAIN_HAND);
        ItemStack takenFromStand = owner.getItemInHand(InteractionHand.MAIN_HAND);
        owner.setItemInHand(InteractionHand.MAIN_HAND, realHeld);
        if (!takenFromStand.isEmpty() && !owner.getInventory().add(takenFromStand)) {
            owner.drop(takenFromStand, false);
        }
    }

    private void crushImplantedHeartsAtHand(ServerLevel level, AABB reach) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, reach)) {
            if (HahUeuh.LITTLE_KING.crushImplant(e)) {
                level.playSound(null, e.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 0.4f, 0.6f);
            }
        }
    }

    public void sendAuthoritiesTo(ServerPlayer player) {
        if (server == null) return;
        PacketDistributor.sendToPlayer(player, new PlayerAuthoritiesPayload(
                authorityManager.canReturnByDeath(player.getUUID()),
                authorityManager.canUseDomain(player.getUUID()),
                authorityManager.canUseSloth(player.getUUID()),
                authorityManager.getSlothVariant(player.getUUID()).ordinal(),
                authorityManager.canUseGreed(player.getUUID()),
                authorityManager.getGreedVariant(player.getUUID()).ordinal()));
    }


    private boolean isDomainActive() { return domainOwnerUuid != null; }

    private boolean isDomainOwner(UUID uuid) {
        return domainOwnerUuid != null && domainOwnerUuid.equals(uuid);
    }

    private boolean isDomainSubject(UUID uuid) {
        return domainSubjectUuid != null && domainSubjectUuid.equals(uuid);
    }

    private boolean isAggressorDomain() {
        return isDomainActive() && domainSubjectUuid != null && !domainSubjectUuid.equals(domainOwnerUuid);
    }

    private void deactivateDomainState() {
        domainOwnerUuid = null;
        domainSubjectUuid = null;
        domainMatrix = null;
        domainDimension = null;
        domainCasterDeadHardcore = false;
    }

    private double domainSphereRadius() {
        return ConfigDomain.DOMAIN_RADIUS.getAsInt() / 2.0;
    }

    private int domainCooldownRemainingTicks(UUID uuid) {
        Integer until = domainCooldownUntilTick.get(uuid);
        if (until == null || server == null) return 0;
        return Math.max(0, until - server.getTickCount());
    }

    private int slothCooldownRemainingTicks(UUID uuid) {
        Integer until = slothCooldownUntilTick.get(uuid);
        if (until == null || server == null) return 0;
        return Math.max(0, until - server.getTickCount());
    }

    private int quickCooldownRemainingTicks(UUID uuid) {
        Integer until = quickCooldownUntilTick.get(uuid);
        if (until == null || server == null) return 0;
        return Math.max(0, until - server.getTickCount());
    }

    private void startSlothCooldown(ServerPlayer owner, boolean quick) {
        if (server == null) return;
        int cooldownSeconds = quick ? ConfigSloth.QUICK_ACTION_COOLDOWN_SECONDS.getAsInt()
                                    : ConfigSloth.SLOTH_COOLDOWN_SECONDS.getAsInt();
        if (cooldownSeconds <= 0) return;
        int ticks = Math.round(cooldownSeconds * 20 * HahUeuh.SLOTH_COMPAT.cooldownMultiplier(owner.getUUID()));
        ResourceLocation key;
        if (quick) {
            quickCooldownUntilTick.put(owner.getUUID(), server.getTickCount() + ticks);
            key = HahUeuhAbilities.QUICK_ACTION_COOLDOWN_KEY;
        } else {
            slothCooldownUntilTick.put(owner.getUUID(), server.getTickCount() + ticks);
            key = HahUeuhAbilities.SLOTH_COOLDOWN_KEY;
        }
        PacketDistributor.sendToPlayer(owner, new AbilityCooldownPayload(key, ticks));
    }

    private Set<String> checkpointProtectedNames() {
        Set<String> names = new HashSet<>(MOD_METADATA_FILES);
        names.add("session.lock");
        names.add(rbd.dirName);
        names.add(domain.dirName);
        return names;
    }

    public void toggleDomain(ServerPlayer player, boolean aggressor) {
        if (server == null) return;

        if (isDomainOwner(player.getUUID())) {
            deactivateDomain("owner toggled off");
            player.displayClientMessage(Component.translatable("hahueuh.message.domain_closed").withStyle(ChatFormatting.AQUA), true);
            return;
        }

        if (!authorityManager.canUseDomain(player.getUUID())) {
            player.displayClientMessage(Component.translatable("hahueuh.message.no_domain_authority")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        int remainingCooldown = player.isCreative() ? 0 : domainCooldownRemainingTicks(player.getUUID());
        if (remainingCooldown > 0) {
            int seconds = (int) Math.ceil(remainingCooldown / 20.0);
            player.displayClientMessage(Component.translatable("hahueuh.message.domain_cooldown", seconds)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (isDomainActive()) {
            player.displayClientMessage(Component.translatable("hahueuh.message.domain_interference")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        LivingEntity target = null;
        if (aggressor) {
            target = raycastTargetEntity(player, domainSphereRadius());
            if (target == null) target = nearestEntityWithin(player, 5.0);
            if (target == null) {
                player.displayClientMessage(Component.translatable("hahueuh.message.domain_no_target")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
        }
        LivingEntity subject = target != null ? target : player;

        domainOwnerUuid = player.getUUID();
        domainSubjectUuid = subject.getUUID();
        domainMatrix = subject.position();
        domainDimension = player.level().dimension();
        createSnapshot(domain, "domain:" + player.getGameProfile().getName());
        PacketDistributor.sendToPlayer(player, activeDomainPayload());

        if (target != null) {
            player.displayClientMessage(Component.translatable("hahueuh.message.domain_cast_on_target",
                    target.getName()).withStyle(ChatFormatting.AQUA), true);
            if (target instanceof ServerPlayer targetPlayer) {
                targetPlayer.displayClientMessage(Component.translatable("hahueuh.message.domain_bound_target")
                        .withStyle(ChatFormatting.AQUA), true);
            }
        } else {
            player.displayClientMessage(Component.translatable("hahueuh.message.domain_deployed")
                    .withStyle(ChatFormatting.AQUA), true);
        }
    }

    private LivingEntity raycastTargetEntity(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(range));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player, eye, end, searchBox,
                e -> e instanceof LivingEntity && e.isAlive() && e != player && !e.isSpectator(),
                range * range);
        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private LivingEntity nearestEntityWithin(ServerPlayer player, double radius) {
        AABB box = player.getBoundingBox().inflate(radius);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != player && !e.isSpectator());
        LivingEntity closest = null;
        double closestDistSq = radius * radius;
        for (LivingEntity e : candidates) {
            double distSq = e.distanceToSqr(player);
            if (distSq <= closestDistSq) {
                closest = e;
                closestDistSq = distSq;
            }
        }
        return closest;
    }

    private void deactivateDomain(String reason) {
        if (!isDomainActive()) return;
        UUID formerOwner = domainOwnerUuid;
        LOGGER.info("Closing domain (owner: {}, reason: {})", formerOwner, reason);
        deactivateDomainState();
        domain.clear();

        int cooldownSeconds = ConfigDomain.DOMAIN_COOLDOWN_SECONDS.getAsInt();
        if (cooldownSeconds > 0 && server != null) {
            ServerPlayer cooldownOwner = server.getPlayerList().getPlayer(formerOwner);
            if (cooldownOwner == null || !cooldownOwner.isCreative()) {
                int untilTick = server.getTickCount() + cooldownSeconds * 20;
                domainCooldownUntilTick.put(formerOwner, untilTick);
                if (cooldownOwner != null) {
                    PacketDistributor.sendToPlayer(cooldownOwner, new AbilityCooldownPayload(HahUeuhAbilities.DOMAIN_VICTIM_ABILITY, cooldownSeconds * 20));
                    PacketDistributor.sendToPlayer(cooldownOwner, new AbilityCooldownPayload(HahUeuhAbilities.DOMAIN_AGGRESSOR_ABILITY, cooldownSeconds * 20));
                }
            }
        }
        if (server != null) {
            Path domainDir = server.getWorldPath(LevelResource.ROOT).resolve(domain.dirName);
            if (Files.exists(domainDir)) {
                try { deleteDirectory(domainDir); } catch (Exception e) {
                    LOGGER.warn("Failed to delete domain checkpoint dir on close", e);
                }
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(formerOwner);
            if (owner != null) PacketDistributor.sendToPlayer(owner, DomainStatePayload.INACTIVE);
        }
    }

    private void tickDomainEnforcement() {
        if (!isDomainActive()) return;
        ServerPlayer owner = server.getPlayerList().getPlayer(domainOwnerUuid);
        if (owner == null) { deactivateDomain("owner offline"); return; }
        if (domainCasterDeadHardcore) return;

        if (!owner.level().dimension().equals(domainDimension)) { deactivateDomain("owner changed dimension"); return; }
        double r2 = domainSphereRadius() * domainSphereRadius();
        if (owner.position().distanceToSqr(domainMatrix) > r2) { deactivateDomain("owner left radius"); return; }

        if (isAggressorDomain() && rollbackAtTick < 0) {
            LivingEntity subject = findDomainSubjectEntity();
            if (subject != null && (!subject.level().dimension().equals(domainDimension)
                    || subject.position().distanceToSqr(domainMatrix) > r2)) {
                healAndSignal(subject);
                scheduleRollback(domain);
            }
        }
    }

    private LivingEntity findDomainSubjectEntity() {
        if (domainSubjectUuid == null || server == null) return null;
        ServerPlayer player = server.getPlayerList().getPlayer(domainSubjectUuid);
        if (player != null) return player;
        ServerLevel level = domainDimension != null ? server.getLevel(domainDimension) : null;
        if (level == null) return null;
        Entity e = level.getEntity(domainSubjectUuid);
        return e instanceof LivingEntity living ? living : null;
    }

    private DomainStatePayload activeDomainPayload() {
        return new DomainStatePayload(true, domainMatrix.x, domainMatrix.y, domainMatrix.z,
                domainSphereRadius(), domainDimension.location());
    }

    public void sendDomainStateTo(ServerPlayer player) {
        DomainStatePayload payload = (isDomainActive() && isDomainOwner(player.getUUID()))
                ? activeDomainPayload()
                : DomainStatePayload.INACTIVE;
        PacketDistributor.sendToPlayer(player, payload);
    }

    private void playPersonalSound(ServerPlayer player, Holder<SoundEvent> sound) {
        player.connection.send(new ClientboundSoundPacket(
                sound, SoundSource.MASTER,
                player.getX(), player.getY(), player.getZ(),
                1.0f, 1.0f, RANDOM.nextLong()
        ));
    }


    public void createSnapshot(String reason) {
        createSnapshot(rbd, reason);
    }

    private void createSnapshot(CheckpointSlot slot, String reason) {
        LOGGER.info("Creating world checkpoint (slot: {}, reason: {})...", slot.dirName, reason);
        long startTime = System.currentTimeMillis();

        try {
            internalSaveInProgress = true;
            try {
                server.saveEverything(false, true, true);
                for (ServerLevel level : server.getAllLevels()) {
                    forceFlushSavedData(level);
                }
            } finally {
                internalSaveInProgress = false;
            }

            Path worldDir = server.getWorldPath(LevelResource.ROOT);
            Path checkpointDir = worldDir.resolve(slot.dirName);

            if (Files.exists(checkpointDir)) {
                deleteDirectory(checkpointDir);
            }

            Set<String> excludes = checkpointProtectedNames();
            Map<Path, Long> fileTimestamps = new HashMap<>();
            copyDirectoryWithTimestamps(worldDir, checkpointDir, excludes, fileTimestamps);

            Map<ResourceKey<Level>, List<CompoundTag>> entityData = new HashMap<>();
            Map<ResourceKey<Level>, Set<Long>> loadedChunks = new HashMap<>();
            for (ServerLevel level : server.getAllLevels()) {
                List<CompoundTag> entities = new ArrayList<>();
                for (Entity entity : level.getAllEntities()) {
                    if (entity instanceof Player) continue;
                    CompoundTag nbt = new CompoundTag();
                    if (entity.save(nbt)) {
                        entities.add(nbt);
                    }
                }
                entityData.put(level.dimension(), entities);

                Set<Long> loaded = new HashSet<>();
                for (ChunkHolder holder : getLoadedChunkHolders(level)) {
                    LevelChunk c = getLoadedChunk(holder);
                    if (c != null) loaded.add(c.getPos().toLong());
                }
                loadedChunks.put(level.dimension(), loaded);
            }

            Map<UUID, PlayerSnapshot> playerData = new HashMap<>();
            if (slot.snapshot != null) {
                playerData.putAll(slot.snapshot.playerData());
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.containerMenu != player.inventoryMenu) {
                    try {
                        player.closeContainer();
                    } catch (Exception e) {
                        LOGGER.warn("Failed to close container for {} during checkpoint capture", player.getGameProfile().getName(), e);
                    }
                }
                playerData.put(player.getUUID(), PlayerSnapshot.capture(player));
            }

            ServerLevel overworld = server.overworld();
            ServerLevelData levelData = (ServerLevelData) overworld.getLevelData();
            slot.snapshot = new WorldSnapshot(
                    checkpointDir,
                    entityData,
                    loadedChunks,
                    playerData,
                    overworld.getGameTime(),
                    overworld.getDayTime(),
                    levelData.isRaining(),
                    levelData.isThundering(),
                    levelData.getClearWeatherTime(),
                    levelData.getRainTime(),
                    levelData.getThunderTime(),
                    fileTimestamps,
                    captureRemaining(domainCooldownUntilTick),
                    HahUeuh.LIONS_HEART.captureCooldownRemaining(),
                    HahUeuh.LITTLE_KING.captureCooldownRemaining(),
                    HahUeuh.MATERIAL_PHASE.captureCooldownRemaining(),
                    HahUeuh.OBJECT_FREEZE.captureCooldownRemaining(),
                    HahUeuh.ALLY_TRACKER.captureCooldownRemaining(),
                    HahUeuh.BASE_SHIFT.captureCooldownRemaining(),
                    HahUeuh.SECOND_SHIFT.captureCooldownRemaining(),
                    HahUeuh.BOOK_OF_WISDOM.captureCooldownRemaining(),
                    HahUeuh.BOOK_OF_WISDOM.captureSummonedState(),
                    HahUeuh.MENTAL_OVERLOAD.captureCooldownRemaining(),
                    HahUeuh.VISION_OF_DANGER.captureCooldownRemaining(),
                    HahUeuh.VISION_OF_LIFE.captureCooldownRemaining(),
                    HahUeuh.FOOTPRINT_TRACKER.captureFootprints(),
                    captureRemaining(slothCooldownUntilTick),
                    captureRemaining(quickCooldownUntilTick),
                    HahUeuh.LIONS_HEART.captureActive(),
                    HahUeuh.MATERIAL_PHASE.captureActive(),
                    HahUeuh.BASE_SHIFT.captureActive(),
                    HahUeuh.SECOND_SHIFT.captureActive(),
                    HahUeuh.VISION_OF_DANGER.captureActive(),
                    HahUeuh.VISION_OF_LIFE.captureActive()
            );
            saveSnapshotMetadataToDisk(checkpointDir, slot.snapshot);

            slot.resetTracking();

            if (slot == rbd) {
                tickCounter = 0;
                rollNextCheckpointInterval();
            }

            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.info("Checkpoint created in {}ms ({} files tracked) (slot: {}, reason: {})", elapsed, fileTimestamps.size(), slot.dirName, reason);

            if (slot == rbd && ConfigReturnByDeath.SHOW_CHECKPOINT_NOTIFICATION.get()) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    player.sendSystemMessage(
                            Component.translatable("hahueuh.message.checkpoint_saved")
                                    .withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC)
                    );
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to create checkpoint! (reason: {})", reason, e);
        }
    }


    private void performRollback(CheckpointSlot slot) {
        WorldSnapshot snapshot = slot.snapshot;
        if (snapshot == null) return;
        rollbackInProgress = true;

        LOGGER.info("Rolling back world to checkpoint (slot: {})...", slot.dirName);
        long startTime = System.currentTimeMillis();

        try {
            long stepStart = startTime;

            Map<ResourceKey<Level>, Set<Long>> chunksToRestore = new HashMap<>();
            for (ServerLevel level : server.getAllLevels()) {
                Set<Long> changed = new HashSet<>(slot.modifiedChunks.getOrDefault(level.dimension(), Set.of()));
                for (ChunkHolder holder : getLoadedChunkHolders(level)) {
                    ChunkAccess chunk = holder.getLatestChunk();
                    if (chunk != null && chunk.isUnsaved()) {
                        changed.add(chunk.getPos().toLong());
                    }
                }
                chunksToRestore.put(level.dimension(), changed);
            }
            int totalChanged = chunksToRestore.values().stream().mapToInt(Set::size).sum();
            stepStart = logStepTime("scan changed chunks (" + totalChanged + ")", stepStart);

            for (ServerLevel level : server.getAllLevels()) {
                clearUnsavedFlags(level);
            }
            stepStart = logStepTime("clearUnsavedFlags (all levels)", stepStart);

            internalSaveInProgress = true;
            try {
                server.saveEverything(false, true, true);
            } finally {
                internalSaveInProgress = false;
            }
            stepStart = logStepTime("saveEverything (drain)", stepStart);
            closeAllRegionStorages();
            stepStart = logStepTime("closeAllRegionStorages", stepStart);

            Path worldDir = server.getWorldPath(LevelResource.ROOT);
            Path checkpointDir = snapshot.checkpointDir();
            Set<String> protectedNames = checkpointProtectedNames();
            int restoredFiles = restoreChangedFiles(worldDir, checkpointDir, protectedNames, snapshot.fileTimestamps());
            stepStart = logStepTime("restoreChangedFiles (" + restoredFiles + " files)", stepStart);

            for (ServerLevel level : server.getAllLevels()) {
                resetSavedDataCache(level);
            }
            stepStart = logStepTime("resetSavedDataCache (all levels)", stepStart);

            for (ServerLevel level : server.getAllLevels()) {
                restoreChangedChunks(level, chunksToRestore.getOrDefault(level.dimension(), Set.of()), checkpointDir);
            }
            stepStart = logStepTime("restoreChangedChunks (all levels)", stepStart);

            for (ServerLevel level : server.getAllLevels()) {
                evictPoiForChangedChunks(level, chunksToRestore.getOrDefault(level.dimension(), Set.of()));
            }
            stepStart = logStepTime("evict POI (all levels)", stepStart);

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.connection.send(new ClientboundBlockChangedAckPacket(Integer.MAX_VALUE));
            }

            for (ServerLevel level : server.getAllLevels()) {
                restoreEntitiesForLevel(level, snapshot);
            }
            stepStart = logStepTime("restoreEntitiesForLevel (all levels)", stepStart);

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.containerMenu != player.inventoryMenu) {
                    try {
                        player.closeContainer();
                    } catch (Exception e) {
                        LOGGER.warn("Failed to close container for {} before rollback restore", player.getGameProfile().getName(), e);
                    }
                }
                PlayerSnapshot ps = snapshot.playerData().get(player.getUUID());
                if (ps != null) {
                    try {
                        ps.restore(player, server);
                    } catch (Exception e) {
                        LOGGER.error("Failed to restore player state for {} during rollback", player.getGameProfile().getName(), e);
                    }
                } else {
                    LOGGER.warn("No snapshot data found for {} — player state was NOT restored on rollback", player.getGameProfile().getName());
                }
            }
            stepStart = logStepTime("restore online players", stepStart);

            if (!pendingMiasmaBump.isEmpty()) {
                for (Map.Entry<UUID, Integer> entry : pendingMiasmaBump.entrySet()) {
                    ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                    if (player != null && !player.isCreative() && !player.isSpectator()) {
                        applyMiasmaLevel(player, entry.getValue());
                    }
                }
                pendingMiasmaBump.clear();
            }

            authorityManager.load(server);
            abilitySlotsManager.load(server);
            HahUeuh.LIONS_HEART.reloadPersisted();
            HahUeuh.MATERIAL_PHASE.reloadPersisted();
            HahUeuh.BASE_SHIFT.reloadPersisted();
            HahUeuh.SECOND_SHIFT.reloadPersisted();
            HahUeuh.VISION_OF_DANGER.reloadPersisted();
            HahUeuh.VISION_OF_LIFE.reloadPersisted();
            HahUeuh.SLOTH_COMPAT.reload();
            HahUeuh.GREED_COMPAT.reload();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sendAuthoritiesTo(player);
                sendAbilitySlotsTo(player);
                HahUeuh.OBJECT_FREEZE.forceResetOnRollback(player);
            }
            HahUeuh.LIONS_HEART.restoreActiveOnRollback(snapshot.lionsHeartActive());
            HahUeuh.MATERIAL_PHASE.restoreActiveOnRollback(snapshot.materialPhaseActive());
            HahUeuh.BASE_SHIFT.restoreActiveOnRollback(snapshot.baseShiftActive());
            HahUeuh.SECOND_SHIFT.restoreActiveOnRollback(snapshot.secondShiftActive());
            HahUeuh.VISION_OF_DANGER.restoreActiveOnRollback(snapshot.visionOfDangerActive());
            HahUeuh.VISION_OF_LIFE.restoreActiveOnRollback(snapshot.visionOfLifeActive());
            HahUeuh.LITTLE_KING.refreshAllOnRollback();
            HahUeuh.ALLY_TRACKER.refreshAllOnRollback();
            restoreCooldowns(domainCooldownUntilTick, snapshot.domainCooldownRemaining(),
                    HahUeuhAbilities.DOMAIN_VICTIM_ABILITY, HahUeuhAbilities.DOMAIN_AGGRESSOR_ABILITY);
            HahUeuh.LIONS_HEART.restoreCooldownRemaining(snapshot.lionsHeartCooldownRemaining());
            HahUeuh.LITTLE_KING.restoreCooldownRemaining(snapshot.littleKingCooldownRemaining());
            HahUeuh.MATERIAL_PHASE.restoreCooldownRemaining(snapshot.materialPhaseCooldownRemaining());
            HahUeuh.OBJECT_FREEZE.restoreCooldownRemaining(snapshot.objectFreezeCooldownRemaining());
            HahUeuh.ALLY_TRACKER.restoreCooldownRemaining(snapshot.allyTrackerCooldownRemaining());
            HahUeuh.BASE_SHIFT.restoreCooldownRemaining(snapshot.baseShiftCooldownRemaining());
            HahUeuh.SECOND_SHIFT.restoreCooldownRemaining(snapshot.secondShiftCooldownRemaining());
            HahUeuh.BOOK_OF_WISDOM.restoreCooldownRemaining(snapshot.bookOfWisdomCooldownRemaining());
            HahUeuh.BOOK_OF_WISDOM.restoreSummonedState(snapshot.bookOfWisdomSummoned());
            HahUeuh.MENTAL_OVERLOAD.restoreCooldownRemaining(snapshot.mentalOverloadCooldownRemaining());
            HahUeuh.VISION_OF_DANGER.restoreCooldownRemaining(snapshot.visionOfDangerCooldownRemaining());
            HahUeuh.VISION_OF_LIFE.restoreCooldownRemaining(snapshot.visionOfLifeCooldownRemaining());
            HahUeuh.FOOTPRINT_TRACKER.restoreFootprints(snapshot.footprints());
            restoreCooldowns(slothCooldownUntilTick, snapshot.slothCooldownRemaining(),
                    HahUeuhAbilities.SLOTH_COOLDOWN_KEY);
            restoreCooldowns(quickCooldownUntilTick, snapshot.quickActionCooldownRemaining(),
                    HahUeuhAbilities.QUICK_ACTION_COOLDOWN_KEY);
            stepStart = logStepTime("reload authorities + compatibility", stepStart);

            ServerLevel overworld = server.overworld();
            overworld.setDayTime(snapshot.dayTime());
            ServerLevelData levelData = (ServerLevelData) overworld.getLevelData();
            levelData.setClearWeatherTime(snapshot.clearWeatherTime());
            levelData.setRainTime(snapshot.rainTime());
            levelData.setThunderTime(snapshot.thunderTime());
            levelData.setRaining(snapshot.raining());
            levelData.setThundering(snapshot.thundering());

            try {
                var worldData = overworld.getLevelData();
                java.lang.reflect.Method setGameTime = worldData.getClass().getMethod("setGameTime", long.class);
                setGameTime.invoke(worldData, snapshot.gameTime());
            } catch (Exception e) {
                LOGGER.debug("Could not restore game time (non-critical): {}", e.getMessage());
            }

            slot.modifiedChunks.clear();
            targetingSuppressUntilTick = overworld.getGameTime() + 20;
            if (slot == rbd) {
                tickCounter = 0;
                rollNextCheckpointInterval();
            }


            if (slot == rbd) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    playPersonalSound(player, ModSounds.HAHH);
                }
                ueuhPlayAtTick = overworld.getGameTime() + 20;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.info("World rolled back in {}ms ({} changed chunks, {} files restored)", elapsed, totalChanged, restoredFiles);

        } catch (Exception e) {
            LOGGER.error("Failed to roll back world!", e);
        } finally {
            rollbackInProgress = false;
        }
    }

    private long logStepTime(String stepName, long stepStart) {
        long now = System.currentTimeMillis();
        long elapsed = now - stepStart;
        if (elapsed >= 5) {
            LOGGER.debug("  rollback step [{}] took {}ms", stepName, elapsed);
        }
        return now;
    }


    private int restoreChangedFiles(Path worldDir, Path checkpointDir, Set<String> protectedNames,
                                    Map<Path, Long> snapshotTimestamps) throws IOException {
        int restoredCount = 0;

        for (Map.Entry<Path, Long> entry : snapshotTimestamps.entrySet()) {
            Path relativePath = entry.getKey();
            long snapshotTime = entry.getValue();

            if (isProtectedStorageFile(relativePath)) continue;

            String topName = relativePath.getName(0).toString();
            if (protectedNames.contains(topName)) continue;

            Path currentFile = worldDir.resolve(relativePath);
            Path checkpointFile = checkpointDir.resolve(relativePath);

            boolean needsRestore;
            if (!Files.exists(currentFile)) {
                needsRestore = true;
            } else {
                needsRestore = Files.getLastModifiedTime(currentFile).toMillis() != snapshotTime;
            }

            if (needsRestore && Files.exists(checkpointFile)) {
                Files.createDirectories(currentFile.getParent());
                Files.copy(checkpointFile, currentFile, StandardCopyOption.REPLACE_EXISTING);
                restoredCount++;
            }
        }

        deleteNewFiles(worldDir, worldDir, snapshotTimestamps, protectedNames);

        return restoredCount;
    }

    private boolean isProtectedStorageFile(Path relativePath) {
        Path parent = relativePath.getParent();
        String fileName = relativePath.getFileName().toString();
        if (parent == null || parent.getFileName() == null || !fileName.endsWith(".mca")) return false;
        String dir = parent.getFileName().toString();
        return dir.equals("region") || dir.equals("entities");
    }

    private void deleteNewFiles(Path baseDir, Path currentDir, Map<Path, Long> snapshotTimestamps,
                                Set<String> protectedNames) throws IOException {
        if (!Files.exists(currentDir) || !Files.isDirectory(currentDir)) return;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentDir)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (protectedNames.contains(name)) continue;

                if (Files.isDirectory(entry)) {
                    deleteNewFiles(baseDir, entry, snapshotTimestamps, protectedNames);
                } else {
                    Path relativePath = baseDir.relativize(entry);
                    if (isProtectedStorageFile(relativePath)) continue;
                    if (!snapshotTimestamps.containsKey(relativePath)) {
                        try {
                            Files.deleteIfExists(entry);
                        } catch (IOException e) {
                            LOGGER.warn("Failed to delete new file during rollback: {}", entry, e);
                        }
                    }
                }
            }
        }
    }


    private void captureNewChunkIntoCheckpoint(CheckpointSlot slot, ServerLevel level, ChunkAccess chunk) throws IOException {
        if (slot.snapshot == null) return;
        ChunkPos pos = chunk.getPos();
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        Path checkpointDir = slot.snapshot.checkpointDir();
        Path liveRegionDir = getDimensionPath(level).resolve("region");
        Path checkpointRegionDir = checkpointDir.resolve(worldDir.relativize(liveRegionDir));

        Files.createDirectories(checkpointRegionDir);
        Path checkpointRegionFile = checkpointRegionDir.resolve("r." + pos.getRegionX() + "." + pos.getRegionZ() + ".mca");
        RegionStorageInfo info = new RegionStorageInfo("hahueuh_capture", level.dimension(), "chunk");

        boolean alreadyPresent;
        try (RegionFile regionFile = new RegionFile(info, checkpointRegionFile, checkpointRegionDir, false)) {
            alreadyPresent = regionFile.hasChunk(pos);
            if (!alreadyPresent) {
                CompoundTag chunkNbt = ChunkSerializer.write(level, chunk);
                try (DataOutputStream out = regionFile.getChunkDataOutputStream(pos)) {
                    NbtIo.write(chunkNbt, out);
                }
            }
        }
        if (alreadyPresent) return;

        Path liveRegionFile = liveRegionDir.resolve("r." + pos.getRegionX() + "." + pos.getRegionZ() + ".mca");
        Path relativePath = worldDir.relativize(liveRegionFile);
        slot.snapshot.fileTimestamps().putIfAbsent(relativePath, System.currentTimeMillis());
    }


    private void restoreChangedChunks(ServerLevel level, Set<Long> changedChunks, Path checkpointDir) throws IOException {
        if (changedChunks.isEmpty()) return;

        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        Path liveRegionDir = getDimensionPath(level).resolve("region");
        Path checkpointRegionDir = checkpointDir.resolve(worldDir.relativize(liveRegionDir));
        RegionStorageInfo liveInfo = new RegionStorageInfo("hahueuh_live_restore", level.dimension(), "chunk");
        RegionStorageInfo ckptInfo = new RegionStorageInfo("hahueuh_ckpt_restore", level.dimension(), "chunk");

        Map<Long, List<ChunkPos>> byRegion = new HashMap<>();
        for (long chunkLong : changedChunks) {
            ChunkPos pos = new ChunkPos(chunkLong);
            byRegion.computeIfAbsent(ChunkPos.asLong(pos.getRegionX(), pos.getRegionZ()), k -> new ArrayList<>()).add(pos);
        }

        Map<Long, LevelChunk> loadedByPos = new HashMap<>();
        for (ChunkHolder holder : getLoadedChunkHolders(level)) {
            LevelChunk c = getLoadedChunk(holder);
            if (c != null && changedChunks.contains(c.getPos().toLong())) {
                loadedByPos.put(c.getPos().toLong(), c);
            }
        }

        int disk = 0, missing = 0, skippedProto = 0;
        for (List<ChunkPos> chunksInRegion : byRegion.values()) {
            ChunkPos any = chunksInRegion.get(0);
            String fileName = "r." + any.getRegionX() + "." + any.getRegionZ() + ".mca";
            Path ckptFile = checkpointRegionDir.resolve(fileName);
            if (!Files.exists(ckptFile)) { missing += chunksInRegion.size(); continue; }

            Files.createDirectories(liveRegionDir);
            try (RegionFile ckptRegion = new RegionFile(ckptInfo, ckptFile, checkpointRegionDir, false);
                 RegionFile liveRegion = new RegionFile(liveInfo, liveRegionDir.resolve(fileName), liveRegionDir, false)) {
                for (ChunkPos pos : chunksInRegion) {
                    if (!ckptRegion.hasChunk(pos)) { missing++; continue; }
                    CompoundTag nbt;
                    try (DataInputStream in = ckptRegion.getChunkDataInputStream(pos)) {
                        if (in == null) { missing++; continue; }
                        nbt = NbtIo.read(in);
                    }
                    if (nbt == null) { missing++; continue; }
                    if (!isFullyGeneratedChunk(nbt)) { skippedProto++; continue; }
                    try (DataOutputStream out = liveRegion.getChunkDataOutputStream(pos)) {
                        NbtIo.write(nbt, out);
                    }
                    disk++;
                }
            }
        }

        int mem = 0, failed = 0;
        if (!loadedByPos.isEmpty()) {
            for (List<ChunkPos> chunksInRegion : byRegion.values()) {
                ChunkPos any = chunksInRegion.get(0);
                Path ckptFile = checkpointRegionDir.resolve("r." + any.getRegionX() + "." + any.getRegionZ() + ".mca");
                if (!Files.exists(ckptFile)) continue;
                try (RegionFile ckptRegion = new RegionFile(ckptInfo, ckptFile, checkpointRegionDir, false)) {
                    for (ChunkPos pos : chunksInRegion) {
                        LevelChunk loaded = loadedByPos.get(pos.toLong());
                        if (loaded == null || !ckptRegion.hasChunk(pos)) continue;
                        CompoundTag nbt;
                        try (DataInputStream in = ckptRegion.getChunkDataInputStream(pos)) {
                            if (in == null) continue;
                            nbt = NbtIo.read(in);
                        }
                        if (nbt == null) continue;
                        if (!isFullyGeneratedChunk(nbt)) continue;
                        try {
                            applyChunkNbtInMemory(level, loaded, nbt);
                            mem++;
                        } catch (Exception e) {
                            failed++;
                            LOGGER.error("Failed to apply chunk {} in {} in-memory — it may now be PARTIALLY restored",
                                    pos, level.dimension().location(), e);
                        }
                    }
                }
            }
        }

        LOGGER.debug("Restored chunks in {}: {} on disk, {} in memory, {} left as-is (no checkpoint data), {} skipped (checkpoint not fully generated), {} region files, {} failed",
                level.dimension().location(), disk, mem, missing, skippedProto, byRegion.size(), failed);
    }

    private boolean isFullyGeneratedChunk(CompoundTag chunkNbt) {
        return ChunkSerializer.getChunkTypeFromTag(chunkNbt) == ChunkType.LEVELCHUNK;
    }

    private void evictPoiForChangedChunks(ServerLevel level, Set<Long> changedChunks) {
        if (changedChunks.isEmpty()) return;
        PoiManager poiManager = level.getChunkSource().getPoiManager();
        int minSection = level.getMinSection();
        int maxSection = level.getMaxSection();
        for (long chunkLong : changedChunks) {
            ChunkPos pos = new ChunkPos(chunkLong);
            for (int sectionY = minSection; sectionY < maxSection; sectionY++) {
                poiManager.remove(SectionPos.of(pos, sectionY).asLong());
            }
        }
    }

    private void clearUnsavedFlags(ServerLevel level) {
        int cleared = 0;
        for (ChunkHolder holder : getLoadedChunkHolders(level)) {
            ChunkAccess chunk = holder.getLatestChunk();
            if (chunk != null && chunk.isUnsaved()) {
                chunk.setUnsaved(false);
                cleared++;
            }
        }
        LOGGER.debug("Cleared unsaved flag on {} chunks in {} (prevents stale re-save over restored files)",
                cleared, level.dimension().location());
    }

    private void resetSavedDataCache(ServerLevel level) {
        try {
            DimensionDataStorage storage = level.getDataStorage();
            Field cacheField = DimensionDataStorage.class.getDeclaredField("cache");
            cacheField.setAccessible(true);
            Map<?, ?> cache = (Map<?, ?>) cacheField.get(storage);
            cache.clear();
        } catch (Exception e) {
            LOGGER.warn("Failed to reset SavedData cache for {} during rollback", level.dimension().location(), e);
        }
    }

    private void forceFlushSavedData(ServerLevel level) {
        try {
            DimensionDataStorage storage = level.getDataStorage();
            Field cacheField = DimensionDataStorage.class.getDeclaredField("cache");
            cacheField.setAccessible(true);
            Map<?, ?> cache = (Map<?, ?>) cacheField.get(storage);
            for (Object value : cache.values()) {
                if (value instanceof net.minecraft.world.level.saveddata.SavedData savedData) {
                    savedData.setDirty();
                }
            }
            storage.save();
        } catch (Exception e) {
            LOGGER.warn("Failed to force-flush SavedData for {} during checkpoint creation", level.dimension().location(), e);
        }
    }

    private Path getDimensionPath(ServerLevel level) {
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        ResourceKey<Level> dim = level.dimension();
        if (dim == Level.OVERWORLD) {
            return worldDir;
        } else if (dim == Level.NETHER) {
            return worldDir.resolve("DIM-1");
        } else if (dim == Level.END) {
            return worldDir.resolve("DIM1");
        } else {
            return worldDir.resolve("dimensions")
                    .resolve(dim.location().getNamespace())
                    .resolve(dim.location().getPath());
        }
    }

    @SuppressWarnings("unchecked")
    private Iterable<ChunkHolder> getLoadedChunkHolders(ServerLevel level) {
        try {
            Object chunkMap = level.getChunkSource().chunkMap;
            for (Class<?> clazz = chunkMap.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (field.getName().contains("visibleChunkMap") || field.getName().contains("visible")) {
                        if (it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap.class.isAssignableFrom(field.getType())) {
                            field.setAccessible(true);
                            var map = (it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap<ChunkHolder>) field.get(chunkMap);
                            return new ArrayList<>(map.values());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to access loaded chunks via reflection", e);
        }
        return List.of();
    }

    private LevelChunk getLoadedChunk(ChunkHolder holder) {
        return holder.getLatestChunk() instanceof LevelChunk levelChunk ? levelChunk : null;
    }

    private void applyChunkNbtInMemory(ServerLevel level, LevelChunk chunk, CompoundTag chunkNbt) {
        ChunkPos pos = chunk.getPos();

        List<BlockPos> lightRechecks = new ArrayList<>();
        var lightEngine = level.getChunkSource().getLightEngine();

        if (chunkNbt.contains("sections", Tag.TAG_LIST)) {
            ListTag sections = chunkNbt.getList("sections", Tag.TAG_COMPOUND);

            for (int i = 0; i < sections.size(); i++) {
                CompoundTag sectionTag = sections.getCompound(i);
                int sectionY = sectionTag.getByte("Y");
                int sectionIndex = level.getSectionIndexFromSectionY(sectionY);

                if (sectionIndex < 0 || sectionIndex >= chunk.getSectionsCount()) continue;

                if (!sectionTag.contains("block_states", Tag.TAG_COMPOUND)) {
                    continue;
                }
                var parsed = BLOCK_STATE_CODEC.parse(NbtOps.INSTANCE, sectionTag.getCompound("block_states")).result();
                if (parsed.isEmpty()) {
                    LOGGER.warn("Parse failed for section Y={} of chunk {} in {} — leaving existing section (not blanking to air)",
                            sectionY, pos, level.dimension().location());
                    continue;
                }
                PalettedContainer<BlockState> blockStates = parsed.get();

                LevelChunkSection oldSection = chunk.getSection(sectionIndex);
                @SuppressWarnings("unchecked")
                PalettedContainer<Holder<Biome>> biomes =
                        (PalettedContainer<Holder<Biome>>) oldSection.getBiomes();

                int baseX = pos.getMinBlockX();
                int baseY = SectionPos.sectionToBlockCoord(sectionY);
                int baseZ = pos.getMinBlockZ();
                for (int lx = 0; lx < 16; lx++) {
                    for (int ly = 0; ly < 16; ly++) {
                        for (int lz = 0; lz < 16; lz++) {
                            if (oldSection.getBlockState(lx, ly, lz) != blockStates.get(lx, ly, lz)) {
                                lightRechecks.add(new BlockPos(baseX + lx, baseY + ly, baseZ + lz));
                            }
                        }
                    }
                }

                LevelChunkSection newSection = new LevelChunkSection(blockStates, biomes);
                if (oldSection.hasOnlyAir() != newSection.hasOnlyAir()) {
                    lightEngine.updateSectionStatus(SectionPos.of(pos, sectionY), newSection.hasOnlyAir());
                }
                chunk.getSections()[sectionIndex] = newSection;
            }
        }

        Set<BlockPos> existingBEPositions = new HashSet<>(chunk.getBlockEntities().keySet());
        for (BlockPos bePos : existingBEPositions) {
            level.removeBlockEntity(bePos);
        }

        if (chunkNbt.contains("block_entities", Tag.TAG_LIST)) {
            ListTag blockEntities = chunkNbt.getList("block_entities", Tag.TAG_COMPOUND);

            for (int i = 0; i < blockEntities.size(); i++) {
                CompoundTag beNbt = blockEntities.getCompound(i);
                int x = beNbt.getInt("x");
                int y = beNbt.getInt("y");
                int z = beNbt.getInt("z");
                BlockPos bePos = new BlockPos(x, y, z);
                BlockState state = chunk.getBlockState(bePos);

                BlockEntity be = BlockEntity.loadStatic(bePos, state, beNbt, level.registryAccess());
                if (be != null) {
                    level.setBlockEntity(be);
                }
            }
        }

        Heightmap.primeHeightmaps(chunk, EnumSet.of(
                Heightmap.Types.MOTION_BLOCKING,
                Heightmap.Types.WORLD_SURFACE,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Heightmap.Types.OCEAN_FLOOR
        ));


        boolean relit = !lightRechecks.isEmpty();
        if (relit) {
            chunk.initializeLightSources();
            for (BlockPos changed : lightRechecks) {
                lightEngine.checkBlock(changed);
            }
        }

        ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(
                chunk, level.getLightEngine(), null, null
        );
        sendToChunkViewers(level, pos, packet);

        if (relit) {
            lightEngine.waitForPendingTasks(pos.x, pos.z).thenRunAsync(() -> {
                if (server == null) return;
                sendToChunkViewers(level, pos,
                        new ClientboundLightUpdatePacket(pos, level.getLightEngine(), null, null));
            }, level.getServer());
        }
    }

    private void sendToChunkViewers(ServerLevel level, ChunkPos pos, net.minecraft.network.protocol.Packet<?> packet) {
        int viewDistance = level.getServer().getPlayerList().getViewDistance() + 1;
        for (ServerPlayer player : level.players()) {
            SectionPos playerSection = player.getLastSectionPos();
            int dist = Math.max(Math.abs(playerSection.x() - pos.x), Math.abs(playerSection.z() - pos.z));
            if (dist <= viewDistance) {
                player.connection.send(packet);
            }
        }
    }

    private static PalettedContainer<BlockState> createEmptyBlockStates() {
        return new PalettedContainer<>(
                Block.BLOCK_STATE_REGISTRY,
                Blocks.AIR.defaultBlockState(),
                PalettedContainer.Strategy.SECTION_STATES
        );
    }


    private void restoreEntitiesForLevel(ServerLevel level, WorldSnapshot snapshot) {
        List<CompoundTag> savedEntities = snapshot.entityData().get(level.dimension());

        if (savedEntities == null) return;

        if (savedEntities.isEmpty() && level.dimension() == Level.END) {
            return;
        }

        Set<Long> loadedAtCheckpoint = snapshot.loadedChunks().getOrDefault(level.dimension(), Set.of());

        Map<UUID, CompoundTag> snapshotByUuid = new HashMap<>();
        for (CompoundTag nbt : savedEntities) {
            if (nbt.hasUUID("UUID")) snapshotByUuid.put(nbt.getUUID("UUID"), nbt);
        }

        List<Entity> current = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (!(e instanceof Player)) current.add(e);
        }

        int reverted = 0, removed = 0, spawned = 0;
        Set<UUID> handled = new HashSet<>();
        for (Entity e : current) {
            UUID id = e.getUUID();
            CompoundTag snap = snapshotByUuid.get(id);
            if (snap != null) {
                try {
                    e.load(snap);
                    resetTransientAiState(e);
                    reverted++;
                    handled.add(id);
                } catch (Exception ex) {
                    LOGGER.warn("Failed to revert entity {} in {}; discarding and re-adding from snapshot", id, level.dimension().location(), ex);
                    e.discard();
                }
            } else if (!e.isPassenger()) {
                long chunkKey = ChunkPos.asLong(e.blockPosition().getX() >> 4, e.blockPosition().getZ() >> 4);
                if (loadedAtCheckpoint.contains(chunkKey)) {
                    e.discard();
                    removed++;
                }
            }
        }

        for (Map.Entry<UUID, CompoundTag> entry : snapshotByUuid.entrySet()) {
            if (handled.contains(entry.getKey())) continue;
            try {
                Entity result = EntityType.loadEntityRecursive(entry.getValue(), level, entity -> {
                    BlockPos pos = entity.blockPosition();
                    if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return null;
                    level.addFreshEntity(entity);
                    return entity;
                });
                if (result != null) spawned++;
            } catch (Exception ex) {
                LOGGER.warn("Failed to re-add missing entity from snapshot", ex);
            }
        }

        if (reverted + removed + spawned > 0) {
            LOGGER.debug("Entities in {}: {} reverted, {} removed (post-checkpoint), {} re-added",
                    level.dimension().location(), reverted, removed, spawned);
        }
    }

    private void resetTransientAiState(Entity entity) {
        if (!(entity instanceof Mob mob)) return;
        mob.setTarget(null);
        mob.setLastHurtByMob(null);
        for (WrappedGoal goal : mob.goalSelector.getAvailableGoals()) {
            if (goal.isRunning()) goal.stop();
        }
        for (WrappedGoal goal : mob.targetSelector.getAvailableGoals()) {
            if (goal.isRunning()) goal.stop();
        }
    }


    private void closeAllRegionStorages() {
        for (ServerLevel level : server.getAllLevels()) {
            closeChunkRegionStorage(level);

            closeFieldStorages(level, "entityManager");
            closeFieldStorages(level, "poiManager");
        }
    }

    private void closeChunkRegionStorage(ServerLevel level) {
        try {
            Object chunkMap = level.getChunkSource().chunkMap;
            IOWorker worker = findFieldByType(chunkMap, IOWorker.class);
            if (worker != null) {
                RegionFileStorage storage = findFieldByType(worker, RegionFileStorage.class);
                if (storage != null) {
                    closeAndClearStorage(storage);
                    LOGGER.debug("Closed chunk RegionFileStorage for {}", level.dimension().location());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to close chunk region storage for {}", level.dimension().location(), e);
        }
    }

    private void closeFieldStorages(ServerLevel level, String fieldHint) {
        try {
            Object target = findFieldByNameHint(level, fieldHint);
            if (target == null) return;

            closeRegionStoragesRecursive(target, new HashSet<>(), 5);
        } catch (Exception e) {
            LOGGER.warn("Failed to close '{}' region storage for {}", fieldHint, level.dimension().location(), e);
        }
    }

    private void closeRegionStoragesRecursive(Object obj, Set<Object> visited, int depth) {
        if (obj == null || depth <= 0 || !visited.add(obj)) return;

        if (obj instanceof RegionFileStorage storage) {
            closeAndClearStorage(storage);
            return;
        }

        for (Class<?> clazz = obj.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (!isStorageRelatedType(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (value instanceof RegionFileStorage storage) {
                        closeAndClearStorage(storage);
                    } else if (value != null) {
                        closeRegionStoragesRecursive(value, visited, depth - 1);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private boolean isStorageRelatedType(Class<?> type) {
        if (RegionFileStorage.class.isAssignableFrom(type)) return true;
        if (IOWorker.class.isAssignableFrom(type)) return true;
        String name = type.getSimpleName().toLowerCase();
        return name.contains("storage") || name.contains("worker") || name.contains("region")
                || name.contains("persistent") || name.contains("sectionmanager");
    }

    private void closeAndClearStorage(RegionFileStorage storage) {
        try {
            storage.close();

            for (Field field : RegionFileStorage.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(storage);
                    if (value instanceof Map<?, ?> map) {
                        map.clear();
                        LOGGER.debug("Cleared RegionFileStorage cache (field: {})", field.getName());
                        return;
                    }
                } catch (Exception ignored) {
                }
            }
            LOGGER.debug("RegionFileStorage closed (cache field not found for clearing)");
        } catch (Exception e) {
            LOGGER.warn("Failed to close/clear RegionFileStorage", e);
        }
    }


    @SuppressWarnings("unchecked")
    private <T> T findFieldByType(Object instance, Class<T> targetType) {
        for (Class<?> clazz = instance.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (targetType.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        return (T) field.get(instance);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return null;
    }

    private Object findFieldByNameHint(Object instance, String nameHint) {
        String lower = nameHint.toLowerCase();
        for (Class<?> clazz = instance.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (field.getName().toLowerCase().contains(lower)) {
                    try {
                        field.setAccessible(true);
                        return field.get(instance);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return null;
    }


    private void copyDirectoryWithTimestamps(Path source, Path target, Set<String> excludes,
                                              Map<Path, Long> timestamps) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relativePath = source.relativize(dir);
                String dirName = dir.getFileName() != null ? dir.getFileName().toString() : "";

                if (!relativePath.toString().isEmpty() && excludes.contains(dirName)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                Path targetDir = target.resolve(relativePath);
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                if (excludes.contains(fileName)) {
                    return FileVisitResult.CONTINUE;
                }

                Path relativePath = source.relativize(file);
                Path targetFile = target.resolve(relativePath);
                try {
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                } catch (NoSuchFileException nsfe) {
                    LOGGER.debug("Skipped file that vanished mid-copy (likely an in-flight atomic save): {}", file);
                    return FileVisitResult.CONTINUE;
                }

                timestamps.put(relativePath, attrs.lastModifiedTime().toMillis());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                LOGGER.warn("Failed to copy file: {}", file, exc);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void copyDirectory(Path source, Path target, Set<String> excludes) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relativePath = source.relativize(dir);
                String dirName = dir.getFileName() != null ? dir.getFileName().toString() : "";

                if (!relativePath.toString().isEmpty() && excludes.contains(dirName)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                Path targetDir = target.resolve(relativePath);
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                if (excludes.contains(fileName)) {
                    return FileVisitResult.CONTINUE;
                }

                Path relativePath = source.relativize(file);
                Path targetFile = target.resolve(relativePath);
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                LOGGER.warn("Failed to copy file: {}", file, exc);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.deleteIfExists(d);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                LOGGER.warn("Failed to delete file: {}", file, exc);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
