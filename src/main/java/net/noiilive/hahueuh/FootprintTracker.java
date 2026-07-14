package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.FootprintSyncPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FootprintTracker {
    private static final int SAMPLE_INTERVAL_TICKS = 10;
    private static final double MOVE_THRESHOLD_SQR = 0.04;
    private static final double DEDUP_DISTANCE_SQR = 0.25;
    private static final double SYNC_RADIUS = 48.0;
    private static final double SYNC_RADIUS_SQR = SYNC_RADIUS * SYNC_RADIUS;

    public static final int CATEGORY_HOSTILE = 0;
    public static final int CATEGORY_PASSIVE = 1;
    public static final int CATEGORY_PLAYER = 2;
    public static final int CATEGORY_WITCH_FACTOR = 3;

    public record FootprintEntry(ResourceKey<Level> dimension, UUID owner, double x, double y, double z, float yaw,
                                 String name, int category, long timestamp) {}

    private final Deque<FootprintEntry> footprints = new ArrayDeque<>();
    private final Map<UUID, Vec3> lastSampledPos = new HashMap<>();
    private final Map<UUID, List<FootprintSyncPayload.Footprint>> lastSent = new HashMap<>();
    private MinecraftServer server;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (server == null) return;
        if (server.getTickCount() % SAMPLE_INTERVAL_TICKS != 0) return;

        long now = server.overworld().getGameTime();
        sample(now);
        prune(now);
        syncAll();
    }

    private void sample(long now) {
        Map<UUID, Vec3> newPos = new HashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            ResourceKey<Level> dim = level.dimension();
            for (Entity e : level.getEntities().getAll()) {
                if (!(e instanceof LivingEntity living) || !living.isAlive() || living.isSpectator()) continue;
                UUID uuid = living.getUUID();
                Vec3 cur = living.position();
                newPos.put(uuid, cur);

                Vec3 prev = lastSampledPos.get(uuid);
                if (prev == null || prev.distanceToSqr(cur) < MOVE_THRESHOLD_SQR) continue;

                if (!living.onGround() || living.isInWater()) continue;

                if (hasNearbyFootprint(dim, uuid, cur)) continue;

                footprints.addLast(new FootprintEntry(dim, uuid, cur.x, cur.y, cur.z, living.getYRot(),
                        living.getName().getString(), classify(living), now));
            }
        }
        lastSampledPos.clear();
        lastSampledPos.putAll(newPos);
    }

    private void prune(long now) {
        long maxAge = maxAgeTicks();
        while (!footprints.isEmpty() && now - footprints.peekFirst().timestamp() > maxAge) {
            footprints.pollFirst();
        }
    }

    private void syncAll() {
        int maxAge = maxAgeTicks();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            List<FootprintSyncPayload.Footprint> near =
                    HahUeuh.VISION_OF_LIFE.isActive(uuid) ? collectNear(player) : List.of();

            if (near.equals(lastSent.get(uuid))) continue;
            PacketDistributor.sendToPlayer(player, new FootprintSyncPayload(maxAge, near));
            if (near.isEmpty()) {
                lastSent.remove(uuid);
            } else {
                lastSent.put(uuid, near);
            }
        }
    }

    private List<FootprintSyncPayload.Footprint> collectNear(ServerPlayer player) {
        ResourceKey<Level> dim = player.level().dimension();
        Vec3 pp = player.position();
        List<FootprintSyncPayload.Footprint> out = new ArrayList<>();
        UUID self = player.getUUID();
        for (FootprintEntry e : footprints) {
            if (!e.dimension().equals(dim)) continue;
            if (e.owner().equals(self)) continue; // you never see your own footprints, only other people's
            double dx = e.x() - pp.x, dy = e.y() - pp.y, dz = e.z() - pp.z;
            if (dx * dx + dy * dy + dz * dz > SYNC_RADIUS_SQR) continue;
            out.add(new FootprintSyncPayload.Footprint(e.x(), e.y(), e.z(), e.yaw(), e.category(), e.timestamp(), e.name()));
        }
        return out;
    }

    private boolean hasNearbyFootprint(ResourceKey<Level> dim, UUID owner, Vec3 pos) {
        for (FootprintEntry e : footprints) {
            if (!e.owner().equals(owner) || !e.dimension().equals(dim)) continue;
            double dx = e.x() - pos.x, dy = e.y() - pos.y, dz = e.z() - pos.z;
            if (dx * dx + dy * dy + dz * dz < DEDUP_DISTANCE_SQR) return true;
        }
        return false;
    }

    private static int classify(LivingEntity e) {
        if (VisionOfLife.holdsWitchFactor(e)) return CATEGORY_WITCH_FACTOR;
        if (e instanceof Player) return CATEGORY_PLAYER;
        if (e instanceof Enemy) return CATEGORY_HOSTILE;
        return CATEGORY_PASSIVE;
    }

    private static int maxAgeTicks() {
        return ConfigGreed.VISION_OF_LIFE_FOOTPRINT_SECONDS.getAsInt() * 20;
    }

    public void syncNow(ServerPlayer player) {
        if (server == null) return;
        List<FootprintSyncPayload.Footprint> near = collectNear(player);
        PacketDistributor.sendToPlayer(player, new FootprintSyncPayload(maxAgeTicks(), near));
        if (near.isEmpty()) lastSent.remove(player.getUUID());
        else lastSent.put(player.getUUID(), near);
    }

    public void clearFor(ServerPlayer player) {
        if (server == null) return;
        if (lastSent.remove(player.getUUID()) != null) {
            PacketDistributor.sendToPlayer(player, new FootprintSyncPayload(maxAgeTicks(), List.of()));
        }
    }

    public List<FootprintEntry> captureFootprints() {
        return new ArrayList<>(footprints);
    }

    public void restoreFootprints(List<FootprintEntry> restored) {
        footprints.clear();
        footprints.addAll(restored);
        lastSampledPos.clear();
        lastSent.clear();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
        footprints.clear();
        lastSampledPos.clear();
        lastSent.clear();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        footprints.clear();
        lastSampledPos.clear();
        lastSent.clear();
        this.server = null;
    }
}
