package net.noiilive.hahueuh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PocketDimension {
    public static final ResourceKey<Level> POCKET_LEVEL =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "pocket"));

    private static final int BOX_SIZE = 10;
    private static final int CELL_SPACING = 64;
    private static final int FLOOR_Y = 64;
    private static final int GROUP_GRID = 3;
    private static final double MANUAL_RELEASE_DISTANCE = 7.0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PERSIST_FILE_NAME = "hahueuh_pocket_dimension.json";
    private static final Type PERSIST_TYPE = new TypeToken<List<PersistedCapture>>() {}.getType();

    private final Map<UUID, Capture> captures = new HashMap<>();
    private final Set<Integer> usedCells = new HashSet<>();
    private Path persistFilePath;

    public List<UUID> banishGroup(List<? extends Entity> targets, UUID casterUuid, int playerSeconds) {
        List<UUID> banished = new ArrayList<>();
        if (targets == null || targets.isEmpty()) return banished;
        MinecraftServer server = null;
        for (Entity e : targets) {
            if (e != null) { server = e.getServer(); break; }
        }
        if (server == null) return banished;
        ServerLevel pocket = server.getLevel(POCKET_LEVEL);
        if (pocket == null) return banished;

        int cell = allocateCell();
        BlockPos origin = cellOrigin(cell);
        forceCellChunks(pocket, origin, true);
        buildRoom(pocket, origin, true);

        int slot = 0;
        for (Entity target : targets) {
            if (target == null || captures.containsKey(target.getUUID())) continue;
            if (HahUeuh.LIONS_HEART.isFrozen(target)) continue;
            Vec3 dest = groupSlotPosition(origin, slot++);
            boolean indefinite = !(target instanceof ServerPlayer);
            if (banishInto(target, pocket, cell, dest, casterUuid, indefinite, playerSeconds)) {
                banished.add(target.getUUID());
            }
        }

        if (banished.isEmpty()) {
            buildRoom(pocket, origin, false);
            forceCellChunks(pocket, origin, false);
            usedCells.remove(cell);
        }
        return banished;
    }

    private static Vec3 groupSlotPosition(BlockPos origin, int index) {
        int i = index % (GROUP_GRID * GROUP_GRID);
        int col = i % GROUP_GRID;
        int row = i / GROUP_GRID;
        double spacing = (double) (BOX_SIZE - 2) / (GROUP_GRID + 1);
        double x = origin.getX() + 1 + spacing * (col + 1);
        double z = origin.getZ() + 1 + spacing * (row + 1);
        return new Vec3(x, origin.getY() + 1, z);
    }

    private boolean banishInto(Entity target, ServerLevel pocket, int cell, Vec3 dest,
                                UUID casterUuid, boolean indefinite, int seconds) {
        if (target == null || captures.containsKey(target.getUUID())) return false;

        Capture capture = new Capture(
                target.level().dimension(), target.position(), target.getYRot(), target.getXRot(),
                cell, casterUuid, indefinite, Math.max(1, seconds) * 20);
        captures.put(target.getUUID(), capture);
        savePersisted();

        target.changeDimension(new DimensionTransition(pocket, dest, Vec3.ZERO,
                target.getYRot(), target.getXRot(), DimensionTransition.DO_NOTHING));
        return true;
    }

    public boolean isBanished(UUID uuid) {
        return captures.containsKey(uuid);
    }

    public boolean hasIndefiniteFor(UUID casterUuid) {
        return captures.values().stream().anyMatch(c -> c.indefinite && casterUuid.equals(c.casterUuid));
    }

    public int releaseAllIndefiniteFor(ServerPlayer caster) {
        MinecraftServer server = caster.getServer();
        if (server == null) return 0;
        UUID casterUuid = caster.getUUID();

        List<Map.Entry<UUID, Capture>> matched = new ArrayList<>();
        Iterator<Map.Entry<UUID, Capture>> it = captures.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Capture> entry = it.next();
            Capture capture = entry.getValue();
            if (capture.indefinite && casterUuid.equals(capture.casterUuid)) {
                matched.add(entry);
                it.remove();
            }
        }
        if (matched.isEmpty()) return 0;

        ServerLevel destLevel = caster.serverLevel();
        Vec3 destPos = lookReleasePosition(caster);
        for (Map.Entry<UUID, Capture> entry : matched) {
            releaseTo(server, entry.getKey(), entry.getValue(), destLevel, destPos, caster.getYRot(), caster.getXRot());
        }
        savePersisted();
        return matched.size();
    }

    private static Vec3 lookReleasePosition(ServerPlayer caster) {
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getViewVector(1.0f).scale(MANUAL_RELEASE_DISTANCE));
        BlockHitResult hit = caster.level().clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    public int discardAllIndefiniteFor(MinecraftServer server, UUID casterUuid) {
        int count = 0;
        ServerLevel pocket = server.getLevel(POCKET_LEVEL);
        Iterator<Map.Entry<UUID, Capture>> it = captures.entrySet().iterator();
        List<Map.Entry<UUID, Capture>> matched = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry<UUID, Capture> entry = it.next();
            Capture capture = entry.getValue();
            if (capture.indefinite && casterUuid.equals(capture.casterUuid)) {
                matched.add(entry);
                it.remove();
            }
        }
        for (Map.Entry<UUID, Capture> entry : matched) {
            Capture capture = entry.getValue();
            Entity entity = pocket != null ? pocket.getEntity(entry.getKey()) : null;
            if (entity != null) entity.discard();
            tearDownIfEmpty(server, capture.cell);
            count++;
        }
        if (count > 0) savePersisted();
        return count;
    }

    public static void spawnTransitParticles(ServerLevel level, double x, double y, double z, float height) {
        double cy = y + height / 2.0;
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, x, cy, z, 90, 0.5, height * 0.6, 0.5, 0.05);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, x, cy, z, 70, 0.4, height * 0.6, 0.4, 0.6);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, x, cy, z, 20, 0.3, height * 0.5, 0.3, 0.02);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (captures.isEmpty()) return;
        MinecraftServer server = event.getServer();
        Iterator<Map.Entry<UUID, Capture>> it = captures.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Capture> entry = it.next();
            Capture capture = entry.getValue();
            if (capture.indefinite) continue;
            if (--capture.ticksRemaining > 0) continue;
            it.remove();
            release(server, entry.getKey(), capture);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Capture capture = captures.remove(event.getEntity().getUUID());
        if (capture != null && event.getEntity().getServer() != null) {
            release(event.getEntity().getServer(), event.getEntity().getUUID(), capture);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) syncTrappedFlag(sp);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.persistFilePath = event.getServer().getWorldPath(LevelResource.ROOT).resolve(PERSIST_FILE_NAME);
        loadPersisted();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        savePersisted();
    }

    public void reloadFromDisk() {
        loadPersisted();
    }

    public void reconcileAfterRollback(MinecraftServer server) {
        if (captures.isEmpty()) return;
        ServerLevel pocket = server.getLevel(POCKET_LEVEL);

        Set<UUID> affectedCasters = new HashSet<>();
        Set<Integer> touchedCells = new HashSet<>();
        boolean changed = false;
        Iterator<Map.Entry<UUID, Capture>> it = captures.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Capture> entry = it.next();
            Capture capture = entry.getValue();
            Entity entity = pocket != null ? pocket.getEntity(entry.getKey()) : null;
            if (entity == null || !entity.isAlive()) {
                affectedCasters.add(capture.casterUuid);
                touchedCells.add(capture.cell);
                it.remove();
                changed = true;
            }
        }

        for (int cell : touchedCells) {
            tearDownIfEmpty(server, cell);
        }
        if (changed) savePersisted();

        if (pocket != null) {
            Set<Integer> liveCells = new HashSet<>();
            for (Capture capture : captures.values()) liveCells.add(capture.cell);
            for (int cell : liveCells) {
                BlockPos origin = cellOrigin(cell);
                forceCellChunks(pocket, origin, true);
                buildRoom(pocket, origin, true);
            }
        }

        for (UUID casterUuid : affectedCasters) {
            ServerPlayer caster = server.getPlayerList().getPlayer(casterUuid);
            if (caster != null) syncTrappedFlag(caster);
        }
    }

    public void syncTrappedFlag(ServerPlayer player) {
        boolean actual = hasIndefiniteFor(player.getUUID());
        boolean flagged = player.getData(ModAttachments.PLAYER_HAS_TRAPPED_ENTITIES.get());
        if (actual != flagged) player.setData(ModAttachments.PLAYER_HAS_TRAPPED_ENTITIES.get(), actual);
    }

    private void loadPersisted() {
        captures.clear();
        usedCells.clear();
        if (persistFilePath == null || !Files.exists(persistFilePath)) return;
        try {
            List<PersistedCapture> list = GSON.fromJson(Files.readString(persistFilePath, StandardCharsets.UTF_8), PERSIST_TYPE);
            if (list == null) return;
            for (PersistedCapture pc : list) {
                UUID entityUuid = safeUuid(pc.entityUuid());
                UUID casterUuid = safeUuid(pc.casterUuid());
                ResourceLocation dimLoc = ResourceLocation.tryParse(pc.originDim());
                if (entityUuid == null || casterUuid == null || dimLoc == null) {
                    HahUeuh.LOGGER.warn("Ignoring malformed persisted pocket dimension capture entry");
                    continue;
                }
                ResourceKey<Level> originDim = ResourceKey.create(Registries.DIMENSION, dimLoc);
                Capture capture = new Capture(originDim, new Vec3(pc.x(), pc.y(), pc.z()),
                        pc.yRot(), pc.xRot(), pc.cell(), casterUuid, pc.indefinite(), pc.ticksRemaining());
                captures.put(entityUuid, capture);
                usedCells.add(pc.cell());
            }
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to load persisted pocket dimension captures from {}", persistFilePath, e);
        }
    }

    private static UUID safeUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void savePersisted() {
        if (persistFilePath == null) return;
        try {
            List<PersistedCapture> list = new ArrayList<>();
            for (Map.Entry<UUID, Capture> entry : captures.entrySet()) {
                Capture c = entry.getValue();
                list.add(new PersistedCapture(
                        entry.getKey().toString(), c.originDim.location().toString(),
                        c.originPos.x, c.originPos.y, c.originPos.z,
                        c.yRot, c.xRot, c.cell, c.casterUuid.toString(), c.indefinite, c.ticksRemaining));
            }
            Files.createDirectories(persistFilePath.getParent());
            Files.writeString(persistFilePath, GSON.toJson(list, PERSIST_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to save persisted pocket dimension captures to {}", persistFilePath, e);
        }
    }

    private record PersistedCapture(
            String entityUuid, String originDim, double x, double y, double z,
            float yRot, float xRot, int cell, String casterUuid, boolean indefinite, int ticksRemaining) {}

    private void release(MinecraftServer server, UUID uuid, Capture capture) {
        ServerLevel originLevel = server.getLevel(capture.originDim);
        releaseTo(server, uuid, capture, originLevel, capture.originPos, capture.yRot, capture.xRot);
        savePersisted();
    }

    private void releaseTo(MinecraftServer server, UUID uuid, Capture capture,
                           ServerLevel destLevel, Vec3 destPos, float yRot, float xRot) {
        ServerLevel pocket = server.getLevel(POCKET_LEVEL);

        Entity entity = pocket != null ? pocket.getEntity(uuid) : null;
        if (entity != null && destLevel != null) {
            try {
                float height = entity.getBbHeight();
                entity.changeDimension(new DimensionTransition(destLevel, destPos, Vec3.ZERO,
                        yRot, xRot, DimensionTransition.DO_NOTHING));
                spawnTransitParticles(destLevel, destPos.x, destPos.y, destPos.z, height);
            } catch (Exception ignored) {
            }
        }

        tearDownIfEmpty(server, capture.cell);
    }

    private void tearDownIfEmpty(MinecraftServer server, int cell) {
        boolean cellStillOccupied = captures.values().stream().anyMatch(c -> c.cell == cell);
        if (cellStillOccupied) return;

        ServerLevel pocket = server.getLevel(POCKET_LEVEL);
        if (pocket != null) {
            BlockPos origin = cellOrigin(cell);
            buildRoom(pocket, origin, false);
            forceCellChunks(pocket, origin, false);
        }
        usedCells.remove(cell);
    }

    private int allocateCell() {
        int i = 0;
        while (usedCells.contains(i)) i++;
        usedCells.add(i);
        return i;
    }

    private static BlockPos cellOrigin(int cell) {
        return new BlockPos(cell * CELL_SPACING, FLOOR_Y, 0);
    }

    private static void buildRoom(ServerLevel level, BlockPos origin, boolean build) {
        var wall = build ? ModBlocks.POCKET_VOID.get().defaultBlockState() : Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < BOX_SIZE; x++) {
            for (int y = 0; y < BOX_SIZE; y++) {
                for (int z = 0; z < BOX_SIZE; z++) {
                    boolean shell = x == 0 || x == BOX_SIZE - 1 || y == 0 || y == BOX_SIZE - 1
                            || z == 0 || z == BOX_SIZE - 1;
                    if (!shell) continue;
                    pos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    level.setBlock(pos, wall, BLOCK_FLAGS);
                }
            }
        }
    }

    private static final int BLOCK_FLAGS = 2 | 16;

    private static void forceCellChunks(ServerLevel level, BlockPos origin, boolean add) {
        int minChunkX = origin.getX() >> 4;
        int maxChunkX = (origin.getX() + BOX_SIZE) >> 4;
        int minChunkZ = origin.getZ() >> 4;
        int maxChunkZ = (origin.getZ() + BOX_SIZE) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                level.setChunkForced(cx, cz, add);
            }
        }
    }

    private static final class Capture {
        final ResourceKey<Level> originDim;
        final Vec3 originPos;
        final float yRot, xRot;
        final int cell;
        final UUID casterUuid;
        final boolean indefinite;
        int ticksRemaining;

        Capture(ResourceKey<Level> originDim, Vec3 originPos, float yRot, float xRot, int cell,
                UUID casterUuid, boolean indefinite, int ticksRemaining) {
            this.originDim = originDim;
            this.originPos = originPos;
            this.yRot = yRot;
            this.xRot = xRot;
            this.cell = cell;
            this.casterUuid = casterUuid;
            this.indefinite = indefinite;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
