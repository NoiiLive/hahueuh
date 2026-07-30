package net.noiilive.hahueuh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DoorCrossing {
    private static final int CELL_GAP = 64;
    private static final int FLOOR_Y = 64;
    private static final int ROOM_Z_BAND = 4096;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PERSIST_FILE_NAME = "hahueuh_door_crossing.json";
    private static final Type PERSIST_TYPE = new TypeToken<List<PersistedRoom>>() {}.getType();

    private final Map<UUID, Room> rooms = new HashMap<>();
    private final Map<DoorKey, Link> links = new ConcurrentHashMap<>();
    private final Map<UUID, Visit> visitors = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> steppedClear = ConcurrentHashMap.newKeySet();
    private Path persistFilePath;
    private long tickCounter;

    public void tryCast(ServerPlayer caster) {
        if (caster.level().dimension() == PocketDimension.POCKET_LEVEL) {
            if (rooms.containsKey(caster.getUUID())) ejectLookedAtOccupant(caster);
            return;
        }
        net.noiilive.hahueuh.magic.SpellRegistry.get(net.noiilive.hahueuh.magic.Spells.DOOR_CROSSING)
                .ifPresent(spell -> HahUeuh.SPELL_CASTING.tryStart(caster, spell));
    }

    public void cast(ServerPlayer caster) {
        if (caster.level().dimension() == PocketDimension.POCKET_LEVEL) return;
        BlockPos door = lookedAtDoor(caster);
        if (door == null) {
            actionBar(caster, "hahueuh.message.door_crossing_no_door", ChatFormatting.RED);
            return;
        }
        armEntrance(caster, door);
    }

    private static void rememberEntryDoor(Room room, BlockPos door, float lookYRot) {
        room.entryPos = new Vec3(door.getX() + 0.5, door.getY(), door.getZ() + 0.5);
        room.entryYRot = net.minecraft.util.Mth.wrapDegrees(lookYRot + 180.0f);
        room.hasEntry = true;
    }

    private boolean ejectLookedAtOccupant(ServerPlayer caster) {
        double range = ConfigMagicYin.DOOR_CROSSING_RANGE.get();
        net.minecraft.world.phys.HitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil
                .getHitResultOnViewVector(caster,
                        e -> e != caster && e.isAlive() && !e.isSpectator(), range);
        if (!(hit instanceof net.minecraft.world.phys.EntityHitResult ehr)) return false;

        Entity target = ehr.getEntity();
        if (target instanceof ServerPlayer visitor && visitors.containsKey(visitor.getUUID())) {
            expelVisitor(visitor);
            actionBar(caster, "hahueuh.message.door_crossing_evicted", ChatFormatting.DARK_PURPLE);
            return true;
        }

        Room room = rooms.get(caster.getUUID());
        if (room == null) return false;
        ServerLevel destination = caster.getServer() == null
                ? null : caster.getServer().getLevel(room.returnDim);
        if (destination == null) return false;

        Vec3 dest = room.returnPos;
        if (target.level() instanceof ServerLevel origin) {
            PocketDimension.spawnTransitParticles(origin,
                    target.getX(), target.getY(), target.getZ(), target.getBbHeight());
            playExitSound(origin, target.position());
        }
        target.changeDimension(new DimensionTransition(destination, dest, Vec3.ZERO,
                target.getYRot(), target.getXRot(), DimensionTransition.DO_NOTHING));
        PocketDimension.spawnTransitParticles(destination, dest.x, dest.y, dest.z, target.getBbHeight());
        playExitSound(destination, dest);
        actionBar(caster, "hahueuh.message.door_crossing_evicted", ChatFormatting.DARK_PURPLE);
        return true;
    }

    private void armEntrance(ServerPlayer caster, BlockPos door) {
        MinecraftServer server = caster.getServer();
        if (server == null) return;
        ServerLevel pocket = server.getLevel(PocketDimension.POCKET_LEVEL);
        if (pocket == null) {
            actionBar(caster, "hahueuh.message.door_crossing_failed", ChatFormatting.RED);
            return;
        }

        Room room = rooms.computeIfAbsent(caster.getUUID(), uuid -> new Room(allocateCell()));
        BlockPos origin = cellOrigin(room.cell);
        forceRoomChunks(pocket, origin, true);
        ensureRoomBuilt(pocket, room, origin);

        room.returnDim = caster.level().dimension();
        room.returnPos = caster.position();
        room.returnYRot = caster.getYRot();
        room.returnXRot = caster.getXRot();
        savePersisted();

        linkDoor(caster.level().dimension(), door, caster.getUUID(),
                tickCounter + ConfigMagicYin.DOOR_CROSSING_ENTRANCE_SECONDS.get() * 20L);
        room.strayCentre = door.immutable();
        int strays = linkStrayDoors(caster.serverLevel(), caster.getUUID(), door);

        pocket.playSound(null, BlockPos.containing(doorwayPosition(origin)), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.6f, 0.5f);
        caster.serverLevel().playSound(null, door, ModSounds.DOOR_CROSSING_OPEN.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);
        PocketDimension.spawnTransitParticles(caster.serverLevel(),
                door.getX() + 0.5, door.getY(), door.getZ() + 0.5, 2.0f);

        actionBar(caster, strays > 0
                ? "hahueuh.message.door_crossing_armed_leaky" : "hahueuh.message.door_crossing_armed",
                ChatFormatting.DARK_PURPLE);
    }

    private void enter(ServerPlayer player, Room room, boolean asOwner) {
        MinecraftServer server = player.getServer();
        steppedClear.remove(player.getUUID());
        ServerLevel pocket = server == null ? null : server.getLevel(PocketDimension.POCKET_LEVEL);
        if (pocket == null) return;

        if (asOwner) {
            room.occupied = true;
            savePersisted();
        } else {
            visitors.put(player.getUUID(), new Visit(roomOwner(room),
                    player.level().dimension(), player.position(), player.getYRot(), player.getXRot()));
        }
        sendInto(player, pocket, room);
        actionBar(player, asOwner
                ? "hahueuh.message.door_crossing_entered" : "hahueuh.message.door_crossing_stumbled",
                ChatFormatting.DARK_PURPLE);
    }

    private UUID roomOwner(Room room) {
        for (Map.Entry<UUID, Room> entry : rooms.entrySet()) {
            if (entry.getValue() == room) return entry.getKey();
        }
        return null;
    }

    private void leave(ServerPlayer caster, Room room) {
        MinecraftServer server = caster.getServer();
        if (server == null) return;
        ServerLevel destination = server.getLevel(room.returnDim);
        if (destination == null) {
            actionBar(caster, "hahueuh.message.door_crossing_failed", ChatFormatting.RED);
            return;
        }

        room.occupied = false;
        unlinkAllFor(caster.getUUID());
        expelVisitorsOf(caster.getUUID(), server);
        savePersisted();

        Vec3 dest = room.returnPos;
        PocketDimension.spawnTransitParticles(caster.serverLevel(),
                caster.getX(), caster.getY(), caster.getZ(), caster.getBbHeight());
        playExitSound(caster.serverLevel(), caster.position());
        caster.changeDimension(new DimensionTransition(destination, dest, Vec3.ZERO,
                room.returnYRot, room.returnXRot, DimensionTransition.DO_NOTHING));
        PocketDimension.spawnTransitParticles(destination, dest.x, dest.y, dest.z, caster.getBbHeight());
        playExitSound(destination, dest);
        actionBar(caster, "hahueuh.message.door_crossing_left", ChatFormatting.DARK_PURPLE);
    }

    private void sendInto(ServerPlayer player, ServerLevel pocket, Room room) {
        Vec3 dest = entryPosition(pocket, room);
        float yRot = room.hasEntry ? room.entryYRot : player.getYRot();
        PocketDimension.spawnTransitParticles(player.serverLevel(),
                player.getX(), player.getY(), player.getZ(), player.getBbHeight());
        player.changeDimension(new DimensionTransition(pocket, dest, Vec3.ZERO,
                yRot, player.getXRot(), DimensionTransition.DO_NOTHING));
        PocketDimension.spawnTransitParticles(pocket, dest.x, dest.y, dest.z, player.getBbHeight());
        pocket.playSound(null, BlockPos.containing(dest), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.8f, 0.7f);
    }

    private void checkWalkThroughs(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level().dimension() == PocketDimension.POCKET_LEVEL) {
                UUID id = player.getUUID();
                long doorPos = standingInDoor(player);
                if (doorPos == Long.MIN_VALUE) {
                    steppedClear.add(id);
                    continue;
                }
                if (!steppedClear.contains(id)) continue;
                steppedClear.remove(id);
                if (visitors.containsKey(id)) {
                    expelVisitor(player);
                    continue;
                }
                Room room = rooms.get(id);
                if (room != null && room.occupied) {
                    rememberEntryDoor(room, BlockPos.of(doorPos), player.getYRot());
                    leave(player, room);
                }
                continue;
            }
            if (HahUeuh.LIONS_HEART.isFrozen(player)) continue;

            Link link = links.get(new DoorKey(player.level().dimension().location().toString(),
                    standingInDoor(player)));
            if (link == null) continue;

            Room room = rooms.get(link.ownerUuid());
            if (room == null) continue;

            boolean owner = link.ownerUuid().equals(player.getUUID());
            boolean armed = link.expiresAtTick() >= 0;
            if (!owner && !armed && !room.occupied) continue;

            enter(player, room, owner);
            if (!owner && !armed) rerollStrayDoors(link.ownerUuid(), server);
        }
    }

    private static long standingInDoor(ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        BlockState state = player.level().getBlockState(feet);
        if (state.getBlock() instanceof DoorBlock) return normalizeDoor(feet, state).asLong();
        BlockState above = player.level().getBlockState(feet.above());
        if (above.getBlock() instanceof DoorBlock) return normalizeDoor(feet.above(), above).asLong();
        return Long.MIN_VALUE;
    }

    public void expelVisitor(ServerPlayer player) {
        Visit visit = visitors.remove(player.getUUID());
        if (visit == null) return;
        sendVisitorHome(player, visit);
        actionBar(player, "hahueuh.message.door_crossing_expelled", ChatFormatting.DARK_PURPLE);
    }

    private void expelVisitorsOf(UUID owner, MinecraftServer server) {
        Iterator<Map.Entry<UUID, Visit>> it = visitors.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Visit> entry = it.next();
            if (!entry.getValue().ownerUuid().equals(owner)) continue;
            it.remove();
            ServerPlayer visitor = server.getPlayerList().getPlayer(entry.getKey());
            if (visitor != null) {
                sendVisitorHome(visitor, entry.getValue());
                actionBar(visitor, "hahueuh.message.door_crossing_expelled", ChatFormatting.DARK_PURPLE);
            }
        }
    }

    private void sendVisitorHome(ServerPlayer player, Visit visit) {
        MinecraftServer server = player.getServer();
        ServerLevel destination = server == null ? null : server.getLevel(visit.dim());
        if (destination == null) return;
        PocketDimension.spawnTransitParticles(player.serverLevel(),
                player.getX(), player.getY(), player.getZ(), player.getBbHeight());
        playExitSound(player.serverLevel(), player.position());
        player.changeDimension(new DimensionTransition(destination, visit.pos(), Vec3.ZERO,
                visit.yRot(), visit.xRot(), DimensionTransition.DO_NOTHING));
        PocketDimension.spawnTransitParticles(destination, visit.pos().x, visit.pos().y, visit.pos().z,
                player.getBbHeight());
        playExitSound(destination, visit.pos());
    }

    private static void playExitSound(ServerLevel level, Vec3 pos) {
        level.playSound(null, pos.x, pos.y, pos.z, ModSounds.DOOR_CROSSING_EXIT.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private BlockPos lookedAtDoor(ServerPlayer caster) {
        double range = ConfigMagicYin.DOOR_CROSSING_RANGE.get();
        Vec3 eye = caster.getEyePosition();
        Vec3 view = caster.getViewVector(1.0f);
        Vec3 end = eye.add(view.scale(range));

        BlockHitResult hit = caster.level().clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockState hitState = caster.level().getBlockState(hit.getBlockPos());
            if (hitState.getBlock() instanceof DoorBlock) {
                return normalizeDoor(hit.getBlockPos(), hitState);
            }
        }
        return doorAlongRay(caster, eye, view, range);
    }

    private static BlockPos doorAlongRay(ServerPlayer caster, Vec3 eye, Vec3 view, double range) {
        BlockPos last = null;
        for (double d = 0.0; d <= range; d += 0.25) {
            BlockPos pos = BlockPos.containing(eye.add(view.scale(d)));
            if (pos.equals(last)) continue;
            last = pos;
            BlockState state = caster.level().getBlockState(pos);
            if (state.getBlock() instanceof DoorBlock) return normalizeDoor(pos, state);
        }
        return null;
    }

    private static BlockPos normalizeDoor(BlockPos pos, BlockState state) {
        if (state.hasProperty(DoorBlock.HALF) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            return pos.below().immutable();
        }
        return pos.immutable();
    }

    private void linkDoor(ResourceKey<Level> dim, BlockPos door, UUID owner, long expiresAtTick) {
        links.put(new DoorKey(dim.location().toString(), door.asLong()), new Link(owner, expiresAtTick));
    }

    private void rerollStrayDoors(UUID owner, MinecraftServer server) {
        Room room = rooms.get(owner);
        if (room == null || room.strayCentre == null || server == null) return;
        ServerLevel level = server.getLevel(room.returnDim);
        if (level == null) return;
        links.entrySet().removeIf(entry -> entry.getValue().ownerUuid().equals(owner)
                && entry.getValue().expiresAtTick() < 0);
        linkStrayDoors(level, owner, room.strayCentre);
    }

    private int linkStrayDoors(ServerLevel level, UUID owner, BlockPos castDoor) {
        int radius = ConfigMagicYin.DOOR_CROSSING_STRAY_DOOR_RADIUS.get();
        int chance = ConfigMagicYin.DOOR_CROSSING_STRAY_DOOR_CHANCE.get();
        if (radius <= 0 || chance <= 0) return 0;

        BlockPos centre = castDoor;
        int linked = 0;

        int minChunkX = (centre.getX() - radius) >> 4;
        int maxChunkX = (centre.getX() + radius) >> 4;
        int minChunkZ = (centre.getZ() - radius) >> 4;
        int maxChunkZ = (centre.getZ() + radius) >> 4;
        int radiusSqr = radius * radius;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    LevelChunkSection section = sections[i];
                    if (section.hasOnlyAir()) continue;
                    if (!section.maybeHas(s -> s.getBlock() instanceof DoorBlock)) continue;

                    int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
                    for (int lx = 0; lx < 16; lx++) {
                        for (int ly = 0; ly < 16; ly++) {
                            for (int lz = 0; lz < 16; lz++) {
                                BlockState state = section.getBlockState(lx, ly, lz);
                                if (!(state.getBlock() instanceof DoorBlock)) continue;
                                if (state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) continue;

                                int x = SectionPos.sectionToBlockCoord(cx) + lx;
                                int y = baseY + ly;
                                int z = SectionPos.sectionToBlockCoord(cz) + lz;
                                if (x == castDoor.getX() && y == castDoor.getY() && z == castDoor.getZ()) continue;
                                int dx = x - centre.getX();
                                int dy = y - centre.getY();
                                int dz = z - centre.getZ();
                                if (dx * dx + dy * dy + dz * dz > radiusSqr) continue;
                                if (level.getRandom().nextInt(100) >= chance) continue;

                                linkDoor(level.dimension(), new BlockPos(x, y, z), owner, -1L);
                                linked++;
                            }
                        }
                    }
                }
            }
        }
        return linked;
    }

    private void unlinkAllFor(UUID owner) {
        links.entrySet().removeIf(entry -> entry.getValue().ownerUuid().equals(owner));
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;

        Iterator<Map.Entry<DoorKey, Link>> it = links.entrySet().iterator();
        while (it.hasNext()) {
            Link link = it.next().getValue();
            if (link.expiresAtTick() >= 0 && tickCounter >= link.expiresAtTick()) it.remove();
        }

        checkWalkThroughs(event.getServer());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.persistFilePath = event.getServer().getWorldPath(LevelResource.ROOT).resolve(PERSIST_FILE_NAME);
        links.clear();
        visitors.clear();
        steppedClear.clear();
        loadPersisted();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        savePersisted();
    }

    private int allocateCell() {
        int cell = 0;
        while (isCellTaken(cell)) cell++;
        return cell;
    }

    private boolean isCellTaken(int cell) {
        for (Room room : rooms.values()) {
            if (room.cell == cell) return true;
        }
        return false;
    }

    private static BlockPos cellOrigin(int cell) {
        int stride = ConfigMagicYin.DOOR_CROSSING_ROOM_SIZE.get() + CELL_GAP;
        return new BlockPos(cell * stride, FLOOR_Y, ROOM_Z_BAND);
    }

    private static Vec3 doorwayPosition(BlockPos origin) {
        return new Vec3(origin.getX() + 1.5, origin.getY() + 1, origin.getZ() + 1.5);
    }

    private static Vec3 entryPosition(ServerLevel pocket, Room room) {
        Vec3 fallback = doorwayPosition(cellOrigin(room.cell));
        if (!room.hasEntry || room.entryPos == null) return fallback;
        BlockPos feet = BlockPos.containing(room.entryPos);
        if (isBlocked(pocket, feet) || isBlocked(pocket, feet.above())) return fallback;
        return room.entryPos;
    }

    private static boolean isBlocked(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isSuffocating(level, pos);
    }

    private void ensureRoomBuilt(ServerLevel pocket, Room room, BlockPos origin) {
        if (room.built && roomShellPresent(pocket, origin)) return;
        buildRoom(pocket, origin);
        room.built = true;
        room.hasEntry = false;
        room.entryPos = null;
        savePersisted();
    }

    private static boolean roomShellPresent(ServerLevel level, BlockPos origin) {
        BlockState expected = ModBlocks.POCKET_VOID.get().defaultBlockState();
        int size = ConfigMagicYin.DOOR_CROSSING_ROOM_SIZE.get();
        int height = ConfigMagicYin.DOOR_CROSSING_ROOM_HEIGHT.get();
        return level.getBlockState(origin) == expected
                && level.getBlockState(origin.offset(size - 1, 0, size - 1)) == expected
                && level.getBlockState(origin.offset(0, height - 1, 0)) == expected;
    }

    public void reloadFromDisk() {
        links.clear();
        visitors.clear();
        loadPersisted();
    }

    public void reconcileAfterRollback() {
        links.clear();
        visitors.clear();
        for (Room room : rooms.values()) {
            room.occupied = false;
            room.built = false;
            room.hasEntry = false;
            room.entryPos = null;
        }
        savePersisted();
    }

    private static void buildRoom(ServerLevel level, BlockPos origin) {
        int size = ConfigMagicYin.DOOR_CROSSING_ROOM_SIZE.get();
        int height = ConfigMagicYin.DOOR_CROSSING_ROOM_HEIGHT.get();
        BlockState wall = ModBlocks.POCKET_VOID.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < size; z++) {
                    boolean shell = x == 0 || x == size - 1 || y == 0 || y == height - 1
                            || z == 0 || z == size - 1;
                    if (!shell) continue;
                    pos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    level.setBlock(pos, wall, BLOCK_FLAGS);
                }
            }
        }

        BlockPos doorPos = origin.offset(1, 1, 1);
        BlockState lower = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        level.setBlock(doorPos, lower, BLOCK_FLAGS);
        level.setBlock(doorPos.above(), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), BLOCK_FLAGS);
    }

    private static final int BLOCK_FLAGS = 2 | 16;

    private static void forceRoomChunks(ServerLevel level, BlockPos origin, boolean add) {
        int size = ConfigMagicYin.DOOR_CROSSING_ROOM_SIZE.get();
        for (int cx = origin.getX() >> 4; cx <= (origin.getX() + size) >> 4; cx++) {
            for (int cz = origin.getZ() >> 4; cz <= (origin.getZ() + size) >> 4; cz++) {
                level.setChunkForced(cx, cz, add);
            }
        }
    }

    private void loadPersisted() {
        rooms.clear();
        if (persistFilePath == null || !Files.exists(persistFilePath)) return;
        try {
            List<PersistedRoom> list = GSON.fromJson(
                    Files.readString(persistFilePath, StandardCharsets.UTF_8), PERSIST_TYPE);
            if (list == null) return;
            for (PersistedRoom pr : list) {
                UUID owner = safeUuid(pr.ownerUuid());
                ResourceLocation dimLoc = ResourceLocation.tryParse(pr.returnDim());
                if (owner == null || dimLoc == null) {
                    HahUeuh.LOGGER.warn("Ignoring malformed persisted Door Crossing room entry");
                    continue;
                }
                Room room = new Room(pr.cell());
                room.built = pr.built();
                room.occupied = pr.occupied();
                room.returnDim = ResourceKey.create(Registries.DIMENSION, dimLoc);
                room.returnPos = new Vec3(pr.x(), pr.y(), pr.z());
                room.returnYRot = pr.yRot();
                room.returnXRot = pr.xRot();
                if (pr.hasEntry()) {
                    room.hasEntry = true;
                    room.entryPos = new Vec3(pr.entryX(), pr.entryY(), pr.entryZ());
                    room.entryYRot = pr.entryYRot();
                }
                rooms.put(owner, room);
            }
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to load persisted Door Crossing rooms from {}", persistFilePath, e);
        }
    }

    private void savePersisted() {
        if (persistFilePath == null) return;
        try {
            List<PersistedRoom> list = new ArrayList<>();
            for (Map.Entry<UUID, Room> entry : rooms.entrySet()) {
                Room room = entry.getValue();
                list.add(new PersistedRoom(entry.getKey().toString(), room.cell, room.built, room.occupied,
                        room.returnDim.location().toString(),
                        room.returnPos.x, room.returnPos.y, room.returnPos.z,
                        room.returnYRot, room.returnXRot,
                        room.hasEntry,
                        room.entryPos == null ? 0 : room.entryPos.x,
                        room.entryPos == null ? 0 : room.entryPos.y,
                        room.entryPos == null ? 0 : room.entryPos.z,
                        room.entryYRot));
            }
            Files.createDirectories(persistFilePath.getParent());
            Files.writeString(persistFilePath, GSON.toJson(list, PERSIST_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to save persisted Door Crossing rooms to {}", persistFilePath, e);
        }
    }

    private static UUID safeUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void actionBar(ServerPlayer player, String key, ChatFormatting style) {
        player.displayClientMessage(Component.translatable(key).withStyle(style), true);
    }

    private record PersistedRoom(String ownerUuid, int cell, boolean built, boolean occupied,
                                 String returnDim, double x, double y, double z,
                                 float yRot, float xRot,
                                 boolean hasEntry, double entryX, double entryY, double entryZ,
                                 float entryYRot) {}

    private record DoorKey(String dimension, long packedPos) {}

    private record Link(UUID ownerUuid, long expiresAtTick) {}

    private record Visit(UUID ownerUuid, ResourceKey<Level> dim, Vec3 pos, float yRot, float xRot) {}

    private static final class Room {
        final int cell;
        boolean built;
        boolean occupied;
        ResourceKey<Level> returnDim = Level.OVERWORLD;
        Vec3 returnPos = Vec3.ZERO;
        float returnYRot;
        float returnXRot;
        boolean hasEntry;
        Vec3 entryPos;
        float entryYRot;
        BlockPos strayCentre;

        Room(int cell) {
            this.cell = cell;
        }
    }
}
