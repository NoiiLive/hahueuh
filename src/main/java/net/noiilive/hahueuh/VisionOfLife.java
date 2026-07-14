package net.noiilive.hahueuh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.noiilive.hahueuh.network.AbilityCooldownPayload;
import net.noiilive.hahueuh.network.BoundVisionAbility;
import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.VisionOfLifeGlowPayload;
import net.noiilive.hahueuh.network.VisionOfLifeStatePayload;
import net.noiilive.hahueuh.network.WitchFactorAuthority;
import net.noiilive.hahueuh.snapshot.PlayerAuthorityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VisionOfLife {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PERSIST_FILE_NAME = "hahueuh_vision_of_life.json";
    private static final Type PERSIST_TYPE = new TypeToken<Set<String>>() {}.getType();

    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final double SCAN_RADIUS = 64.0;

    private final Set<UUID> active = ConcurrentHashMap.newKeySet();
    private final Set<UUID> activatedViaBookOfWisdom = ConcurrentHashMap.newKeySet();
    private final Set<UUID> persistedActive = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> cooldownUntilTick = new ConcurrentHashMap<>();
    private final Map<UUID, VisionOfLifeGlowPayload> lastSentGlow = new ConcurrentHashMap<>();
    private MinecraftServer server;
    private Path persistFilePath;

    public boolean isActive(UUID uuid) {
        return active.contains(uuid);
    }

    public void toggle(ServerPlayer player) {
        if (server == null) return;
        UUID uuid = player.getUUID();

        if (active.contains(uuid)) {
            deactivate(player, true);
            return;
        }

        if (!HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().canUseGreed(uuid)
                || HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().getGreedVariant(uuid) != GreedVariant.ECHIDNA) {
            player.displayClientMessage(Component.translatable("hahueuh.message.no_greed_authority")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (!HahUeuh.BOOK_OF_WISDOM.isHoldingCredentialFor(player, BoundVisionAbility.VISION_OF_LIFE)) {
            player.displayClientMessage(Component.translatable("hahueuh.message.echidna_needs_book")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        int remainingCooldown = player.isCreative() ? 0 : cooldownRemainingTicks(uuid);
        if (remainingCooldown > 0) {
            int seconds = (int) Math.ceil(remainingCooldown / 20.0);
            player.displayClientMessage(Component.translatable("hahueuh.message.vision_of_life_cooldown", seconds)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        active.add(uuid);
        PacketDistributor.sendToPlayer(player, new VisionOfLifeStatePayload(true));
        HahUeuh.FOOTPRINT_TRACKER.syncNow(player);
        player.displayClientMessage(Component.translatable("hahueuh.message.vision_of_life_activated")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    public void toggleViaBookOfWisdom(ServerPlayer player) {
        if (server == null) return;
        UUID uuid = player.getUUID();

        if (active.contains(uuid)) {
            deactivate(player, true);
            return;
        }

        active.add(uuid);
        activatedViaBookOfWisdom.add(uuid);
        PacketDistributor.sendToPlayer(player, new VisionOfLifeStatePayload(true));
        HahUeuh.FOOTPRINT_TRACKER.syncNow(player);
        player.displayClientMessage(Component.translatable("hahueuh.message.vision_of_life_activated")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    private void deactivate(ServerPlayer player, boolean startCooldown) {
        deactivate(player, startCooldown, "hahueuh.message.vision_of_life_deactivated", ChatFormatting.LIGHT_PURPLE);
    }

    private void deactivateForLostBook(ServerPlayer player) {
        String messageKey = activatedViaBookOfWisdom.contains(player.getUUID())
                ? "hahueuh.message.book_of_wisdom_lost" : "hahueuh.message.echidna_lost_book";
        deactivate(player, true, messageKey, ChatFormatting.RED);
    }

    private void deactivate(ServerPlayer player, boolean startCooldown, String messageKey, ChatFormatting messageColor) {
        UUID uuid = player.getUUID();
        if (!active.remove(uuid)) return;
        boolean viaBook = activatedViaBookOfWisdom.remove(uuid);
        PacketDistributor.sendToPlayer(player, new VisionOfLifeStatePayload(false));
        if (startCooldown) {
            if (viaBook) {
                HahUeuh.BOOK_OF_WISDOM_COPY.startCooldownForWielder(player, BoundVisionAbility.VISION_OF_LIFE);
            } else {
                startCooldown(player);
            }
        }
        lastSentGlow.remove(uuid);
        PacketDistributor.sendToPlayer(player, new VisionOfLifeGlowPayload(List.of(), List.of(), List.of(), List.of()));
        HahUeuh.FOOTPRINT_TRACKER.clearFor(player);
        player.displayClientMessage(Component.translatable(messageKey).withStyle(messageColor), true);
    }

    public void forceResetOnRollback(ServerPlayer player) {
        if (active.remove(player.getUUID())) {
            PacketDistributor.sendToPlayer(player, new VisionOfLifeStatePayload(false));
        }
        activatedViaBookOfWisdom.remove(player.getUUID());
        lastSentGlow.remove(player.getUUID());
        HahUeuh.FOOTPRINT_TRACKER.clearFor(player);
    }

    public Set<UUID> captureActive() {
        return new HashSet<>(active);
    }

    public void restoreActiveOnRollback(Set<UUID> activeAtSnapshot) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            boolean want = activeAtSnapshot.contains(uuid);
            if (want == active.contains(uuid)) continue;
            if (want) {
                active.add(uuid);
                HahUeuh.FOOTPRINT_TRACKER.syncNow(player);
                PacketDistributor.sendToPlayer(player, new VisionOfLifeStatePayload(true));
            } else {
                active.remove(uuid);
                activatedViaBookOfWisdom.remove(uuid);
                lastSentGlow.remove(uuid);
                PacketDistributor.sendToPlayer(player, new VisionOfLifeStatePayload(false));
                PacketDistributor.sendToPlayer(player, new VisionOfLifeGlowPayload(List.of(), List.of(), List.of(), List.of()));
                HahUeuh.FOOTPRINT_TRACKER.clearFor(player);
            }
        }
        boolean changed = false;
        for (UUID uuid : activeAtSnapshot) {
            if (server.getPlayerList().getPlayer(uuid) == null) changed |= persistedActive.add(uuid);
        }
        if (changed) savePersisted();
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (server == null || active.isEmpty()) return;

        boolean scanTick = server.getTickCount() % SCAN_INTERVAL_TICKS == 0;
        for (UUID uuid : new ArrayList<>(active)) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) continue;

            if (!HahUeuh.BOOK_OF_WISDOM.isHoldingCredentialFor(player, BoundVisionAbility.VISION_OF_LIFE)) {
                deactivateForLostBook(player);
                continue;
            }

            if (scanTick) {
                scanNearby(player);
            }
        }
    }

    private void scanNearby(ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(SCAN_RADIUS);
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != player);

        List<Integer> hostile = new ArrayList<>();
        List<Integer> passive = new ArrayList<>();
        List<Integer> players = new ArrayList<>();
        List<Integer> witchFactor = new ArrayList<>();
        for (LivingEntity e : nearby) {
            if (holdsWitchFactor(e)) {
                witchFactor.add(e.getId());
            } else if (e instanceof Player) {
                players.add(e.getId());
            } else if (e instanceof Enemy) {
                hostile.add(e.getId());
            } else {
                passive.add(e.getId());
            }
        }

        VisionOfLifeGlowPayload payload = new VisionOfLifeGlowPayload(hostile, passive, players, witchFactor);
        if (payload.equals(lastSentGlow.get(player.getUUID()))) return;
        lastSentGlow.put(player.getUUID(), payload);
        PacketDistributor.sendToPlayer(player, payload);
    }

    static boolean holdsWitchFactor(LivingEntity e) {
        if (e instanceof WitchFactorEntity) return true;
        if (e instanceof Player p) {
            PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
            return am.hasWitchFactorSloth(p.getUUID()) || am.hasWitchFactorGreed(p.getUUID());
        }
        return e.getData(ModAttachments.MOB_WITCH_FACTOR.get()) != WitchFactorAuthority.NONE;
    }

    private void startCooldown(ServerPlayer player) {
        if (server == null || player.isCreative()) return;
        int cooldownSeconds = ConfigGreed.VISION_OF_LIFE_COOLDOWN_SECONDS.getAsInt();
        if (cooldownSeconds <= 0) return;
        cooldownUntilTick.put(player.getUUID(), server.getTickCount() + HahUeuh.GREED_COMPAT.scaleCooldownTicks(player.getUUID(), cooldownSeconds * 20));
        PacketDistributor.sendToPlayer(player,
                new AbilityCooldownPayload(HahUeuhAbilities.VISION_OF_LIFE_ABILITY, HahUeuh.GREED_COMPAT.scaleCooldownTicks(player.getUUID(), cooldownSeconds * 20)));
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
            PacketDistributor.sendToPlayer(player, new AbilityCooldownPayload(HahUeuhAbilities.VISION_OF_LIFE_ABILITY, remaining));
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
        this.persistFilePath = server.getWorldPath(LevelResource.ROOT).resolve(PERSIST_FILE_NAME);
        loadPersisted();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        active.clear();
        activatedViaBookOfWisdom.clear();
        cooldownUntilTick.clear();
        lastSentGlow.clear();
        this.server = null;
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        if (active.remove(uuid)) {
            persistedActive.add(uuid);
            savePersisted();
        }
        lastSentGlow.remove(uuid);
    }

    public void restoreOnLogin(ServerPlayer player) {
        if (server == null) return;
        UUID uuid = player.getUUID();
        if (!persistedActive.remove(uuid)) return;
        savePersisted();

        active.add(uuid);
        PacketDistributor.sendToPlayer(player, new VisionOfLifeStatePayload(true));
    }

    public void reloadPersisted() {
        loadPersisted();
    }

    private void loadPersisted() {
        persistedActive.clear();
        if (persistFilePath == null || !Files.exists(persistFilePath)) return;
        try {
            Set<String> raw = GSON.fromJson(Files.readString(persistFilePath, StandardCharsets.UTF_8), PERSIST_TYPE);
            if (raw == null) return;
            for (String key : raw) {
                try {
                    persistedActive.add(UUID.fromString(key));
                } catch (IllegalArgumentException e) {
                    HahUeuh.LOGGER.warn("Ignoring malformed Vision of Life persisted UUID '{}'", key);
                }
            }
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to load persisted Vision of Life state from {}", persistFilePath, e);
        }
    }

    private void savePersisted() {
        if (persistFilePath == null) return;
        try {
            Set<String> raw = new HashSet<>();
            persistedActive.forEach(uuid -> raw.add(uuid.toString()));
            Files.createDirectories(persistFilePath.getParent());
            Files.writeString(persistFilePath, GSON.toJson(raw, PERSIST_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to save persisted Vision of Life state to {}", persistFilePath, e);
        }
    }
}
