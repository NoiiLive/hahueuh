package net.noiilive.hahueuh.snapshot;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.common.util.DummySavedData;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.noiilive.hahueuh.mixin.ChunkMapAccessor;
import net.noiilive.hahueuh.mixin.DimensionDataStorageAccessor;
import net.noiilive.hahueuh.mixin.RegionFileStorageAccessor;
import net.noiilive.hahueuh.mixin.SectionStorageAccessor;
import net.noiilive.hahueuh.mixin.ServerLevelEntityManagerAccessor;
import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.ConfigDomain;
import net.noiilive.hahueuh.ConfigReturnByDeath;
import net.noiilive.hahueuh.ModEffects;
import net.noiilive.hahueuh.ModSounds;
import net.noiilive.hahueuh.network.DeathFadePacket;
import net.noiilive.hahueuh.network.DeathFadeState;
import net.noiilive.hahueuh.HahUeuhAbilities;
import net.noiilive.hahueuh.network.AbilitySlotsSyncPacket;
import net.noiilive.hahueuh.network.DomainStatePacket;
import net.noiilive.hahueuh.network.AbilityCooldownPacket;
import net.noiilive.hahueuh.network.HandMode;
import net.noiilive.hahueuh.network.SlothVariant;
import net.noiilive.hahueuh.network.UnseenHandSyncPacket;
import net.noiilive.hahueuh.network.ModNetworking;
import net.noiilive.hahueuh.network.PlayerAuthoritiesPacket;
import org.slf4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
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
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Pose;
import net.noiilive.hahueuh.ConfigSloth;
import net.noiilive.hahueuh.ModGameRules;
import net.noiilive.hahueuh.network.UnseenHandGrabSyncPacket;

