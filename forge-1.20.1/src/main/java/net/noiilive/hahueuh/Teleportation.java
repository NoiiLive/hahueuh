package net.noiilive.hahueuh;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Teleportation {
    private static final int TICKS_PER_SECOND = 20;
    private static final int REENTRY_IMMUNITY_TICKS = 40;

    private static final com.google.gson.Gson GSON =
            new com.google.gson.GsonBuilder().setPrettyPrinting().create();
    private static final String PERSIST_FILE_NAME = "hahueuh_teleport_portals.json";
    private static final java.lang.reflect.Type PERSIST_TYPE =
            new com.google.gson.reflect.TypeToken<List<PersistedPortal>>() {}.getType();

    private java.nio.file.Path persistFilePath;

    private final Map<UUID, Request> pending = new ConcurrentHashMap<>();
    private final List<Portal> portals = new CopyOnWriteArrayList<>();
    private final Map<UUID, Long> reentryBlock = new ConcurrentHashMap<>();

    public void request(ServerPlayer caster, int x, int y, int z, boolean portal) {
        Vec3 destination = new Vec3(x + 0.5, y, z + 0.5);
        int distance = (int) Math.round(caster.position().distanceTo(destination));

        int total = ConfigMagicYin.TELEPORT_TOTAL_MANA.get()
                + distance * ConfigMagicYin.TELEPORT_MANA_PER_BLOCK.get()
                + (portal ? ConfigMagicYin.TELEPORT_PORTAL_EXTRA_MANA.get()
                          : ConfigMagicYin.TELEPORT_SELF_EXTRA_MANA.get());

        pending.put(caster.getUUID(), new Request(destination, portal));
        HahUeuh.SPELL_CASTING.overrideNextTotalMana(caster, total);
        actionBar(caster, Component.translatable("hahueuh.message.teleport_charging", total, distance)
                .withStyle(ChatFormatting.LIGHT_PURPLE));

        net.noiilive.hahueuh.magic.SpellRegistry.get(net.noiilive.hahueuh.magic.Spells.TELEPORTATION)
                .ifPresent(spell -> HahUeuh.SPELL_CASTING.tryStart(caster, spell));
    }

    public void cast(ServerPlayer caster) {
        Request request = pending.remove(caster.getUUID());
        if (request == null) {
            actionBar(caster, Component.translatable("hahueuh.message.teleport_no_target")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        ServerLevel level = caster.serverLevel();
        if (request.portal) {
            openPortal(level, portalPlacement(caster), request.destination);
            actionBar(caster, Component.translatable("hahueuh.message.teleport_portal_opened")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            burst(level, caster.position());
            caster.teleportTo(request.destination.x, request.destination.y, request.destination.z);
            burst(level, request.destination);
            level.playSound(null, BlockPos.containing(request.destination), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 1.0f, 1.0f);
            actionBar(caster, Component.translatable("hahueuh.message.teleport_self")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private void openPortal(ServerLevel level, Vec3 from, Vec3 to) {
        long expiry = gameTime(level.getServer())
                + (long) ConfigMagicYin.TELEPORT_PORTAL_SECONDS.get() * TICKS_PER_SECOND;
        portals.add(new Portal(level.dimension(), from, to, expiry));
        savePersisted();
        burst(level, from);
        burst(level, to);
        level.playSound(null, BlockPos.containing(from), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.0f, 0.7f);
        level.playSound(null, BlockPos.containing(to), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    private static Vec3 portalPlacement(ServerPlayer caster) {
        double range = ConfigMagicYin.TELEPORT_PORTAL_PLACEMENT_RANGE.get();
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getViewVector(1.0f).scale(range));
        net.minecraft.world.phys.BlockHitResult hit = caster.level().clip(
                new net.minecraft.world.level.ClipContext(eye, end,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE, caster));

        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return end;
        BlockPos free = hit.getBlockPos().relative(hit.getDirection());
        return new Vec3(free.getX() + 0.5, free.getY(), free.getZ() + 0.5);
    }

    private static void burst(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y + 1.0, pos.z, 80, 0.4, 0.8, 0.4, 0.6);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 1.0, pos.z, 40, 0.3, 0.6, 0.3, 0.2);
        level.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y + 1.0, pos.z, 45, 0.35, 0.7, 0.35, 0.05);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 0.6, pos.z, 30, 0.4, 0.4, 0.4, 0.02);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || portals.isEmpty()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        long now = gameTime(server);
        boolean expired = false;

        for (Portal portal : portals) {
            if (now >= portal.expiry) {
                portals.remove(portal);
                expired = true;
                continue;
            }
            ServerLevel level = server.getLevel(portal.dimension);
            if (level == null) continue;
            drawPortal(level, portal.from);
            drawPortal(level, portal.to);
        }

        if (expired) savePersisted();
        if (portals.isEmpty()) return;
        double radius = ConfigMagicYin.TELEPORT_PORTAL_RADIUS.get();
        double radiusSqr = radius * radius;

        for (ServerPlayer player : new ArrayList<>(server.getPlayerList().getPlayers())) {
            if (HahUeuh.LIONS_HEART.isFrozen(player)) continue;
            Long blockedUntil = reentryBlock.get(player.getUUID());
            if (blockedUntil != null) {
                if (now < blockedUntil) continue;
                reentryBlock.remove(player.getUUID());
            }
            for (Portal portal : portals) {
                if (!portal.dimension.equals(player.level().dimension())) continue;
                if (player.position().distanceToSqr(portal.from) <= radiusSqr) {
                    step(player, portal.to);
                    break;
                }
                if (player.position().distanceToSqr(portal.to) <= radiusSqr) {
                    step(player, portal.from);
                    break;
                }
            }
        }
    }

    private void step(ServerPlayer player, Vec3 destination) {
        ServerLevel level = player.serverLevel();
        burst(level, player.position());
        player.teleportTo(destination.x, destination.y, destination.z);
        burst(level, destination);
        level.playSound(null, BlockPos.containing(destination), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.0f, 1.0f);
        reentryBlock.put(player.getUUID(), gameTime(player.getServer()) + REENTRY_IMMUNITY_TICKS);
    }

    private static void drawPortal(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y + 1.0, pos.z, 8, 0.35, 0.7, 0.35, 0.15);
        level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y + 1.0, pos.z, 2, 0.3, 0.6, 0.3, 0.01);
        level.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y + 1.0, pos.z, 4, 0.3, 0.6, 0.3, 0.01);
        level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.4, pos.z, 3, 0.35, 0.2, 0.35, 0.005);
    }

    public void clearPending(UUID uuid) {
        pending.remove(uuid);
    }

    private static long gameTime(MinecraftServer server) {
        return server == null ? 0L : server.overworld().getGameTime();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.persistFilePath = event.getServer()
                .getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve(PERSIST_FILE_NAME);
        pending.clear();
        reentryBlock.clear();
        loadPersisted();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        savePersisted();
        pending.clear();
        portals.clear();
        reentryBlock.clear();
    }

    public void reloadFromDisk() {
        pending.clear();
        reentryBlock.clear();
        loadPersisted();
    }

    private void loadPersisted() {
        portals.clear();
        if (persistFilePath == null || !java.nio.file.Files.exists(persistFilePath)) return;
        try {
            List<PersistedPortal> list = GSON.fromJson(
                    java.nio.file.Files.readString(persistFilePath, java.nio.charset.StandardCharsets.UTF_8),
                    PERSIST_TYPE);
            if (list == null) return;
            for (PersistedPortal p : list) {
                net.minecraft.resources.ResourceLocation dim =
                        net.minecraft.resources.ResourceLocation.tryParse(p.dimension());
                if (dim == null) {
                    HahUeuh.LOGGER.warn("Ignoring malformed persisted teleport portal entry");
                    continue;
                }
                portals.add(new Portal(
                        ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dim),
                        new Vec3(p.fromX(), p.fromY(), p.fromZ()),
                        new Vec3(p.toX(), p.toY(), p.toZ()),
                        p.expiry()));
            }
        } catch (java.io.IOException e) {
            HahUeuh.LOGGER.error("Failed to load persisted teleport portals from {}", persistFilePath, e);
        }
    }

    private void savePersisted() {
        if (persistFilePath == null) return;
        try {
            List<PersistedPortal> list = new ArrayList<>();
            for (Portal portal : portals) {
                list.add(new PersistedPortal(portal.dimension.location().toString(),
                        portal.from.x, portal.from.y, portal.from.z,
                        portal.to.x, portal.to.y, portal.to.z, portal.expiry));
            }
            java.nio.file.Files.createDirectories(persistFilePath.getParent());
            java.nio.file.Files.writeString(persistFilePath, GSON.toJson(list, PERSIST_TYPE),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            HahUeuh.LOGGER.error("Failed to save persisted teleport portals to {}", persistFilePath, e);
        }
    }

    private record PersistedPortal(String dimension, double fromX, double fromY, double fromZ,
                                   double toX, double toY, double toZ, long expiry) {}

    private static void actionBar(ServerPlayer player, Component text) {
        player.displayClientMessage(text, true);
    }

    private record Request(Vec3 destination, boolean portal) {}

    private record Portal(ResourceKey<Level> dimension, Vec3 from, Vec3 to, long expiry) {}
}
