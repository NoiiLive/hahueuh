package net.noiilive.hahueuh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.noiilive.hahueuh.network.AbilityCooldownPayload;
import net.noiilive.hahueuh.network.AllyDataPayload;
import net.noiilive.hahueuh.network.AllyType;
import net.noiilive.hahueuh.network.GreedVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class AllyTracker {
    private static final String FILE_NAME = "hahueuh_ally_tracker.json";
    private static final double MARK_REACH = 12.0;
    private static final TicketType<ChunkPos> ALLY_BURDEN_TICKET =
            TicketType.create("hahueuh_ally_burden", Comparator.comparingLong(ChunkPos::toLong), 200);
    private static final int BURDEN_TICKET_RADIUS = 2;
    private static final int PENDING_BURDEN_TIMEOUT_TICKS = 100;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type STORE_TYPE = new TypeToken<Map<String, StoredKing>>() {}.getType();

    private final Map<UUID, KingData> kings = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> cooldownUntilTick = new ConcurrentHashMap<>();
    private final List<PendingBurden> pendingBurdens = new ArrayList<>();
    private MinecraftServer server;
    private Path filePath;

    private static final class PendingBurden {
        final UUID mobUuid;
        final ResourceKey<Level> dimension;
        final ChunkPos chunk;
        final Consumer<LivingEntity> action;
        int ticksRemaining = PENDING_BURDEN_TIMEOUT_TICKS;

        PendingBurden(UUID mobUuid, ResourceKey<Level> dimension, ChunkPos chunk, Consumer<LivingEntity> action) {
            this.mobUuid = mobUuid;
            this.dimension = dimension;
            this.chunk = chunk;
            this.action = action;
        }
    }

    private static final class Ally {
        final UUID uuid;
        AllyType type;
        String name;
        double weight;

        boolean hasData;
        double lastX, lastY, lastZ;
        float lastHealth, lastMaxHealth;
        ResourceKey<Level> lastDimension;
        List<AllyDataPayload.Effect> lastEffects = List.of();

        Ally(UUID uuid, AllyType type, String name, double weight) {
            this.uuid = uuid;
            this.type = type;
            this.name = name;
            this.weight = weight;
        }
    }

    private static final class KingData {
        final List<Ally> allies = new ArrayList<>();
        double selfWeight = 100.0;
    }

    private static final class StoredAlly {
        String uuid;
        String type;
        String name;
        double weight;
        boolean hasData;
        double lastX, lastY, lastZ;
        float lastHealth, lastMaxHealth;
        String lastDimension;
    }

    private static final class StoredKing {
        double selfWeight = 100.0;
        List<StoredAlly> allies = new ArrayList<>();
    }

    public void activate(ServerPlayer king) {
        if (server == null) return;
        UUID uuid = king.getUUID();

        if (!HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().canUseGreed(uuid)
                || HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().getGreedVariant(uuid) != GreedVariant.CORLEONIS) {
            king.displayClientMessage(Component.translatable("hahueuh.message.no_greed_authority")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        LivingEntity target = raycastTarget(king);
        if (target == null) {
            int remainingCooldown = king.isCreative() ? 0 : cooldownRemainingTicks(uuid);
            if (remainingCooldown > 0) {
                int seconds = (int) Math.ceil(remainingCooldown / 20.0);
                king.displayClientMessage(Component.translatable("hahueuh.message.ally_tracker_cooldown", seconds)
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
            sendSnapshot(king, true);
            startCooldown(king);
            return;
        }

        if (isAlly(uuid, target.getUUID())) {
            king.displayClientMessage(Component.translatable("hahueuh.message.ally_already", target.getName())
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        int max = ConfigGreed.ALLY_TRACKER_MAX_ALLIES.getAsInt();
        if (allyCount(uuid) >= max) {
            king.displayClientMessage(Component.translatable("hahueuh.message.ally_list_full", max)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (target instanceof ServerPlayer targetPlayer) {
            if (HahUeuh.PLAYER_ALLIES.areAllies(uuid, targetPlayer.getUUID())) {
                addAlly(uuid, targetPlayer.getUUID(), AllyType.PLAYER, targetPlayer.getName().getString());
                king.level().playSound(null, king.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.PLAYERS, 0.6f, 1.2f);
                king.displayClientMessage(Component.translatable("hahueuh.message.ally_added",
                        targetPlayer.getName(), allyCount(uuid)).withStyle(ChatFormatting.GOLD), true);
            } else {
                HahUeuh.PLAYER_ALLIES.requestAlly(king, targetPlayer.getGameProfile());
            }
        } else {
            AllyType type = isOwnedBy(target, uuid) ? AllyType.TAMED : AllyType.PASSIVE;
            addAlly(uuid, target.getUUID(), type, target.getName().getString());
            seedLastKnown(uuid, target);
            king.level().playSound(null, king.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.6f, 1.2f);
            king.displayClientMessage(Component.translatable("hahueuh.message.ally_added",
                    target.getName(), allyCount(uuid)).withStyle(ChatFormatting.GOLD), true);
        }
    }

    private LivingEntity raycastTarget(ServerPlayer king) {
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(king,
                e -> e != king && e.isAlive() && !e.isSpectator() && isMarkable(king.getUUID(), e), MARK_REACH);
        if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private static boolean isMarkable(UUID king, Entity e) {
        if (!(e instanceof LivingEntity)) return false;
        if (e instanceof Player) return true;
        if (isOwnedBy(e, king)) return true;
        return !(e instanceof Enemy);
    }

    private static boolean isOwnedBy(Entity e, UUID king) {
        return e instanceof OwnableEntity ownable && king.equals(ownable.getOwnerUUID());
    }

    private void addAlly(UUID king, UUID target, AllyType type, String name) {
        KingData data = kings.computeIfAbsent(king, k -> new KingData());
        for (Ally a : data.allies) {
            if (a.uuid.equals(target)) return;
        }
        data.allies.add(new Ally(target, type, name, 0.0));
        renormalize(data);
        save();
        resendIfOnline(king);
    }

    private void renormalize(KingData data) {
        int n = data.allies.size() + 1;
        double[] w = new double[n];
        w[0] = data.selfWeight;
        for (int i = 0; i < data.allies.size(); i++) w[i + 1] = data.allies.get(i).weight;
        double[] norm = BurdenMath.normalize(w);
        data.selfWeight = norm[0];
        for (int i = 0; i < data.allies.size(); i++) data.allies.get(i).weight = norm[i + 1];
    }

    public void updateBurden(ServerPlayer king, float selfWeight, Map<UUID, Float> allyWeights) {
        KingData data = kings.get(king.getUUID());
        if (data == null) return;
        int n = data.allies.size() + 1;
        double[] w = new double[n];
        w[0] = Math.max(0.0, selfWeight);
        for (int i = 0; i < data.allies.size(); i++) {
            Ally a = data.allies.get(i);
            Float fw = allyWeights.get(a.uuid);
            w[i + 1] = fw != null ? Math.max(0.0, fw) : a.weight;
        }
        double[] norm = BurdenMath.normalize(w);
        data.selfWeight = norm[0];
        for (int i = 0; i < data.allies.size(); i++) data.allies.get(i).weight = norm[i + 1];
        save();
    }

    public Map<UUID, Double> effectiveWeights(UUID king) {
        Map<UUID, Double> result = new LinkedHashMap<>();
        KingData data = kings.get(king);
        if (data == null) {
            result.put(king, 100.0);
            return result;
        }

        List<UUID> ids = new ArrayList<>();
        List<Double> raw = new ArrayList<>();
        if (server != null && server.getPlayerList().getPlayer(king) != null) {
            ids.add(king);
            raw.add(data.selfWeight);
        }
        for (Ally a : data.allies) {
            if (a.type == AllyType.PLAYER) {
                if (server == null || server.getPlayerList().getPlayer(a.uuid) == null) continue;
            }
            ids.add(a.uuid);
            raw.add(a.weight);
        }

        double[] weights = new double[raw.size()];
        for (int i = 0; i < raw.size(); i++) weights[i] = raw.get(i);
        double[] normalized = BurdenMath.normalize(weights);
        for (int i = 0; i < ids.size(); i++) result.put(ids.get(i), normalized[i]);
        return result;
    }

    public boolean isAlly(UUID king, UUID target) {
        KingData data = kings.get(king);
        if (data == null) return false;
        for (Ally a : data.allies) {
            if (a.uuid.equals(target)) return true;
        }
        return false;
    }

    public List<LivingEntity> getLiveAllies(UUID king) {
        KingData data = kings.get(king);
        if (data == null) return List.of();
        List<LivingEntity> result = new ArrayList<>();
        for (Ally a : data.allies) {
            LivingEntity living = findLiving(a);
            if (living != null && living.isAlive()) result.add(living);
        }
        return result;
    }

    private int allyCount(UUID king) {
        KingData data = kings.get(king);
        return data == null ? 0 : data.allies.size();
    }

    public void sendRefresh(ServerPlayer king) {
        if (server == null) return;
        sendSnapshot(king, false);
    }

    private void sendSnapshot(ServerPlayer king, boolean open) {
        PacketDistributor.sendToPlayer(king, buildSnapshot(king, open));
    }

    private void resendIfOnline(UUID kingUuid) {
        ServerPlayer king = server == null ? null : server.getPlayerList().getPlayer(kingUuid);
        if (king != null) sendSnapshot(king, false);
    }

    private AllyDataPayload buildSnapshot(ServerPlayer king, boolean open) {
        KingData data = kings.get(king.getUUID());
        List<AllyDataPayload.Ally> out = new ArrayList<>();
        float selfWeight = data != null ? (float) data.selfWeight : 100.0f;
        ResourceKey<Level> kingDim = king.level().dimension();
        double kingX = king.getX();
        double kingZ = king.getZ();

        if (data != null) {
            for (Ally a : data.allies) {
                LivingEntity living = findLiving(a);
                boolean online = living != null && living.isAlive();
                if (online) {
                    captureLive(a, living);
                }
                boolean sameDim = a.hasData && kingDim.equals(a.lastDimension);
                double dx = sameDim ? a.lastX - kingX : 0.0;
                double dz = sameDim ? a.lastZ - kingZ : 0.0;
                out.add(new AllyDataPayload.Ally(a.uuid, a.name, a.type.ordinal(), online, a.hasData,
                        a.lastHealth, a.lastMaxHealth, a.lastX, a.lastY, a.lastZ,
                        dx, dz, sameDim, (float) a.weight, a.lastEffects));
            }
        }

        return new AllyDataPayload(open, selfWeight, king.getHealth(), king.getMaxHealth(),
                kingX, king.getY(), kingZ, effectsOf(king), out);
    }

    private LivingEntity findLiving(Ally a) {
        if (a.type == AllyType.PLAYER) {
            return server.getPlayerList().getPlayer(a.uuid);
        }
        Entity e = findEntity(a.uuid);
        return e instanceof LivingEntity living ? living : null;
    }

    private void captureLive(Ally a, LivingEntity living) {
        a.name = living.getName().getString();
        a.hasData = true;
        a.lastX = living.getX();
        a.lastY = living.getY();
        a.lastZ = living.getZ();
        a.lastHealth = living.getHealth();
        a.lastMaxHealth = living.getMaxHealth();
        a.lastDimension = living.level().dimension();
        a.lastEffects = effectsOf(living);
    }

    private void seedLastKnown(UUID king, LivingEntity entity) {
        KingData data = kings.get(king);
        if (data == null) return;
        for (Ally a : data.allies) {
            if (a.uuid.equals(entity.getUUID())) {
                captureLive(a, entity);
                return;
            }
        }
    }

    private void refreshMobSnapshot(LivingEntity mob) {
        UUID id = mob.getUUID();
        for (Map.Entry<UUID, KingData> entry : kings.entrySet()) {
            boolean found = false;
            for (Ally a : entry.getValue().allies) {
                if (a.uuid.equals(id)) {
                    captureLive(a, mob);
                    found = true;
                }
            }
            if (found) resendIfOnline(entry.getKey());
        }
    }

    public void applyToMob(UUID mobUuid, Consumer<LivingEntity> action) {
        if (server == null) return;
        Entity loaded = findEntity(mobUuid);
        if (loaded instanceof LivingEntity living && living.isAlive()) {
            action.accept(living);
            refreshMobSnapshot(living);
            return;
        }
        Ally known = lastKnownMob(mobUuid);
        if (known == null || known.lastDimension == null) return;
        ServerLevel level = server.getLevel(known.lastDimension);
        if (level == null) return;
        ChunkPos chunk = new ChunkPos(SectionPos.blockToSectionCoord(Mth.floor(known.lastX)),
                SectionPos.blockToSectionCoord(Mth.floor(known.lastZ)));
        level.getChunkSource().addRegionTicket(ALLY_BURDEN_TICKET, chunk, BURDEN_TICKET_RADIUS, chunk, true);
        pendingBurdens.add(new PendingBurden(mobUuid, known.lastDimension, chunk, action));
    }

    private Ally lastKnownMob(UUID mobUuid) {
        for (KingData data : kings.values()) {
            for (Ally a : data.allies) {
                if (a.uuid.equals(mobUuid) && a.type != AllyType.PLAYER && a.hasData) return a;
            }
        }
        return null;
    }

    private void releaseBurdenTicket(PendingBurden pb) {
        ServerLevel level = server == null ? null : server.getLevel(pb.dimension);
        if (level != null) {
            level.getChunkSource().removeRegionTicket(ALLY_BURDEN_TICKET, pb.chunk, BURDEN_TICKET_RADIUS, pb.chunk, true);
        }
    }

    private static List<AllyDataPayload.Effect> effectsOf(LivingEntity living) {
        List<AllyDataPayload.Effect> effects = new ArrayList<>();
        for (MobEffectInstance instance : living.getActiveEffects()) {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value());
            if (id == null) continue;
            effects.add(new AllyDataPayload.Effect(id.toString(), instance.getAmplifier(), instance.getDuration()));
        }
        return effects;
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (server == null || kings.isEmpty()) return;
        UUID dead = event.getEntity().getUUID();
        boolean isPlayer = event.getEntity() instanceof Player;
        boolean changed = false;

        for (Map.Entry<UUID, KingData> entry : kings.entrySet()) {
            KingData data = entry.getValue();
            Ally match = null;
            for (Ally a : data.allies) {
                if (a.uuid.equals(dead)) {
                    match = a;
                    break;
                }
            }
            if (match == null) continue;

            ServerPlayer king = server.getPlayerList().getPlayer(entry.getKey());
            if (king != null) {
                String key = isPlayer ? "hahueuh.message.ally_died_player" : "hahueuh.message.ally_died";
                king.displayClientMessage(Component.translatable(key, match.name).withStyle(ChatFormatting.RED), false);
            }

            if (!isPlayer) {
                data.allies.remove(match);
                renormalize(data);
                changed = true;
            }
            resendIfOnline(entry.getKey());
        }
        if (changed) save();
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (server == null || pendingBurdens.isEmpty()) return;
        Iterator<PendingBurden> it = pendingBurdens.iterator();
        while (it.hasNext()) {
            PendingBurden pb = it.next();
            Entity e = findEntity(pb.mobUuid);
            if (e instanceof LivingEntity living && living.isAlive()) {
                try {
                    pb.action.accept(living);
                    if (living.isAlive()) refreshMobSnapshot(living);
                } catch (Exception ex) {
                    HahUeuh.LOGGER.warn("Failed to apply deferred Cor Leonis burden to {}", pb.mobUuid, ex);
                }
                releaseBurdenTicket(pb);
                it.remove();
            } else if (--pb.ticksRemaining <= 0) {
                // Chunk loaded but the mob's gone (despawned/removed while unloaded), or it never came in.
                releaseBurdenTicket(pb);
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
        this.filePath = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        load();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        save();
        for (PendingBurden pb : pendingBurdens) releaseBurdenTicket(pb);
        pendingBurdens.clear();
        kings.clear();
        cooldownUntilTick.clear();
        this.server = null;
        this.filePath = null;
    }

    public void refreshAllOnRollback() {
        if (server == null) return;
        load();
        cooldownUntilTick.clear();
        for (ServerPlayer king : server.getPlayerList().getPlayers()) {
            sendSnapshot(king, false);
        }
    }

    private void startCooldown(ServerPlayer king) {
        if (king.isCreative()) return;
        int cooldownSeconds = ConfigGreed.ALLY_TRACKER_COOLDOWN_SECONDS.getAsInt();
        if (cooldownSeconds <= 0) return;
        cooldownUntilTick.put(king.getUUID(), server.getTickCount() + HahUeuh.GREED_COMPAT.scaleCooldownTicks(king.getUUID(), cooldownSeconds * 20));
        PacketDistributor.sendToPlayer(king,
                new AbilityCooldownPayload(HahUeuhAbilities.ALLY_TRACKER_ABILITY, HahUeuh.GREED_COMPAT.scaleCooldownTicks(king.getUUID(), cooldownSeconds * 20)));
    }

    private int cooldownRemainingTicks(UUID uuid) {
        Integer until = cooldownUntilTick.get(uuid);
        if (until == null || server == null) return 0;
        return Math.max(0, until - server.getTickCount());
    }

    public Map<UUID, Integer> captureCooldownRemaining() {
        Map<UUID, Integer> result = new HashMap<>();
        if (server == null) return result;
        int tick = server.getTickCount();
        cooldownUntilTick.forEach((uuid, until) -> {
            int remaining = until - tick;
            if (remaining > 0) result.put(uuid, remaining);
        });
        return result;
    }

    public void restoreCooldownRemaining(Map<UUID, Integer> remainingByUuid) {
        if (server == null) return;
        cooldownUntilTick.clear();
        int tick = server.getTickCount();
        remainingByUuid.forEach((uuid, remaining) -> cooldownUntilTick.put(uuid, tick + remaining));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int remaining = remainingByUuid.getOrDefault(player.getUUID(), 0);
            PacketDistributor.sendToPlayer(player, new AbilityCooldownPayload(HahUeuhAbilities.ALLY_TRACKER_ABILITY, remaining));
        }
    }

    private Entity findEntity(UUID id) {
        if (server == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }

    private void load() {
        kings.clear();
        if (filePath == null || !Files.exists(filePath)) return;
        try {
            Map<String, StoredKing> raw = GSON.fromJson(Files.readString(filePath, StandardCharsets.UTF_8), STORE_TYPE);
            if (raw == null) return;
            raw.forEach((kingStr, stored) -> {
                try {
                    UUID king = UUID.fromString(kingStr);
                    KingData data = new KingData();
                    data.selfWeight = stored.selfWeight;
                    if (stored.allies != null) {
                        for (StoredAlly sa : stored.allies) {
                            try {
                                Ally ally = new Ally(UUID.fromString(sa.uuid),
                                        parseType(sa.type), sa.name != null ? sa.name : "?", sa.weight);
                                ally.hasData = sa.hasData;
                                ally.lastX = sa.lastX;
                                ally.lastY = sa.lastY;
                                ally.lastZ = sa.lastZ;
                                ally.lastHealth = sa.lastHealth;
                                ally.lastMaxHealth = sa.lastMaxHealth;
                                if (sa.lastDimension != null) {
                                    ResourceLocation dimId = ResourceLocation.tryParse(sa.lastDimension);
                                    if (dimId != null) ally.lastDimension = ResourceKey.create(Registries.DIMENSION, dimId);
                                }
                                data.allies.add(ally);
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                    renormalize(data);
                    kings.put(king, data);
                } catch (IllegalArgumentException e) {
                    HahUeuh.LOGGER.warn("Ignoring malformed Ally Tracker king UUID '{}'", kingStr);
                }
            });
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to load Ally Tracker data from {}", filePath, e);
        }
    }

    private void save() {
        if (filePath == null) return;
        try {
            Map<String, StoredKing> raw = new HashMap<>();
            kings.forEach((king, data) -> {
                StoredKing sk = new StoredKing();
                sk.selfWeight = data.selfWeight;
                for (Ally a : data.allies) {
                    StoredAlly sa = new StoredAlly();
                    sa.uuid = a.uuid.toString();
                    sa.type = a.type.name();
                    sa.name = a.name;
                    sa.weight = a.weight;
                    sa.hasData = a.hasData;
                    sa.lastX = a.lastX;
                    sa.lastY = a.lastY;
                    sa.lastZ = a.lastZ;
                    sa.lastHealth = a.lastHealth;
                    sa.lastMaxHealth = a.lastMaxHealth;
                    sa.lastDimension = a.lastDimension != null ? a.lastDimension.location().toString() : null;
                    sk.allies.add(sa);
                }
                raw.put(king.toString(), sk);
            });
            Files.writeString(filePath, GSON.toJson(raw, STORE_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to save Ally Tracker data to {}", filePath, e);
        }
    }

    private static AllyType parseType(String s) {
        if (s != null) {
            try {
                return AllyType.valueOf(s);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return AllyType.PASSIVE;
    }
}