public class SnapshotManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();

    private static final Set<String> MOD_METADATA_FILES = Set.of(
            "hahueuh_authority.json",
            "hahueuh_door_crossing.json",
            "hahueuh_checkpoint_meta.dat");

    private static final Codec<PalettedContainer<BlockState>> BLOCK_STATE_CODEC = PalettedContainer.codecRW(
            Block.BLOCK_STATE_REGISTRY,
            BlockState.CODEC,
            PalettedContainer.Strategy.SECTION_STATES,
            Blocks.AIR.defaultBlockState());

    private static final String META_FILE = "hahueuh_checkpoint_meta.dat";
    private static final int RBD_PARTIAL_WINDOW_TICKS = 5 * 20;
    private static final int WITCH_SCENT_DURATION_TICKS = 5 * 60 * 20;

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
        if (hand.grabbed.length != n) hand.grabbed = new UUID[Math.max(0, n)];
    }

    private boolean grabsTarget(UUID holder, UUID target) {
        UnseenHand hand = unseenHands.get(holder);
        if (hand == null) return false;
        for (UUID id : hand.grabbed) {
            if (target.equals(id)) return true;
        }
        return false;
    }

    private boolean wouldGrabBackwards(ServerPlayer owner, Entity candidate) {
        return grabsTarget(candidate.getUUID(), owner.getUUID());
    }

    private boolean losesMutualGrab(ServerPlayer owner, Entity held) {
        return grabsTarget(held.getUUID(), owner.getUUID())
                && held.getUUID().compareTo(owner.getUUID()) < 0;
    }

    private static class CheckpointSlot {
        final String dirName;
        WorldSnapshot snapshot;
        final Map<ResourceKey<Level>, Set<Long>> modifiedChunks = new ConcurrentHashMap<>();

        CheckpointSlot(String dirName) {
            this.dirName = dirName;
        }

        boolean isActive() {
            return snapshot != null;
        }

        void resetTracking() {
            modifiedChunks.clear();
        }
    }

    private java.util.List<CheckpointSlot> activeSlots() {
        java.util.List<CheckpointSlot> slots = new java.util.ArrayList<>(2);
        if (rbd.isActive()) slots.add(rbd);
        if (domain.isActive()) slots.add(domain);
        return slots;
    }

    private final CheckpointSlot rbd = new CheckpointSlot("hahueuh_checkpoint");
    private final CheckpointSlot domain = new CheckpointSlot("hahueuh_domain_checkpoint");
    private final PlayerAuthorityManager authorityManager = new PlayerAuthorityManager();
    private final AbilitySlotsManager abilitySlotsManager = new AbilitySlotsManager();

    private MinecraftServer server;
    private int tickCounter;
    private int nextCheckpointIntervalSeconds;
    private boolean rollbackInProgress;
    private boolean internalSaveInProgress;
    private CheckpointSlot pendingRollback;
    private final Map<UUID, PlayerSnapshot> pendingPlayerRestores = new ConcurrentHashMap<>();
    private long targetingSuppressUntilTick;
    private long ueuhPlayAtTick = -1;

    private final Map<UUID, Integer> rbdPartialActivationTick = new HashMap<>();
    private final Map<UUID, Integer> pendingWitchScentBump = new HashMap<>();
    private final Map<UUID, Integer> pendingWitchScentDecay = new HashMap<>();

    private UUID domainOwnerUuid;
    private UUID domainSubjectUuid;
    private Vec3 domainMatrix;
    private ResourceKey<Level> domainDimension;
    private boolean domainCasterDeadHardcore;
    private int domainSubjectInsanityBase;
    private int domainInsanityStacks;
    private final Map<UUID, Integer> domainCooldownUntilTick = new HashMap<>();
    private static final float INVIS_PROVIDENCE_MAX_GRAB_SIZE = 1.5f;

    private final Map<UUID, UnseenHand> unseenHands = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Integer> slothCooldownUntilTick = new HashMap<>();
    private final Map<UUID, Integer> quickCooldownUntilTick = new HashMap<>();

    private long rollbackAtTick = -1;

    private static final int DIMENSION_SETTLE_SECONDS = 1;
    private long suppressAutoCheckpointsUntilTick = -1;
    private String deferredCheckpointReason;
    private long lastAutoCheckpointGameTick = -1;
    private final Map<UUID, ResourceKey<Level>> lastPlayerDimension = new HashMap<>();

    private static final int LIGHT_RESYNC_DELAY_TICKS = 4;
    private final Map<ResourceKey<Level>, Set<Long>> pendingLightResync = new HashMap<>();
    private long lightResyncAtTick = -1;

    public PlayerAuthorityManager getAuthorityManager() {
        return authorityManager;
    }

    public AbilitySlotsManager getAbilitySlotsManager() {
        return abilitySlotsManager;
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

    public boolean isDomainProtected(UUID uuid) {
        if (isDomainActive() && isDomainSubject(uuid)) return true;
        return isAggressorDomain() && isDomainOwner(uuid);
    }

    private void playDomainSound(SoundEvent sound) {
        if (server == null || domainDimension == null || domainMatrix == null) return;
        ServerLevel level = server.getLevel(domainDimension);
        if (level == null) return;
        level.playSound(null, domainMatrix.x, domainMatrix.y, domainMatrix.z,
                sound, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private void deactivateDomainState() {
        domainOwnerUuid = null;
        domainSubjectUuid = null;
        domainMatrix = null;
        domainDimension = null;
        domainCasterDeadHardcore = false;
        domainSubjectInsanityBase = 0;
        domainInsanityStacks = 0;
    }

    private double domainSphereRadius() {
        return ConfigDomain.DOMAIN_RADIUS.get() / 2.0;
    }

    private int domainCooldownRemainingTicks(UUID uuid) {
        Integer until = domainCooldownUntilTick.get(uuid);
        if (until == null || server == null) return 0;
        return Math.max(0, until - server.getTickCount());
    }

    public void toggleDomain(ServerPlayer player, boolean aggressor) {
        if (server == null) return;

        if (isDomainOwner(player.getUUID())) {
            if (rollbackAtTick >= 0) return;
            deactivateDomain("owner toggled off");
            player.displayClientMessage(Component.translatable("hahueuh.message.domain_closed")
                    .withStyle(ChatFormatting.AQUA), true);
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
            double maxHealth = ConfigDomain.DOMAIN_AGGRESSOR_MAX_HEALTH.get();
            if (!player.isCreative() && player.getHealth() > maxHealth) {
                player.displayClientMessage(Component.translatable("hahueuh.message.domain_aggressor_too_healthy",
                        String.format("%.1f", maxHealth)).withStyle(ChatFormatting.RED), true);
                return;
            }
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
        domainSubjectInsanityBase = HahUeuh.INSANITY.level(subject);
        domainInsanityStacks = 0;
        domainMatrix = subject.position();
        domainDimension = player.level().dimension();
        createSnapshot(domain, "domain:" + player.getGameProfile().getName());
        ModNetworking.sendToPlayer(player, activeDomainPacket());
        playDomainSound(ModSounds.DOMAIN_OPEN.get());

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
        playDomainSound(ModSounds.DOMAIN_CLOSE.get());
        deactivateDomainState();
        domain.snapshot = null;
        domain.resetTracking();

        int cooldownSeconds = ConfigDomain.DOMAIN_COOLDOWN_SECONDS.get();
        if (cooldownSeconds > 0 && server != null) {
            ServerPlayer cooldownOwner = server.getPlayerList().getPlayer(formerOwner);
            if (cooldownOwner == null || !cooldownOwner.isCreative()) {
                int untilTick = server.getTickCount() + cooldownSeconds * 20;
                domainCooldownUntilTick.put(formerOwner, untilTick);
                if (cooldownOwner != null) {
                    ModNetworking.sendToPlayer(cooldownOwner, new AbilityCooldownPacket(
                            HahUeuhAbilities.DOMAIN_VICTIM_ABILITY, cooldownSeconds * 20));
                    ModNetworking.sendToPlayer(cooldownOwner, new AbilityCooldownPacket(
                            HahUeuhAbilities.DOMAIN_AGGRESSOR_ABILITY, cooldownSeconds * 20));
                }
            }
        }
        if (server != null) {
            Path domainDir = server.getWorldPath(LevelResource.ROOT).resolve(domain.dirName);
            if (Files.exists(domainDir)) {
                try {
                    deleteDirectory(domainDir);
                } catch (Exception e) {
                    LOGGER.warn("Failed to delete domain checkpoint dir on close", e);
                }
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(formerOwner);
            if (owner != null) ModNetworking.sendToPlayer(owner, DomainStatePacket.INACTIVE);
        }
    }

    private void tickDomainEnforcement() {
        if (!isDomainActive()) return;
        if (rollbackAtTick >= 0) return;
        ServerPlayer owner = server.getPlayerList().getPlayer(domainOwnerUuid);
        if (owner == null) { deactivateDomain("owner offline"); return; }
        if (domainCasterDeadHardcore) return;

        if (!owner.level().dimension().equals(domainDimension)) {
            deactivateDomain("owner changed dimension");
            return;
        }
        double r2 = domainSphereRadius() * domainSphereRadius();
        if (owner.position().distanceToSqr(domainMatrix) > r2) {
            deactivateDomain("owner left radius");
            return;
        }

        if (isAggressorDomain() && rollbackAtTick < 0) {
            LivingEntity subject = findDomainSubjectEntity();
            if (subject != null && (!subject.level().dimension().equals(domainDimension)
                    || subject.position().distanceToSqr(domainMatrix) > r2)) {
                healAndSignal(subject);
                scheduleRollback(domain);
            }
        }
    }

    private void bumpSubjectInsanity() {
        if (!isAggressorDomain()) return;
        domainInsanityStacks++;
        LivingEntity subject = findDomainSubjectEntity();
        if (subject == null) return;
        HahUeuh.INSANITY.applyLevel(subject,
                Math.max(HahUeuh.INSANITY.level(subject), domainSubjectInsanityBase + domainInsanityStacks));
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

    private DomainStatePacket activeDomainPacket() {
        return new DomainStatePacket(true, domainMatrix.x, domainMatrix.y, domainMatrix.z,
                domainSphereRadius(), domainDimension.location());
    }

    public void onUnseenHandUpdate(ServerPlayer owner, boolean active, float distance, int modeId,
                                   boolean mobility, boolean quickSession) {
        if (server == null) return;
        UUID uuid = owner.getUUID();
        UnseenHand existing = unseenHands.get(uuid);
        boolean wasActive = existing != null;

        boolean actsAsUnseenHands = authorityManager.canUseSloth(uuid)
                && authorityManager.getSlothVariant(uuid) == SlothVariant.UNSEEN_HANDS;
        int fingerHands = HahUeuh.FINGER_GRANT.receivedHands(uuid);
        boolean isFingerRecipient = fingerHands > 0;
        boolean canBear = authorityManager.canUseSloth(uuid) || isFingerRecipient;
        int handCount = actsAsUnseenHands ? HahUeuh.FINGER_GRANT.effectiveCount(uuid)
                : isFingerRecipient ? fingerHands : 0;

        boolean show = active && canBear;
        if (show && !wasActive && !owner.isCreative()) {
            int remaining = quickSession ? quickCooldownRemainingTicks(uuid) : slothCooldownRemainingTicks(uuid);
            if (remaining > 0) {
                show = false;
                ModNetworking.sendToPlayer(owner, new AbilityCooldownPacket(
                        quickSession ? HahUeuhAbilities.QUICK_ACTION_COOLDOWN_KEY : HahUeuhAbilities.SLOTH_COOLDOWN_KEY,
                        remaining));
            }
        }

        boolean mobilityAllowed = mobility && handCount >= 2 && (actsAsUnseenHands || isFingerRecipient);
        boolean wasMobility = existing != null && existing.mobility;
        if (show && quickSession && !wasActive) {
            net.noiilive.hahueuh.Miasma.addSingleUse(owner);
        }

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
        int variantOrdinal = (authorityManager.canUseSloth(uuid)
                ? authorityManager.getSlothVariant(uuid) : SlothVariant.UNSEEN_HANDS).ordinal();
        UnseenHandSyncPacket packet = new UnseenHandSyncPacket(owner.getUUID(), owner.getId(), show, distance,
                show ? HandMode.byId(modeId).ordinal() : 0,
                variantOrdinal, show && mobilityAllowed, handCount);
        ResourceKey<Level> dim = owner.level().dimension();
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (viewer == owner) continue;
            if (!canSeeUnseenHands(viewer.getUUID())) continue;
            if (!viewer.level().dimension().equals(dim)) continue;
            ModNetworking.sendToPlayer(viewer, packet);
        }
    }

    private boolean canSeeUnseenHands(UUID uuid) {
        return authorityManager.canUseSloth(uuid) || HahUeuh.FINGER_GRANT.receivedHands(uuid) > 0;
    }

    public boolean isUnseenHandMobility(UUID uuid) {
        UnseenHand hand = unseenHands.get(uuid);
        return hand != null && hand.mobility;
    }

    public boolean hasSustainedUnseenHand(UUID uuid) {
        UnseenHand hand = unseenHands.get(uuid);
        return hand != null && !hand.quickSession;
    }

    private void clearUnseenHand(UUID owner) {
        if (unseenHands.remove(owner) != null && server != null) {
            UnseenHandSyncPacket off = new UnseenHandSyncPacket(owner, -1, false, 0f, 0, 0, false, 0);
            for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
                if (!viewer.getUUID().equals(owner) && canSeeUnseenHands(viewer.getUUID())) {
                    ModNetworking.sendToPlayer(viewer, off);
                }
            }
        }
    }

    private void sendActiveUnseenHandsTo(ServerPlayer viewer) {
        if (server == null || !canSeeUnseenHands(viewer.getUUID())) return;
        for (Map.Entry<UUID, UnseenHand> entry : unseenHands.entrySet()) {
            if (entry.getKey().equals(viewer.getUUID())) continue;
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (owner != null && owner.level().dimension().equals(viewer.level().dimension())) {
                ModNetworking.sendToPlayer(viewer, new UnseenHandSyncPacket(entry.getKey(), owner.getId(), true,
                        entry.getValue().distance, entry.getValue().mode.ordinal(),
                        handVariantOrdinal(entry.getKey()), entry.getValue().mobility,
                        handRenderCount(entry.getKey())));
            }
        }
    }

    private int handRenderCount(UUID uuid) {
        boolean actsAsUnseenHands = authorityManager.canUseSloth(uuid)
                && authorityManager.getSlothVariant(uuid) == SlothVariant.UNSEEN_HANDS;
        if (actsAsUnseenHands) return HahUeuh.FINGER_GRANT.effectiveCount(uuid);
        return HahUeuh.FINGER_GRANT.receivedHands(uuid);
    }

    private int handVariantOrdinal(UUID uuid) {
        return (authorityManager.canUseSloth(uuid)
                ? authorityManager.getSlothVariant(uuid) : SlothVariant.UNSEEN_HANDS).ordinal();
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
        int cooldownSeconds = quick ? net.noiilive.hahueuh.ConfigSloth.QUICK_ACTION_COOLDOWN_SECONDS.get()
                                    : net.noiilive.hahueuh.ConfigSloth.SLOTH_COOLDOWN_SECONDS.get();
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
        ModNetworking.sendToPlayer(owner, new AbilityCooldownPacket(key, ticks));
    }

    public void sendDomainStateTo(ServerPlayer player) {
        if (server == null) return;
        DomainStatePacket packet = (isDomainActive() && isDomainOwner(player.getUUID()))
                ? activeDomainPacket()
                : DomainStatePacket.INACTIVE;
        ModNetworking.sendToPlayer(player, packet);
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

        UnseenHandGrabSyncPacket packet = new UnseenHandGrabSyncPacket(owner.getUUID(), ids);
        ModNetworking.sendToPlayer(owner, packet);
        ResourceKey<Level> dim = owner.level().dimension();
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            if (viewer == owner) continue;
            if (!authorityManager.canUseSloth(viewer.getUUID())) continue;
            if (!viewer.level().dimension().equals(dim)) continue;
            ModNetworking.sendToPlayer(viewer, packet);
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

    private void crushImplantedHeartsAtHand(ServerLevel level, AABB reach) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, reach)) {
            if (HahUeuh.LITTLE_KING.crushImplant(e)) {
                level.playSound(null, e.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 0.4f, 0.6f);
            }
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
                if (e == null || !e.isAlive() || isRidingOn(owner, e) || losesMutualGrab(owner, e)) hand.grabbed[i] = null;
                else claimed.add(hand.grabbed[i]);
            }

            for (int i = 0; i < count; i++) {
                if (hand.grabbed[i] != null) continue;
                List<Entity> cands = level.getEntities(owner, new AABB(tips[i], tips[i]).inflate(0.6),
                        e -> e.isAlive() && e != owner && !e.isSpectator() && !isRidingOn(owner, e)
                                && !claimed.contains(e.getUUID()) && !wouldGrabBackwards(owner, e));
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
                holdGrabbed(e, target);
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
        double speed = ConfigSloth.UNSEEN_HANDS_MOBILITY_SPEED.get() / 20.0;

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
        double maxReach = ConfigSloth.SLOTH_MAX_DISTANCE.get() * SlothVariant.SEKHMET.reachMultiplier;
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
            away = away.lengthSqr() > 1.0e-4 ? away.normalize()
                    : new Vec3(owner.getViewVector(1.0f).x, 0, owner.getViewVector(1.0f).z).normalize();
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
        if (grabbed == null || !grabbed.isAlive() || isRidingOn(owner, grabbed) || losesMutualGrab(owner, grabbed)) {
            grabbed = null;
            for (Entity e : level.getEntities(owner, reach,
                    e -> e.isAlive() && e != owner && !e.isSpectator() && !isRidingOn(owner, e)
                            && !exclude.contains(e.getUUID()) && !wouldGrabBackwards(owner, e)
                            && extra.test(e))) {
                grabbed = e;
                break;
            }
        }
        if (grabbed == null) return null;
        Vec3 target = new Vec3(tip.x, tip.y - grabbed.getBbHeight() / 2.0, tip.z);
        holdGrabbed(grabbed, target);
        return grabbed.getUUID();
    }

    private static void holdGrabbed(Entity e, Vec3 target) {
        e.setDeltaMovement(target.subtract(e.position()));
        e.hasImpulse = true;
        e.hurtMarked = true;
        if (e instanceof ServerPlayer sp) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
        }
    }

    private void nudgeBlockAtHand(ServerLevel level, ServerPlayer owner, Vec3 tip, UnseenHand hand) {
        BlockPos tipBlock = BlockPos.containing(tip);
        if (tipBlock.equals(hand.lastInteractBlock)) return;
        hand.lastInteractBlock = tipBlock;

        BlockState state = level.getBlockState(tipBlock);
        if (state.isAir() || state.getBlock() instanceof SignBlock) return;

        BlockHitResult hit = new BlockHitResult(tip, owner.getDirection(), tipBlock, false);
        boolean hadMenuOpen = owner.hasContainerOpen();
        ItemStack realHeld = owner.getItemInHand(InteractionHand.MAIN_HAND);
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        try {
            net.noiilive.hahueuh.ProxiedInteractionSound.begin(owner);
            state.use(level, owner, InteractionHand.MAIN_HAND, hit);
        } finally {
            net.noiilive.hahueuh.ProxiedInteractionSound.end();
            owner.setItemInHand(InteractionHand.MAIN_HAND, realHeld);
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
        net.noiilive.hahueuh.ProxiedInteractionSound.begin(owner);
        try {
            stand.interactAt(owner, relative, InteractionHand.MAIN_HAND);
        } finally {
            net.noiilive.hahueuh.ProxiedInteractionSound.end();
        }
        ItemStack takenFromStand = owner.getItemInHand(InteractionHand.MAIN_HAND);
        owner.setItemInHand(InteractionHand.MAIN_HAND, realHeld);
        if (!takenFromStand.isEmpty() && !owner.getInventory().add(takenFromStand)) {
            owner.drop(takenFromStand, false);
        }
    }

    public void sendAuthoritiesTo(ServerPlayer player) {
        if (server == null) return;
        sendActiveUnseenHandsTo(player);
        List<String> owned = new ArrayList<>();
        if (authorityManager.canReturnByDeath(player.getUUID())) {
            owned.add(HahUeuhAbilities.RETURN_BY_DEATH_AUTHORITY.toString());
        }
        if (authorityManager.canUseDomain(player.getUUID())) {
            owned.add(HahUeuhAbilities.DOMAIN_AUTHORITY.toString());
        }
        if (authorityManager.canUseSloth(player.getUUID())) {
            owned.add(HahUeuhAbilities.SLOTH_AUTHORITY.toString());
        }
        if (authorityManager.canUseGreed(player.getUUID())) {
            owned.add(HahUeuhAbilities.GREED_AUTHORITY.toString());
        }
        if (HahUeuh.FINGER_GRANT.receivedHands(player.getUUID()) > 0) {
            owned.add(HahUeuhAbilities.FINGER_AUTHORITY.toString());
        }
        ModNetworking.sendToPlayer(player, new net.noiilive.hahueuh.network.SlothStatePacket(
                authorityManager.canUseSloth(player.getUUID()),
                authorityManager.getSlothVariant(player.getUUID()).ordinal(),
                handRenderCount(player.getUUID()),
                HahUeuh.FINGER_GRANT.receivedHands(player.getUUID())));
        ModNetworking.sendToPlayer(player, new net.noiilive.hahueuh.network.GreedStatePacket(
                authorityManager.canUseGreed(player.getUUID()),
                authorityManager.getGreedVariant(player.getUUID()).ordinal()));
        ModNetworking.sendToPlayer(player, new PlayerAuthoritiesPacket(owned));
    }

    public void sendAbilitySlotsTo(ServerPlayer player) {
        if (server == null) return;
        ModNetworking.sendToPlayer(player,
                new AbilitySlotsSyncPacket(abilitySlotsManager.get(player.getUUID())));
    }

    public boolean isTargetingSuppressed() {
        return server != null && server.overworld().getGameTime() < targetingSuppressUntilTick;
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
        rollbackInProgress = false;
        internalSaveInProgress = false;
        pendingRollback = null;
        pendingPlayerRestores.clear();
        rollbackAtTick = -1;
        pendingLightResync.clear();
        lightResyncAtTick = -1;
        suppressAutoCheckpointsUntilTick = -1;
        deferredCheckpointReason = null;
        lastAutoCheckpointGameTick = -1;
        lastPlayerDimension.clear();
        rbd.snapshot = null;
        rbd.resetTracking();
        domain.snapshot = null;
        domain.resetTracking();
        deactivateDomainState();
        domainCooldownUntilTick.clear();
        unseenHands.clear();
        slothCooldownUntilTick.clear();
        quickCooldownUntilTick.clear();
        rbdPartialActivationTick.clear();
        pendingWitchScentBump.clear();
        pendingWitchScentDecay.clear();
        ueuhPlayAtTick = -1;
        targetingSuppressUntilTick = -1;
        authorityManager.load(server);
        abilitySlotsManager.load(server);
        rollNextCheckpointInterval();
        tickCounter = Math.min(loadPersistedTickCounter(server), nextCheckpointIntervalSeconds * 20);
        LOGGER.info("HahUeuh checkpoint system initialized (interval: {}s +/- {}s, resuming timer at {}/{} ticks)",
                ConfigReturnByDeath.CHECKPOINT_INTERVAL_SECONDS.get(),
                ConfigReturnByDeath.CHECKPOINT_INTERVAL_RANDOMNESS_SECONDS.get(),
                tickCounter, nextCheckpointIntervalSeconds * 20);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        Path worldDir = server.getWorldPath(LevelResource.ROOT);

        Path domainDir = worldDir.resolve(domain.dirName);
        if (Files.exists(domainDir)) {
            try {
                deleteDirectory(domainDir);
            } catch (Exception e) {
                LOGGER.warn("Failed to delete stale domain checkpoint on startup", e);
            }
        }

        Path checkpointDir = worldDir.resolve(rbd.dirName);

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
        persistTickCounter(server);
        rbd.snapshot = null;
        rbd.resetTracking();
        rbdPartialActivationTick.clear();
        pendingWitchScentBump.clear();
        pendingWitchScentDecay.clear();
        server = null;
    }

    private static final String TIMER_STATE_FILE_NAME = "hahueuh_timer_state.txt";

    private void persistTickCounter(MinecraftServer server) {
        if (server == null) return;
        try {
            Path path = server.getWorldPath(LevelResource.ROOT).resolve(TIMER_STATE_FILE_NAME);
            Files.writeString(path, Integer.toString(tickCounter), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Failed to persist checkpoint timer state", e);
        }
    }

    private int loadPersistedTickCounter(MinecraftServer server) {
        try {
            Path path = server.getWorldPath(LevelResource.ROOT).resolve(TIMER_STATE_FILE_NAME);
            if (!Files.exists(path)) return 0;
            return Math.max(0, Integer.parseInt(
                    Files.readString(path, java.nio.charset.StandardCharsets.UTF_8).trim()));
        } catch (Exception e) {
            return 0;
        }
    }

    private void rebuildSnapshotFromDisk(Path checkpointDir) throws IOException {
        Path metaFile = checkpointDir.resolve(META_FILE);
        if (!Files.exists(metaFile)) return;

        CompoundTag meta = NbtIo.readCompressed(metaFile.toFile());
        if (meta == null) return;

        Map<ResourceKey<Level>, List<CompoundTag>> entityData = new HashMap<>();
        CompoundTag entitiesTag = meta.getCompound("Entities");
        for (String dim : entitiesTag.getAllKeys()) {
            List<CompoundTag> list = new ArrayList<>();
            ListTag stored = entitiesTag.getList(dim, Tag.TAG_COMPOUND);
            for (int i = 0; i < stored.size(); i++) {
                list.add(stored.getCompound(i));
            }
            entityData.put(dimensionKey(dim), list);
        }

        Map<ResourceKey<Level>, Set<Long>> loadedChunks = new HashMap<>();
        CompoundTag chunksTag = meta.getCompound("LoadedChunks");
        for (String dim : chunksTag.getAllKeys()) {
            Set<Long> set = new HashSet<>();
            for (long value : chunksTag.getLongArray(dim)) {
                set.add(value);
            }
            loadedChunks.put(dimensionKey(dim), set);
        }

        Map<UUID, PlayerSnapshot> playerData = loadPlayerSnapshotsFromDisk(meta);

        Map<Path, Long> fileTimestamps = new HashMap<>();
        ListTag files = meta.getList("Files", Tag.TAG_COMPOUND);
        for (int i = 0; i < files.size(); i++) {
            CompoundTag entry = files.getCompound(i);
            fileTimestamps.put(Path.of(entry.getString("Path")), entry.getLong("Time"));
        }

        rbd.snapshot = new WorldSnapshot(
                checkpointDir,
                entityData,
                loadedChunks,
                playerData,
                meta.getLong("GameTime"),
                meta.getLong("DayTime"),
                meta.getBoolean("Raining"),
                meta.getBoolean("Thundering"),
                meta.getInt("ClearWeatherTime"),
                meta.getInt("RainTime"),
                meta.getInt("ThunderTime"),
                fileTimestamps,
                cooldownsFromNbt(meta.getCompound("DomainCooldowns")),
                cooldownsFromNbt(meta.getCompound("SlothCooldowns")),
                cooldownsFromNbt(meta.getCompound("QuickActionCooldowns")),
                cooldownsFromNbt(meta.getCompound("LionsHeartCooldowns")),
                cooldownsFromNbt(meta.getCompound("LittleKingCooldowns")),
                cooldownsFromNbt(meta.getCompound("MaterialPhaseCooldowns")),
                cooldownsFromNbt(meta.getCompound("ObjectFreezeCooldowns")),
                cooldownsFromNbt(meta.getCompound("AllyTrackerCooldowns")),
                cooldownsFromNbt(meta.getCompound("BaseShiftCooldowns")),
                cooldownsFromNbt(meta.getCompound("SecondShiftCooldowns")),
                cooldownsFromNbt(meta.getCompound("BookOfWisdomCooldowns")),
                uuidSetFromNbt(meta.getList("BookOfWisdomSummoned", Tag.TAG_STRING)),
                cooldownsFromNbt(meta.getCompound("MentalOverloadCooldowns")),
                cooldownsFromNbt(meta.getCompound("VisionOfDangerCooldowns")),
                uuidSetFromNbt(meta.getList("VisionOfDangerActive", Tag.TAG_STRING)),
                cooldownsFromNbt(meta.getCompound("VisionOfLifeCooldowns")),
                uuidSetFromNbt(meta.getList("VisionOfLifeActive", Tag.TAG_STRING)),
                footprintsFromNbt(meta.getList("Footprints", Tag.TAG_COMPOUND)),
                activeDurationsFromNbt(meta.getCompound("LionsHeartActive")),
                uuidSetFromNbt(meta.getList("MaterialPhaseActive", Tag.TAG_STRING)),
                cooldownsFromNbt(meta.getCompound("FingerGrantCooldowns")),
                spellCooldownsFromNbt(meta.getCompound("SpellCooldowns")));
    }

    private Map<UUID, PlayerSnapshot> loadPlayerSnapshotsFromDisk(CompoundTag meta) {
        Map<UUID, PlayerSnapshot> playerData = new HashMap<>();
        ListTag players = meta.getList("Players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag entry = players.getCompound(i);
            try {
                playerData.put(entry.getUUID("UUID"), PlayerSnapshot.fromNbt(entry.getCompound("Data")));
            } catch (Exception e) {
                LOGGER.warn("Skipping malformed player snapshot in checkpoint metadata", e);
            }
        }
        return playerData;
    }

    private void saveSnapshotMetadataToDisk(Path checkpointDir, WorldSnapshot snapshot) {
        try {
            CompoundTag meta = new CompoundTag();

            CompoundTag entitiesTag = new CompoundTag();
            snapshot.entityData().forEach((dim, list) -> {
                ListTag stored = new ListTag();
                stored.addAll(list);
                entitiesTag.put(dim.location().toString(), stored);
            });
            meta.put("Entities", entitiesTag);

            CompoundTag chunksTag = new CompoundTag();
            snapshot.loadedChunks().forEach((dim, set) -> {
                long[] values = new long[set.size()];
                int i = 0;
                for (long value : set) values[i++] = value;
                chunksTag.putLongArray(dim.location().toString(), values);
            });
            meta.put("LoadedChunks", chunksTag);

            ListTag players = new ListTag();
            snapshot.playerData().forEach((uuid, ps) -> {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("UUID", uuid);
                entry.put("Data", ps.toNbt());
                players.add(entry);
            });
            meta.put("Players", players);

            ListTag files = new ListTag();
            snapshot.fileTimestamps().forEach((path, time) -> {
                CompoundTag entry = new CompoundTag();
                entry.putString("Path", path.toString());
                entry.putLong("Time", time);
                files.add(entry);
            });
            meta.put("Files", files);

            meta.putLong("GameTime", snapshot.gameTime());
            meta.putLong("DayTime", snapshot.dayTime());
            meta.putBoolean("Raining", snapshot.raining());
            meta.putBoolean("Thundering", snapshot.thundering());
            meta.putInt("ClearWeatherTime", snapshot.clearWeatherTime());
            meta.putInt("RainTime", snapshot.rainTime());
            meta.putInt("ThunderTime", snapshot.thunderTime());
            meta.put("DomainCooldowns", cooldownsToNbt(snapshot.domainCooldownRemaining()));
            meta.put("SlothCooldowns", cooldownsToNbt(snapshot.slothCooldownRemaining()));
            meta.put("QuickActionCooldowns", cooldownsToNbt(snapshot.quickActionCooldownRemaining()));
            meta.put("LionsHeartCooldowns", cooldownsToNbt(snapshot.lionsHeartCooldownRemaining()));
            meta.put("LittleKingCooldowns", cooldownsToNbt(snapshot.littleKingCooldownRemaining()));
            meta.put("MaterialPhaseCooldowns", cooldownsToNbt(snapshot.materialPhaseCooldownRemaining()));
            meta.put("ObjectFreezeCooldowns", cooldownsToNbt(snapshot.objectFreezeCooldownRemaining()));
            meta.put("AllyTrackerCooldowns", cooldownsToNbt(snapshot.allyTrackerCooldownRemaining()));
            meta.put("BaseShiftCooldowns", cooldownsToNbt(snapshot.baseShiftCooldownRemaining()));
            meta.put("SecondShiftCooldowns", cooldownsToNbt(snapshot.secondShiftCooldownRemaining()));
            meta.put("FingerGrantCooldowns", cooldownsToNbt(snapshot.fingerGrantCooldownRemaining()));
            meta.put("SpellCooldowns", spellCooldownsToNbt(snapshot.spellCooldownRemaining()));
            meta.put("BookOfWisdomCooldowns", cooldownsToNbt(snapshot.bookOfWisdomCooldownRemaining()));
            meta.put("BookOfWisdomSummoned", uuidSetToNbt(snapshot.bookOfWisdomSummoned()));
            meta.put("MentalOverloadCooldowns", cooldownsToNbt(snapshot.mentalOverloadCooldownRemaining()));
            meta.put("VisionOfDangerCooldowns", cooldownsToNbt(snapshot.visionOfDangerCooldownRemaining()));
            meta.put("VisionOfDangerActive", uuidSetToNbt(snapshot.visionOfDangerActive()));
            meta.put("VisionOfLifeCooldowns", cooldownsToNbt(snapshot.visionOfLifeCooldownRemaining()));
            meta.put("VisionOfLifeActive", uuidSetToNbt(snapshot.visionOfLifeActive()));
            meta.put("Footprints", footprintsToNbt(snapshot.footprints()));
            meta.put("LionsHeartActive", activeDurationsToNbt(snapshot.lionsHeartActive()));
            meta.put("MaterialPhaseActive", uuidSetToNbt(snapshot.materialPhaseActive()));

            NbtIo.writeCompressed(meta, checkpointDir.resolve(META_FILE).toFile());
        } catch (IOException e) {
            LOGGER.error("Failed to save checkpoint metadata to disk", e);
        }
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
                ModNetworking.sendToPlayer(player, new AbilityCooldownPacket(abilityId, remaining));
            }
        }
    }

    private static CompoundTag cooldownsToNbt(Map<UUID, Integer> remaining) {
        CompoundTag tag = new CompoundTag();
        remaining.forEach((uuid, ticks) -> tag.putInt(uuid.toString(), ticks));
        return tag;
    }

    private static CompoundTag activeDurationsToNbt(Map<UUID, int[]> active) {
        CompoundTag tag = new CompoundTag();
        active.forEach((uuid, pair) -> tag.putIntArray(uuid.toString(), pair));
        return tag;
    }

    private static Map<UUID, int[]> activeDurationsFromNbt(CompoundTag tag) {
        Map<UUID, int[]> result = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            try {
                int[] pair = tag.getIntArray(key);
                if (pair.length == 2) result.put(UUID.fromString(key), pair);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    private static ListTag footprintsToNbt(List<net.noiilive.hahueuh.FootprintTracker.FootprintEntry> entries) {
        ListTag list = new ListTag();
        for (net.noiilive.hahueuh.FootprintTracker.FootprintEntry e : entries) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dim", e.dimension().location().toString());
            tag.putUUID("Owner", e.owner());
            tag.putDouble("X", e.x());
            tag.putDouble("Y", e.y());
            tag.putDouble("Z", e.z());
            tag.putFloat("Yaw", e.yaw());
            tag.putString("Name", e.name());
            tag.putInt("Category", e.category());
            tag.putLong("Time", e.timestamp());
            list.add(tag);
        }
        return list;
    }

    private static List<net.noiilive.hahueuh.FootprintTracker.FootprintEntry> footprintsFromNbt(ListTag list) {
        List<net.noiilive.hahueuh.FootprintTracker.FootprintEntry> entries = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            ResourceLocation dim = ResourceLocation.tryParse(tag.getString("Dim"));
            if (dim == null || !tag.hasUUID("Owner")) continue;
            entries.add(new net.noiilive.hahueuh.FootprintTracker.FootprintEntry(
                    ResourceKey.create(Registries.DIMENSION, dim),
                    tag.getUUID("Owner"),
                    tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"),
                    tag.getFloat("Yaw"), tag.getString("Name"),
                    tag.getInt("Category"), tag.getLong("Time")));
        }
        return entries;
    }

    private static CompoundTag spellCooldownsToNbt(Map<UUID, Map<ResourceLocation, Integer>> remaining) {
        CompoundTag tag = new CompoundTag();
        remaining.forEach((uuid, spellMap) -> {
            CompoundTag spellTag = new CompoundTag();
            spellMap.forEach((id, ticks) -> spellTag.putInt(id.toString(), ticks));
            tag.put(uuid.toString(), spellTag);
        });
        return tag;
    }

    private static Map<UUID, Map<ResourceLocation, Integer>> spellCooldownsFromNbt(CompoundTag tag) {
        Map<UUID, Map<ResourceLocation, Integer>> result = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                CompoundTag spellTag = tag.getCompound(key);
                Map<ResourceLocation, Integer> spellMap = new HashMap<>();
                for (String spellKey : spellTag.getAllKeys()) {
                    ResourceLocation id = ResourceLocation.tryParse(spellKey);
                    if (id != null) spellMap.put(id, spellTag.getInt(spellKey));
                }
                if (!spellMap.isEmpty()) result.put(uuid, spellMap);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    private static ListTag uuidSetToNbt(Set<UUID> uuids) {
        ListTag list = new ListTag();
        for (UUID uuid : uuids) list.add(StringTag.valueOf(uuid.toString()));
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

    private static ResourceKey<Level> dimensionKey(String location) {
        return ResourceKey.create(Registries.DIMENSION, new ResourceLocation(location));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || server == null) return;

        if (rollbackAtTick >= 0 && !rollbackInProgress && server.overworld().getGameTime() >= rollbackAtTick) {
            rollbackAtTick = -1;
            CheckpointSlot slot = pendingRollback != null ? pendingRollback : rbd;
            pendingRollback = null;
            boolean rolledBack = performRollback(slot);
            if (slot == rbd) {
                if (isDomainActive()) deactivateDomain("rbd rollback");
            } else {
                domainCasterDeadHardcore = false;
                bumpSubjectInsanity();
            }
            ModNetworking.sendToAll(new DeathFadePacket(false));
            if (!rolledBack) announceRollbackFailure();
            return;
        }

        flushPendingWitchScentDecay();
        flushPendingLightResync();

        if (ueuhPlayAtTick >= 0 && server.overworld().getGameTime() >= ueuhPlayAtTick) {
            ueuhPlayAtTick = -1;
            if (RANDOM.nextFloat() < 0.25f) {
                SoundEvent sound = RANDOM.nextBoolean() ? ModSounds.UEUH.get() : ModSounds.EUHEUH.get();
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    playPersonalSound(player, sound);
                }
            }
        }

        if (rollbackInProgress) return;

        tickDomainEnforcement();

        tickUnseenHands();

        refreshDimensionSettleWindow();

        if (deferredCheckpointReason != null
                && server.overworld().getGameTime() >= suppressAutoCheckpointsUntilTick) {
            String reason = deferredCheckpointReason;
            deferredCheckpointReason = null;
            attemptAutoCheckpoint(reason, 100);
        }

        if (ConfigReturnByDeath.CHECKPOINT_TIMER_ENABLED.get()) {
            tickCounter++;
            if (tickCounter >= nextCheckpointIntervalSeconds * 20) {
                if (!attemptAutoCheckpoint("timer", ConfigReturnByDeath.CHECKPOINT_TIMER_CHANCE.get())) {
                    tickCounter = 0;
                    rollNextCheckpointInterval();
                }
            }
        }
    }

    private boolean attemptAutoCheckpoint(String reason, int chancePercent) {
        if (server == null || rollbackInProgress) return false;
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

        if (!rollChance(chancePercent)) {
            LOGGER.debug("Skipping {} checkpoint (failed {}% roll)", reason, chancePercent);
            return false;
        }

        lastAutoCheckpointGameTick = gameTime;
        createSnapshot(rbd, reason);
        return true;
    }

    private void refreshDimensionSettleWindow() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ResourceKey<Level> current = player.level().dimension();
            ResourceKey<Level> last = lastPlayerDimension.put(player.getUUID(), current);
            if (last != null && !last.equals(current) && DIMENSION_SETTLE_SECONDS > 0) {
                long until = server.overworld().getGameTime() + (long) DIMENSION_SETTLE_SECONDS * 20;
                suppressAutoCheckpointsUntilTick = Math.max(suppressAutoCheckpointsUntilTick, until);
            }
        }
    }

    private void rollNextCheckpointInterval() {
        int base = ConfigReturnByDeath.CHECKPOINT_INTERVAL_SECONDS.get();
        int jitter = ConfigReturnByDeath.CHECKPOINT_INTERVAL_RANDOMNESS_SECONDS.get();
        if (jitter <= 0) {
            nextCheckpointIntervalSeconds = base;
            return;
        }
        int offset = RANDOM.nextInt(jitter * 2 + 1) - jitter;
        nextCheckpointIntervalSeconds = Math.max(5, base + offset);
    }

    private boolean rollChance(int chancePercent) {
        if (chancePercent >= 100) return true;
        if (chancePercent <= 0) return false;
        return RANDOM.nextInt(100) < chancePercent;
    }

    @SubscribeEvent
    public void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (!ConfigReturnByDeath.CHECKPOINT_ON_ADVANCEMENT_ENABLED.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        attemptAutoCheckpoint("advancement", ConfigReturnByDeath.CHECKPOINT_ON_ADVANCEMENT_CHANCE.get());
    }

    @SubscribeEvent
    public void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!ConfigReturnByDeath.CHECKPOINT_ON_SLEEP_ENABLED.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        attemptAutoCheckpoint("sleep", ConfigReturnByDeath.CHECKPOINT_ON_SLEEP_CHANCE.get());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        sendDomainStateTo(player);
        sendActiveUnseenHandsTo(player);
        sendAuthoritiesTo(player);
        sendAbilitySlotsTo(player);
        HahUeuh.LIONS_HEART.restoreOnLogin(player);
        HahUeuh.MATERIAL_PHASE.restoreOnLogin(player);
        HahUeuh.BASE_SHIFT.restoreOnLogin(player);
        HahUeuh.SECOND_SHIFT.restoreOnLogin(player);
        HahUeuh.VISION_OF_DANGER.restoreOnLogin(player);
        HahUeuh.VISION_OF_LIFE.restoreOnLogin(player);

        int cooldownTicksLeft = player.isCreative() ? 0 : domainCooldownRemainingTicks(player.getUUID());
        if (cooldownTicksLeft > 0) {
            ModNetworking.sendToPlayer(player, new AbilityCooldownPacket(
                    HahUeuhAbilities.DOMAIN_VICTIM_ABILITY, cooldownTicksLeft));
            ModNetworking.sendToPlayer(player, new AbilityCooldownPacket(
                    HahUeuhAbilities.DOMAIN_AGGRESSOR_ABILITY, cooldownTicksLeft));
        }

        PlayerSnapshot deferred = pendingPlayerRestores.remove(player.getUUID());
        if (deferred != null) {
            try {
                deferred.restore(player, server);
                LOGGER.info("Applied deferred rollback restore for {} on login",
                        player.getGameProfile().getName());
            } catch (Exception e) {
                LOGGER.error("Failed to apply deferred rollback restore for {}",
                        player.getGameProfile().getName(), e);
            }
        }

        if (isAggressorDomain() && player.getUUID().equals(domainSubjectUuid) && domainInsanityStacks > 0) {
            HahUeuh.INSANITY.applyLevel(player,
                    Math.max(HahUeuh.INSANITY.level(player), domainSubjectInsanityBase + domainInsanityStacks));
        }

        PlayerSnapshot joinSnapshot = null;
        for (CheckpointSlot slot : new CheckpointSlot[]{rbd, domain}) {
            if (slot.snapshot == null || slot.snapshot.playerData().containsKey(player.getUUID())) continue;
            if (joinSnapshot == null) {
                net.noiilive.hahueuh.PlayerLifespan.ensureRolled(player);
                net.noiilive.hahueuh.GateDefectiveState.ensureRolled(player);
                net.noiilive.hahueuh.GateStrain.ensureRolled(player);
                net.noiilive.hahueuh.PlayerStats.ensureRolled(player);
                joinSnapshot = PlayerSnapshot.capture(player);
            }
            slot.snapshot.playerData().put(player.getUUID(), joinSnapshot);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimensionForHands(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendActiveUnseenHandsTo(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            rbdPartialActivationTick.remove(player.getUUID());
            lastPlayerDimension.remove(player.getUUID());
            clearUnseenHand(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (server == null || rollbackInProgress) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        ChunkAccess chunk = event.getChunk();
        if (!(chunk instanceof LevelChunk)) return;

        long key = chunk.getPos().toLong();
        for (CheckpointSlot slot : activeSlots()) {
            Set<Long> tracked = slot.modifiedChunks.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet());
            if (tracked.contains(key)) continue;

            Set<Long> loadedAtCheckpoint = slot.snapshot.loadedChunks().getOrDefault(level.dimension(), Set.of());
            if (!loadedAtCheckpoint.contains(key)) {
                try {
                    captureNewChunkIntoCheckpoint(slot, level, chunk);
                } catch (IOException e) {
                    LOGGER.warn("Failed to capture newly loaded chunk {} into checkpoint {}", chunk.getPos(), slot.dirName, e);
                }
            }
        }
    }

    @SubscribeEvent
    public void onChunkSave(ChunkDataEvent.Save event) {
        if (server == null || rollbackInProgress || internalSaveInProgress) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long key = event.getChunk().getPos().toLong();
        for (CheckpointSlot slot : activeSlots()) {
            slot.modifiedChunks.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(key);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onLivingDeath(LivingDeathEvent event) {
        if (rollbackInProgress) return;
        if (handleAuthorityDeath(event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        if (event.isCanceled()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            handleWitchFactorLossOnDeath(player);
        }
    }

    private void handleWitchFactorLossOnDeath(ServerPlayer player) {
        if (!net.noiilive.hahueuh.ConfigMain.LOSE_WITCH_FACTOR_ON_DEATH.get()) return;
        UUID uuid = player.getUUID();
        ServerLevel level = player.serverLevel();
        Vec3 pos = player.position();

        if (authorityManager.hasWitchFactorSloth(uuid)) {
            authorityManager.setWitchFactorSloth(uuid, false);
            spawnWitchFactor(level, pos, net.noiilive.hahueuh.network.WitchFactorAuthority.SLOTH);
            player.displayClientMessage(Component.translatable("hahueuh.message.witch_factor_lost_on_death",
                    Component.translatable("hahueuh.authority.sloth")).withStyle(ChatFormatting.RED), true);
        }
        if (authorityManager.hasWitchFactorGreed(uuid)) {
            authorityManager.setWitchFactorGreed(uuid, false);
            spawnWitchFactor(level, pos, net.noiilive.hahueuh.network.WitchFactorAuthority.GREED);
            player.displayClientMessage(Component.translatable("hahueuh.message.witch_factor_lost_on_death",
                    Component.translatable("hahueuh.authority.greed")).withStyle(ChatFormatting.RED), true);
        }
    }

    private void spawnWitchFactor(ServerLevel level, Vec3 pos, net.noiilive.hahueuh.network.WitchFactorAuthority authority) {
        net.noiilive.hahueuh.WitchFactorEntity entity = new net.noiilive.hahueuh.WitchFactorEntity(net.noiilive.hahueuh.ModEntities.WITCH_FACTOR.get(), level);
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

    public boolean onEntityWouldSelfDestruct(LivingEntity entity) {
        if (rollbackInProgress) return false;
        return handleAuthorityDeath(entity);
    }

    public boolean isReturnByDeathActive(UUID uuid) {
        return authorityManager.canReturnByDeath(uuid) && rbd.isActive();
    }

    public boolean isDeathCurrentlyProtected(UUID uuid) {
        return isDomainProtected(uuid) || isReturnByDeathActive(uuid);
    }

    public void forceNormalDeath(ServerPlayer player) {
        boolean wasRollingBack = rollbackInProgress;
        rollbackInProgress = true;
        try {
            player.kill();
        } finally {
            rollbackInProgress = wasRollingBack;
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
            if (ConfigReturnByDeath.WITCH_SCENT_ENABLED.get() && !player.isCreative() && !player.isSpectator()) {
                pendingWitchScentBump.put(uuid, nextWitchScentAmplifier(player));
            }
            scheduleRollback(rbd);
            return true;
        }
        return false;
    }

    public void handleReturnByDeathActivate(ServerPlayer player) {
        if (rollbackInProgress || server == null) return;
        UUID uuid = player.getUUID();
        if (!authorityManager.canReturnByDeath(uuid)) return;

        int now = server.getTickCount();
        Integer lastPartial = rbdPartialActivationTick.get(uuid);
        if (lastPartial != null && now - lastPartial <= RBD_PARTIAL_WINDOW_TICKS) {
            rbdPartialActivationTick.remove(uuid);
            broadcastRbdLine(player, "hahueuh.message.rbd_chant_full");
            handleAuthorityDeath(player);
            return;
        }

        rbdPartialActivationTick.put(uuid, now);
        broadcastRbdLine(player, "hahueuh.message.rbd_chant_partial");
        if (ConfigReturnByDeath.WITCH_SCENT_ENABLED.get() && !player.isCreative() && !player.isSpectator()) {
            applyWitchScentLevel(player, nextWitchScentAmplifier(player));
        }
    }

    private static int nextWitchScentAmplifier(ServerPlayer player) {
        MobEffectInstance existing = player.getEffect(ModEffects.WITCH_SCENT.get());
        int maxAmplifier = ConfigReturnByDeath.WITCH_SCENT_MAX_LEVEL.get() - 1;
        return existing != null ? Math.min(existing.getAmplifier() + 1, maxAmplifier) : 0;
    }

    private void broadcastRbdLine(ServerPlayer player, String translationKey) {
        if (server == null) return;
        Component message = Component.literal("<").append(player.getDisplayName()).append("> ")
                .append(Component.translatable(translationKey));
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private void healAndSignal(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            net.noiilive.hahueuh.compat.PlayerReviveCompat.forceRevive(player);
        }
        entity.setHealth(entity.getMaxHealth());
        if (entity instanceof ServerPlayer player) {
            playPersonalSound(player, SoundEvents.WARDEN_HEARTBEAT);
        }
    }

    private void applyWitchScentLevel(ServerPlayer player, int amplifier) {
        player.forceAddEffect(new MobEffectInstance(ModEffects.WITCH_SCENT.get(),
                WITCH_SCENT_DURATION_TICKS, amplifier, false, false, true), null);
    }

    @SubscribeEvent
    public void onMobEffectExpired(MobEffectEvent.Expired event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || instance.getEffect() != ModEffects.WITCH_SCENT.get()) return;
        if (instance.getAmplifier() <= 0) return;
        pendingWitchScentDecay.put(player.getUUID(), instance.getAmplifier() - 1);
    }

    private void flushPendingWitchScentDecay() {
        if (pendingWitchScentDecay.isEmpty()) return;
        for (Map.Entry<UUID, Integer> entry : new HashMap<>(pendingWitchScentDecay).entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null && !player.isCreative() && !player.isSpectator()) {
                applyWitchScentLevel(player, entry.getValue());
            }
        }
        pendingWitchScentDecay.clear();
    }

    private void scheduleRollback(CheckpointSlot slot) {
        if (slot.snapshot == null || rollbackAtTick >= 0) return;
        pendingRollback = slot;
        int fadeInTicks = (int) Math.ceil(DeathFadeState.FADE_SECONDS * 20f) + 2;
        rollbackAtTick = server.overworld().getGameTime() + fadeInTicks;
        ModNetworking.sendToAll(new DeathFadePacket(true));
    }

    private void playPersonalSound(ServerPlayer player, SoundEvent sound) {
        player.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.MASTER,
                player.getX(), player.getY(), player.getZ(), 1.0f, 1.0f, player.getRandom().nextLong()));
    }

    private Set<String> checkpointProtectedNames() {
        Set<String> names = new HashSet<>();
        names.add(rbd.dirName);
        names.add(domain.dirName);
        names.add("session.lock");
        names.addAll(MOD_METADATA_FILES);
        names.add(TIMER_STATE_FILE_NAME);
        return names;
    }

    public void createSnapshot(String reason) {
        createSnapshot(rbd, reason);
    }

    private void createSnapshot(CheckpointSlot slot, String reason) {
        if (server == null) return;
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
                        LOGGER.warn("Failed to close container for {} during checkpoint capture",
                                player.getGameProfile().getName(), e);
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
                    captureRemaining(slothCooldownUntilTick),
                    captureRemaining(quickCooldownUntilTick),
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
                    HahUeuh.VISION_OF_DANGER.captureActive(),
                    HahUeuh.VISION_OF_LIFE.captureCooldownRemaining(),
                    HahUeuh.VISION_OF_LIFE.captureActive(),
                    HahUeuh.FOOTPRINT_TRACKER.captureFootprints(),
                    HahUeuh.LIONS_HEART.captureActive(),
                    HahUeuh.MATERIAL_PHASE.captureActive(),
                    HahUeuh.FINGER_GRANT.captureCooldownRemaining(),
                    HahUeuh.SPELL_CASTING.captureCooldownRemaining());
            saveSnapshotMetadataToDisk(checkpointDir, slot.snapshot);

            slot.resetTracking();

            if (slot == rbd) {
                tickCounter = 0;
                rollNextCheckpointInterval();
            }

            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.info("Checkpoint created in {}ms ({} files tracked) (slot: {}, reason: {})",
                    elapsed, fileTimestamps.size(), slot.dirName, reason);

            if (slot == rbd && ConfigReturnByDeath.SHOW_CHECKPOINT_NOTIFICATION.get()) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    player.sendSystemMessage(Component.translatable("hahueuh.message.checkpoint_saved")
                            .withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to create checkpoint! (reason: {})", reason, e);
        }
    }

    private void announceRollbackFailure() {
        if (server == null) return;
        LOGGER.error("Return by Death did NOT restore the world — see the stack trace above. "
                + "Players were left in their post-death state.");
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.displayClientMessage(Component.translatable("hahueuh.message.rollback_failed")
                    .withStyle(ChatFormatting.RED), false);
        }
    }

    private boolean performRollback(CheckpointSlot slot) {
        WorldSnapshot snapshot = slot.snapshot;
        if (snapshot == null || server == null) return false;
        rollbackInProgress = true;

        LOGGER.info("Rolling back world to checkpoint (slot: {})...", slot.dirName);
        long startTime = System.currentTimeMillis();

        int totalChanged = 0;
        int restoredFiles = 0;
        try {
            long stepStart = startTime;

            Map<ResourceKey<Level>, Set<Long>> chunksToRestore = new HashMap<>();
            for (ServerLevel level : server.getAllLevels()) {
                Set<Long> changed = new HashSet<>(slot.modifiedChunks.getOrDefault(level.dimension(), Set.of()));
                for (ChunkHolder holder : getLoadedChunkHolders(level)) {
                    ChunkAccess chunk = holder.getLastAvailable();
                    if (chunk != null && chunk.isUnsaved()) {
                        changed.add(chunk.getPos().toLong());
                    }
                }
                chunksToRestore.put(level.dimension(), changed);
            }
            totalChanged = chunksToRestore.values().stream().mapToInt(Set::size).sum();
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
            restoredFiles = restoreChangedFiles(worldDir, checkpointDir, protectedNames, snapshot.fileTimestamps());
            stepStart = logStepTime("restoreChangedFiles (" + restoredFiles + " files)", stepStart);

            HahUeuh.POCKET_DIMENSION.reloadFromDisk();
            HahUeuh.TELEPORTATION.reloadFromDisk();

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

            HahUeuh.POCKET_DIMENSION.reconcileAfterRollback(server);
            HahUeuh.DOOR_CROSSING.reconcileAfterRollback();

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.containerMenu != player.inventoryMenu) {
                    try {
                        player.closeContainer();
                    } catch (Exception e) {
                        LOGGER.warn("Failed to close container for {} before rollback restore",
                                player.getGameProfile().getName(), e);
                    }
                }
                HahUeuh.SPELL_CASTING.cancelCastSilently(player);
                PlayerSnapshot ps = snapshot.playerData().get(player.getUUID());
                if (ps != null) {
                    try {
                        ps.restore(player, server);
                    } catch (Exception e) {
                        LOGGER.error("Failed to restore player state for {} during rollback",
                                player.getGameProfile().getName(), e);
                    }
                } else {
                    LOGGER.warn("No snapshot data found for {} — player state was NOT restored on rollback",
                            player.getGameProfile().getName());
                }
            }
            stepStart = logStepTime("restore online players", stepStart);

            snapshot.playerData().forEach((uuid, ps) -> {
                if (server.getPlayerList().getPlayer(uuid) == null) pendingPlayerRestores.put(uuid, ps);
            });
            if (!pendingPlayerRestores.isEmpty()) {
                LOGGER.info("Deferred player restore queued for {} offline player(s)", pendingPlayerRestores.size());
            }

            if (!pendingWitchScentBump.isEmpty()) {
                for (Map.Entry<UUID, Integer> entry : pendingWitchScentBump.entrySet()) {
                    ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                    if (player != null && !player.isCreative() && !player.isSpectator()) {
                        applyWitchScentLevel(player, entry.getValue());
                    }
                }
                pendingWitchScentBump.clear();
            }

            authorityManager.load(server);
            abilitySlotsManager.load(server);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sendAuthoritiesTo(player);
                sendAbilitySlotsTo(player);
            }
            HahUeuh.LIONS_HEART.restoreActiveOnRollback(snapshot.lionsHeartActive());
            HahUeuh.MATERIAL_PHASE.restoreActiveOnRollback(snapshot.materialPhaseActive());
            HahUeuh.LITTLE_KING.refreshAllOnRollback();
            HahUeuh.ALLY_TRACKER.refreshAllOnRollback();
            HahUeuh.FINGER_GRANT.refreshAllOnRollback();
            HahUeuh.OL_SHAMAK.reconcileAfterRollback(server);
            restoreCooldowns(domainCooldownUntilTick, snapshot.domainCooldownRemaining(),
                    HahUeuhAbilities.DOMAIN_VICTIM_ABILITY, HahUeuhAbilities.DOMAIN_AGGRESSOR_ABILITY);
            restoreCooldowns(slothCooldownUntilTick, snapshot.slothCooldownRemaining(),
                    HahUeuhAbilities.SLOTH_COOLDOWN_KEY);
            restoreCooldowns(quickCooldownUntilTick, snapshot.quickActionCooldownRemaining(),
                    HahUeuhAbilities.QUICK_ACTION_COOLDOWN_KEY);
            HahUeuh.LIONS_HEART.restoreCooldownRemaining(snapshot.lionsHeartCooldownRemaining());
            HahUeuh.LITTLE_KING.restoreCooldownRemaining(snapshot.littleKingCooldownRemaining());
            HahUeuh.MATERIAL_PHASE.restoreCooldownRemaining(snapshot.materialPhaseCooldownRemaining());
            HahUeuh.OBJECT_FREEZE.restoreCooldownRemaining(snapshot.objectFreezeCooldownRemaining());
            HahUeuh.ALLY_TRACKER.restoreCooldownRemaining(snapshot.allyTrackerCooldownRemaining());
            HahUeuh.BASE_SHIFT.restoreCooldownRemaining(snapshot.baseShiftCooldownRemaining());
            HahUeuh.SECOND_SHIFT.restoreCooldownRemaining(snapshot.secondShiftCooldownRemaining());
            HahUeuh.FINGER_GRANT.restoreCooldownRemaining(snapshot.fingerGrantCooldownRemaining());
            HahUeuh.SPELL_CASTING.restoreCooldownRemaining(snapshot.spellCooldownRemaining());
            HahUeuh.BOOK_OF_WISDOM.restoreCooldownRemaining(snapshot.bookOfWisdomCooldownRemaining());
            HahUeuh.BOOK_OF_WISDOM.restoreSummonedState(snapshot.bookOfWisdomSummoned());
            HahUeuh.MENTAL_OVERLOAD.restoreCooldownRemaining(snapshot.mentalOverloadCooldownRemaining());
            HahUeuh.VISION_OF_DANGER.restoreActiveOnRollback(snapshot.visionOfDangerActive());
            HahUeuh.VISION_OF_DANGER.restoreCooldownRemaining(snapshot.visionOfDangerCooldownRemaining());
            HahUeuh.VISION_OF_LIFE.restoreActiveOnRollback(snapshot.visionOfLifeActive());
            HahUeuh.VISION_OF_LIFE.restoreCooldownRemaining(snapshot.visionOfLifeCooldownRemaining());
            HahUeuh.FOOTPRINT_TRACKER.restoreFootprints(snapshot.footprints());
            logStepTime("reload authorities + ability slots", stepStart);

            if (net.noiilive.hahueuh.compat.CreateRollbackCompat.isPresent()) {
                net.noiilive.hahueuh.compat.CreateRollbackCompat.reloadAndResync(server);
            }

            ServerLevel overworld = server.overworld();
            overworld.setDayTime(snapshot.dayTime());
            ServerLevelData levelData = (ServerLevelData) overworld.getLevelData();
            levelData.setClearWeatherTime(snapshot.clearWeatherTime());
            levelData.setRainTime(snapshot.rainTime());
            levelData.setThunderTime(snapshot.thunderTime());
            levelData.setRaining(snapshot.raining());
            levelData.setThundering(snapshot.thundering());

            long gameTimeBefore = overworld.getGameTime();
            levelData.setGameTime(snapshot.gameTime());
            long gameTimeShift = snapshot.gameTime() - gameTimeBefore;
            if (gameTimeShift != 0) {
                if (suppressAutoCheckpointsUntilTick >= 0) suppressAutoCheckpointsUntilTick += gameTimeShift;
                if (lastAutoCheckpointGameTick >= 0) lastAutoCheckpointGameTick += gameTimeShift;
            }

            slot.modifiedChunks.clear();
            targetingSuppressUntilTick = overworld.getGameTime() + 20;
            if (slot == rbd) {
                tickCounter = 0;
                rollNextCheckpointInterval();
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    playPersonalSound(player, ModSounds.HAHH.get());
                }
                ueuhPlayAtTick = overworld.getGameTime() + 20;
            }

            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.info("World rolled back in {}ms ({} changed chunks, {} files restored)",
                    elapsed, totalChanged, restoredFiles);

            return true;

        } catch (Exception e) {
            LOGGER.error("Failed to roll back world!", e);
            return false;
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

    private void captureNewChunkIntoCheckpoint(CheckpointSlot slot, ServerLevel level, ChunkAccess chunk)
            throws IOException {
        if (slot.snapshot == null) return;
        ChunkPos pos = chunk.getPos();
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        Path checkpointDir = slot.snapshot.checkpointDir();
        Path liveRegionDir = getDimensionPath(level).resolve("region");
        Path checkpointRegionDir = checkpointDir.resolve(worldDir.relativize(liveRegionDir));

        Files.createDirectories(checkpointRegionDir);
        Path checkpointRegionFile = checkpointRegionDir.resolve(
                "r." + pos.getRegionX() + "." + pos.getRegionZ() + ".mca");

        boolean alreadyPresent;
        try (RegionFile regionFile = new RegionFile(checkpointRegionFile, checkpointRegionDir, false)) {
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

    private void restoreChangedChunks(ServerLevel level, Set<Long> changedChunks, Path checkpointDir)
            throws IOException {
        if (changedChunks.isEmpty()) return;

        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        Path liveRegionDir = getDimensionPath(level).resolve("region");
        Path checkpointRegionDir = checkpointDir.resolve(worldDir.relativize(liveRegionDir));

        Map<Long, List<ChunkPos>> byRegion = new HashMap<>();
        for (long chunkLong : changedChunks) {
            ChunkPos pos = new ChunkPos(chunkLong);
            byRegion.computeIfAbsent(ChunkPos.asLong(pos.getRegionX(), pos.getRegionZ()),
                    k -> new ArrayList<>()).add(pos);
        }

        Map<Long, LevelChunk> loadedByPos = new HashMap<>();
        for (ChunkHolder holder : getLoadedChunkHolders(level)) {
            LevelChunk c = getLoadedChunk(holder);
            if (c != null && changedChunks.contains(c.getPos().toLong())) {
                loadedByPos.put(c.getPos().toLong(), c);
            }
        }

        List<ChunkPos> missingPositions = new ArrayList<>();
        int disk = 0, missing = 0, skippedProto = 0;
        for (List<ChunkPos> chunksInRegion : byRegion.values()) {
            ChunkPos any = chunksInRegion.get(0);
            String fileName = "r." + any.getRegionX() + "." + any.getRegionZ() + ".mca";
            Path ckptFile = checkpointRegionDir.resolve(fileName);
            if (!Files.exists(ckptFile)) { missing += chunksInRegion.size(); missingPositions.addAll(chunksInRegion); continue; }

            Files.createDirectories(liveRegionDir);
            try (RegionFile ckptRegion = new RegionFile(ckptFile, checkpointRegionDir, false);
                 RegionFile liveRegion = new RegionFile(liveRegionDir.resolve(fileName), liveRegionDir, false)) {
                for (ChunkPos pos : chunksInRegion) {
                    if (!ckptRegion.hasChunk(pos)) { missing++; missingPositions.add(pos); continue; }
                    CompoundTag nbt;
                    try (DataInputStream in = ckptRegion.getChunkDataInputStream(pos)) {
                        if (in == null) { missing++; missingPositions.add(pos); continue; }
                        nbt = NbtIo.read(in);
                    }
                    if (nbt == null) { missing++; missingPositions.add(pos); continue; }
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
                Path ckptFile = checkpointRegionDir.resolve(
                        "r." + any.getRegionX() + "." + any.getRegionZ() + ".mca");
                if (!Files.exists(ckptFile)) continue;
                try (RegionFile ckptRegion = new RegionFile(ckptFile, checkpointRegionDir, false)) {
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

        if (level.dimension().equals(net.noiilive.hahueuh.PocketDimension.POCKET_LEVEL) && !missingPositions.isEmpty()) {
            int wiped = 0;
            for (ChunkPos pos : missingPositions) {
                LevelChunk loaded = loadedByPos.get(pos.toLong());
                if (loaded == null) continue;
                clearChunkInMemory(level, loaded);
                wiped++;
            }
            LOGGER.debug("Cleared {} pocket chunks with no checkpoint data", wiped);
        }

        LOGGER.debug("Restored chunks in {}: {} on disk, {} in memory, {} left as-is, {} skipped (proto), {} regions, {} failed",
                level.dimension().location(), disk, mem, missing, skippedProto, byRegion.size(), failed);
    }

    private boolean isFullyGeneratedChunk(CompoundTag chunkNbt) {
        return ChunkSerializer.getChunkTypeFromTag(chunkNbt) == ChunkStatus.ChunkType.LEVELCHUNK;
    }

    private void evictPoiForChangedChunks(ServerLevel level, Set<Long> changedChunks) {
        if (changedChunks.isEmpty()) return;
        PoiManager poiManager = level.getChunkSource().getPoiManager();
        int minSection = level.getMinSection();
        int maxSection = level.getMaxSection();

        try {
            SectionStorageAccessor accessor = (SectionStorageAccessor) poiManager;
            var storage = accessor.hahueuh$getStorage();
            var dirty = accessor.hahueuh$getDirty();

            for (long chunkLong : changedChunks) {
                ChunkPos pos = new ChunkPos(chunkLong);
                for (int sectionY = minSection; sectionY < maxSection; sectionY++) {
                    long sectionKey = SectionPos.of(pos, sectionY).asLong();
                    storage.remove(sectionKey);
                    dirty.rem(sectionKey);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to evict POI sections for {} during rollback", level.dimension().location(), e);
        }
    }

    private void clearUnsavedFlags(ServerLevel level) {
        int cleared = 0;
        for (ChunkHolder holder : getLoadedChunkHolders(level)) {
            ChunkAccess chunk = holder.getLastAvailable();
            if (chunk != null && chunk.isUnsaved()) {
                chunk.setUnsaved(false);
                cleared++;
            }
        }
        LOGGER.debug("Cleared unsaved flag on {} chunks in {}", cleared, level.dimension().location());
    }

    private void resetSavedDataCache(ServerLevel level) {
        try {
            DimensionDataStorage storage = level.getDataStorage();
            ((DimensionDataStorageAccessor) storage).hahueuh$getCache().clear();
        } catch (Exception e) {
            LOGGER.warn("Failed to reset SavedData cache for {} during rollback", level.dimension().location(), e);
        }
    }

    private void forceFlushSavedData(ServerLevel level) {
        try {
            DimensionDataStorage storage = level.getDataStorage();
            for (SavedData savedData : ((DimensionDataStorageAccessor) storage).hahueuh$getCache().values()) {
                if (savedData != null && isFlushableSavedData(savedData)) {
                    savedData.setDirty();
                }
            }
            storage.save();
        } catch (Exception e) {
            LOGGER.warn("Failed to force-flush SavedData for {} during checkpoint creation",
                    level.dimension().location(), e);
        } finally {
            DummySavedData.DUMMY.setDirty(false);
        }
    }

    private static boolean isFlushableSavedData(SavedData savedData) {
        if (savedData == DummySavedData.DUMMY) return false;
        try {
            return savedData.save(new CompoundTag()) != null;
        } catch (Exception e) {
            return false;
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

    private Iterable<ChunkHolder> getLoadedChunkHolders(ServerLevel level) {
        try {
            ChunkMapAccessor chunkMap = (ChunkMapAccessor) level.getChunkSource().chunkMap;
            return new ArrayList<>(chunkMap.hahueuh$getVisibleChunkMap().values());
        } catch (Exception e) {
            LOGGER.warn("Failed to access loaded chunks for {}", level.dimension().location(), e);
            return List.of();
        }
    }

    private LevelChunk getLoadedChunk(ChunkHolder holder) {
        return holder.getLastAvailable() instanceof LevelChunk levelChunk ? levelChunk : null;
    }

    private void clearChunkInMemory(ServerLevel level, LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        var lightEngine = level.getChunkSource().getLightEngine();
        LevelChunkSection[] sections = chunk.getSections();

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection old = sections[i];
            if (old.hasOnlyAir()) continue;
            @SuppressWarnings("unchecked")
            PalettedContainer<Holder<Biome>> biomes = (PalettedContainer<Holder<Biome>>) old.getBiomes();
            sections[i] = new LevelChunkSection(new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY,
                    Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES), biomes);
            lightEngine.updateSectionStatus(SectionPos.of(pos, level.getSectionYFromSectionIndex(i)), true);
        }

        for (BlockPos bePos : new HashSet<>(chunk.getBlockEntities().keySet())) {
            level.removeBlockEntity(bePos);
        }

        Heightmap.primeHeightmaps(chunk, EnumSet.of(
                Heightmap.Types.MOTION_BLOCKING,
                Heightmap.Types.WORLD_SURFACE,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Heightmap.Types.OCEAN_FLOOR));

        chunk.setUnsaved(true);
        sendToChunkViewers(level, pos, new ClientboundLevelChunkWithLightPacket(
                chunk, level.getLightEngine(), null, null));
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

                if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) continue;

                if (!sectionTag.contains("block_states", Tag.TAG_COMPOUND)) continue;

                var parsed = BLOCK_STATE_CODEC.parse(NbtOps.INSTANCE,
                        sectionTag.getCompound("block_states")).result();
                if (parsed.isEmpty()) {
                    LOGGER.warn("Parse failed for section Y={} of chunk {} in {} — leaving existing section",
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
                BlockPos bePos = new BlockPos(beNbt.getInt("x"), beNbt.getInt("y"), beNbt.getInt("z"));
                BlockState state = chunk.getBlockState(bePos);

                BlockEntity be = BlockEntity.loadStatic(bePos, state, beNbt);
                if (be != null) {
                    level.setBlockEntity(be);
                }
            }
        }

        Heightmap.primeHeightmaps(chunk, EnumSet.of(
                Heightmap.Types.MOTION_BLOCKING,
                Heightmap.Types.WORLD_SURFACE,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Heightmap.Types.OCEAN_FLOOR));

        restoreChunkCapabilities(chunk, chunkNbt);

        boolean relit = !lightRechecks.isEmpty();
        if (relit) {
            for (BlockPos changed : lightRechecks) {
                lightEngine.checkBlock(changed);
            }
        }

        sendToChunkViewers(level, pos, new ClientboundLevelChunkWithLightPacket(
                chunk, level.getLightEngine(), null, null));

        if (relit) {
            pendingLightResync.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.toLong());
        }
    }

    private void flushPendingLightResync() {
        if (pendingLightResync.isEmpty()) return;
        if (lightResyncAtTick < 0) {
            lightResyncAtTick = server.overworld().getGameTime() + LIGHT_RESYNC_DELAY_TICKS;
            return;
        }
        if (server.overworld().getGameTime() < lightResyncAtTick) return;

        lightResyncAtTick = -1;
        for (Map.Entry<ResourceKey<Level>, Set<Long>> entry : pendingLightResync.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) continue;
            for (long chunkLong : entry.getValue()) {
                ChunkPos pos = new ChunkPos(chunkLong);
                LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
                if (chunk == null) continue;
                sendToChunkViewers(level, pos, new ClientboundLevelChunkWithLightPacket(
                        chunk, level.getLightEngine(), null, null));
                sendToChunkViewers(level, pos,
                        new ClientboundLightUpdatePacket(pos, level.getLightEngine(), null, null));
            }
        }
        pendingLightResync.clear();
    }

    private void restoreChunkCapabilities(LevelChunk chunk, CompoundTag chunkNbt) {
        CompoundTag caps = chunkNbt.contains("ForgeCaps", Tag.TAG_COMPOUND)
                ? chunkNbt.getCompound("ForgeCaps")
                : new CompoundTag();
        try {
            chunk.readCapsFromNBT(caps);
        } catch (Exception e) {
            LOGGER.warn("Failed to restore chunk capabilities for {} on rollback", chunk.getPos(), e);
        }
    }

    private void sendToChunkViewers(ServerLevel level, ChunkPos pos, Packet<?> packet) {
        int viewDistance = level.getServer().getPlayerList().getViewDistance() + 1;
        for (ServerPlayer player : level.players()) {
            SectionPos playerSection = player.getLastSectionPos();
            int dist = Math.max(Math.abs(playerSection.x() - pos.x), Math.abs(playerSection.z() - pos.z));
            if (dist <= viewDistance) {
                player.connection.send(packet);
            }
        }
    }

    private void restoreEntitiesForLevel(ServerLevel level, WorldSnapshot snapshot) {
        List<CompoundTag> savedEntities = snapshot.entityData().get(level.dimension());
        if (savedEntities == null) return;
        if (savedEntities.isEmpty() && level.dimension() == Level.END) return;

        Set<Long> loadedAtCheckpoint = snapshot.loadedChunks().getOrDefault(level.dimension(), Set.of());

        Map<UUID, CompoundTag> snapshotByUuid = new HashMap<>();
        for (CompoundTag nbt : savedEntities) {
            if (nbt.hasUUID("UUID")) snapshotByUuid.put(nbt.getUUID("UUID"), nbt);
        }

        List<Entity> current = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (!(e instanceof Player)) current.add(e);
        }

        boolean createCompat = net.noiilive.hahueuh.compat.CreateRollbackCompat.isPresent();

        int reverted = 0, removed = 0, spawned = 0, carriages = 0;
        Set<UUID> handled = new HashSet<>();
        for (Entity e : current) {
            UUID id = e.getUUID();
            if (createCompat && isCreateCarriage(e)) {
                e.discard();
                handled.add(id);
                carriages++;
                continue;
            }
            CompoundTag snap = snapshotByUuid.get(id);
            if (snap != null) {
                if (e instanceof LivingEntity dying && dying.deathTime > 0) {
                    e.discard();
                    continue;
                }
                try {
                    e.load(snap);
                    resetTransientAiState(e);
                    reverted++;
                    handled.add(id);
                } catch (Exception ex) {
                    LOGGER.warn("Failed to revert entity {} in {}; discarding and re-adding from snapshot",
                            id, level.dimension().location(), ex);
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
            if (createCompat && CREATE_CARRIAGE_ENTITY_ID.equals(entry.getValue().getString("id"))) continue;
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

        if (reverted + removed + spawned + carriages > 0) {
            LOGGER.debug("Entities in {}: {} reverted, {} removed (post-checkpoint), {} re-added, {} carriages left to Create",
                    level.dimension().location(), reverted, removed, spawned, carriages);
        }
    }

    private static final String CREATE_CARRIAGE_ENTITY_ID = "create:carriage_contraption";

    private static boolean isCreateCarriage(Entity e) {
        net.minecraft.resources.ResourceLocation key =
                net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        return key != null && CREATE_CARRIAGE_ENTITY_ID.equals(key.toString());
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
            closeEntityStorage(level);
            closePoiStorage(level);
        }
    }

    private void closeChunkRegionStorage(ServerLevel level) {
        try {
            Object chunkMap = level.getChunkSource().chunkMap;
            closeRegionStoragesRecursive(chunkMap, new HashSet<>(), 0);
        } catch (Exception e) {
            LOGGER.warn("Failed to close chunk region storage for {}", level.dimension().location(), e);
        }
    }

    private void closeEntityStorage(ServerLevel level) {
        try {
            closeRegionStoragesRecursive(
                    ((ServerLevelEntityManagerAccessor) level).hahueuh$getEntityManager(), new HashSet<>(), 0);
        } catch (Exception e) {
            LOGGER.warn("Failed to close entity storage for {}", level.dimension().location(), e);
        }
    }

    private void closePoiStorage(ServerLevel level) {
        try {
            closeRegionStoragesRecursive(level.getChunkSource().getPoiManager(), new HashSet<>(), 0);
        } catch (Exception e) {
            LOGGER.warn("Failed to close poi storage for {}", level.dimension().location(), e);
        }
    }

    private void closeRegionStoragesRecursive(Object obj, Set<Object> visited, int depth) {
        if (obj == null || depth > 4) return;
        if (!visited.add(obj)) return;

        if (obj instanceof RegionFileStorage storage) {
            closeAndClearStorage(storage);
            return;
        }

        for (Class<?> clazz = obj.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                if (!isStorageRelatedType(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    closeRegionStoragesRecursive(field.get(obj), visited, depth + 1);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private boolean isStorageRelatedType(Class<?> type) {
        if (type.isPrimitive()) return false;
        String name = type.getName();
        return RegionFileStorage.class.isAssignableFrom(type)
                || name.contains("IOWorker")
                || name.contains("Storage")
                || name.contains("ChunkMap")
                || name.contains("PoiManager")
                || name.contains("EntityManager")
                || name.contains("EntityStorage");
    }

    private void closeAndClearStorage(RegionFileStorage storage) {
        try {
            storage.close();
        } catch (Exception e) {
            LOGGER.debug("Region storage close reported: {}", e.getMessage());
        }
        try {
            ((RegionFileStorageAccessor) (Object) storage).hahueuh$getRegionCache().clear();
        } catch (Exception e) {
            LOGGER.debug("Could not clear region cache: {}", e.getMessage());
        }
    }

    private void copyDirectoryWithTimestamps(Path source, Path target, Set<String> excludes,
                                             Map<Path, Long> timestamps) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(source) && excludes.contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (excludes.contains(file.getFileName().toString())) return FileVisitResult.CONTINUE;
                Path relative = source.relativize(file);
                Path destination = target.resolve(relative);
                try {
                    Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES);
                    timestamps.put(relative, Files.getLastModifiedTime(file).toMillis());
                } catch (IOException e) {
                    LOGGER.warn("Failed to copy {} into checkpoint", file, e);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                LOGGER.warn("Skipping unreadable file during checkpoint copy: {}", file);
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
                LOGGER.warn("Failed to delete during checkpoint cleanup: {}", file);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
