package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.AbilityCooldownPayload;
import net.noiilive.hahueuh.network.LionsHeartStatePayload;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class LionsHeart {
    private static final int WARNING_LEAD_TICKS = 20;
    private static final int BURNOUT_DAMAGE_INTERVAL_TICKS = 5;
    private static final float BURNOUT_DAMAGE_MIN = 4f;
    private static final float BURNOUT_DAMAGE_MAX = 20f;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PERSIST_FILE_NAME = "hahueuh_lions_heart.json";
    private static final Type PERSIST_TYPE = new TypeToken<Map<String, PersistedActivation>>() {}.getType();

    private record ActivationState(int startTick, int durationTicks) {}
    private record PersistedActivation(int durationTicks, int elapsedTicks) {}

    private final Map<UUID, ActivationState> activations = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> cooldownUntilTick = new ConcurrentHashMap<>();
    private final Set<UUID> burnoutInProgress = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> dangerAnchorTick = new ConcurrentHashMap<>();
    private final Map<UUID, PersistedActivation> persistedActivations = new ConcurrentHashMap<>();
    private MinecraftServer server;
    private Path persistFilePath;

    public boolean isActive(UUID uuid) {
        return activations.containsKey(uuid);
    }

    private static int rollDurationTicks() {
        int min = ConfigGreed.LIONS_HEART_DURATION_MIN_SECONDS.getAsInt();
        int max = ConfigGreed.LIONS_HEART_DURATION_MAX_SECONDS.getAsInt();
        if (max < min) max = min;
        int seconds = min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
        return seconds * 20;
    }

    public void forceResetOnRollback(ServerPlayer player) {
        UUID uuid = player.getUUID();
        burnoutInProgress.remove(uuid);
        dangerAnchorTick.remove(uuid);
        if (activations.remove(uuid) != null) {
            PacketDistributor.sendToPlayer(player, new LionsHeartStatePayload(false));
        }
    }

    public void toggle(ServerPlayer player) {
        if (server == null) return;
        UUID uuid = player.getUUID();

        if (isActive(uuid)) {
            deactivate(player, "hahueuh.message.lions_heart_deactivated");
            return;
        }

        if (!HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().canUseGreed(uuid)) {
            player.displayClientMessage(Component.translatable("hahueuh.message.no_greed_authority")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        int remainingCooldown = player.isCreative() ? 0 : cooldownRemainingTicks(uuid);
        if (remainingCooldown > 0) {
            int seconds = (int) Math.ceil(remainingCooldown / 20.0);
            player.displayClientMessage(Component.translatable("hahueuh.message.lions_heart_cooldown", seconds)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        activate(player);
    }

    private void activate(ServerPlayer player) {
        activations.put(player.getUUID(), new ActivationState(server.getTickCount(), rollDurationTicks()));
        PacketDistributor.sendToPlayer(player, new LionsHeartStatePayload(true));
        player.level().playSound(null, player, ModSounds.LIONSHEART_ACTIVATE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        player.displayClientMessage(Component.translatable("hahueuh.message.lions_heart_activated")
                .withStyle(ChatFormatting.GOLD), true);
    }

    private void deactivate(ServerPlayer player, String messageKey) {
        UUID uuid = player.getUUID();
        activations.remove(uuid);
        dangerAnchorTick.remove(uuid);
        PacketDistributor.sendToPlayer(player, new LionsHeartStatePayload(false));
        player.level().playSound(null, player, ModSounds.LIONSHEART_DEACTIVATE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

        if (!player.isCreative()) {
            int cooldownSeconds = ConfigGreed.LIONS_HEART_COOLDOWN_SECONDS.getAsInt();
            if (cooldownSeconds > 0) {
                cooldownUntilTick.put(uuid, server.getTickCount() + cooldownSeconds * 20);
                PacketDistributor.sendToPlayer(player,
                        new AbilityCooldownPayload(HahUeuhAbilities.LIONS_HEART_ABILITY, cooldownSeconds * 20));
            }
        }

        player.displayClientMessage(Component.translatable(messageKey).withStyle(ChatFormatting.GOLD), true);
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
            PacketDistributor.sendToPlayer(player, new AbilityCooldownPayload(HahUeuhAbilities.LIONS_HEART_ABILITY, remaining));
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
        activations.clear();
        cooldownUntilTick.clear();
        burnoutInProgress.clear();
        dangerAnchorTick.clear();
        this.server = null;
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || server == null) return;
        UUID uuid = player.getUUID();
        ActivationState state = activations.remove(uuid);
        dangerAnchorTick.remove(uuid);
        burnoutInProgress.remove(uuid);
        if (state == null) return;

        int elapsed = server.getTickCount() - state.startTick();
        persistedActivations.put(uuid, new PersistedActivation(state.durationTicks(), elapsed));
        savePersisted();
    }

    public void restoreOnLogin(ServerPlayer player) {
        if (server == null) return;
        UUID uuid = player.getUUID();
        PersistedActivation persisted = persistedActivations.remove(uuid);
        if (persisted == null) return;
        savePersisted();

        activations.put(uuid, new ActivationState(server.getTickCount() - persisted.elapsedTicks(), persisted.durationTicks()));
        PacketDistributor.sendToPlayer(player, new LionsHeartStatePayload(true));
    }

    public void reloadPersisted() {
        loadPersisted();
    }

    private void loadPersisted() {
        persistedActivations.clear();
        if (persistFilePath == null || !Files.exists(persistFilePath)) return;
        try {
            Map<String, PersistedActivation> raw = GSON.fromJson(Files.readString(persistFilePath, StandardCharsets.UTF_8), PERSIST_TYPE);
            if (raw == null) return;
            raw.forEach((key, value) -> {
                try {
                    persistedActivations.put(UUID.fromString(key), value);
                } catch (IllegalArgumentException e) {
                    HahUeuh.LOGGER.warn("Ignoring malformed Lion's Heart persisted UUID '{}'", key);
                }
            });
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to load persisted Lion's Heart activations from {}", persistFilePath, e);
        }
    }

    private void savePersisted() {
        if (persistFilePath == null) return;
        try {
            Map<String, PersistedActivation> raw = new HashMap<>();
            persistedActivations.forEach((uuid, state) -> raw.put(uuid.toString(), state));
            Files.createDirectories(persistFilePath.getParent());
            Files.writeString(persistFilePath, GSON.toJson(raw, PERSIST_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to save persisted Lion's Heart activations to {}", persistFilePath, e);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (server == null || activations.isEmpty()) return;
        for (Map.Entry<UUID, ActivationState> entry : new ArrayList<>(activations.entrySet())) {
            UUID uuid = entry.getKey();
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                activations.remove(uuid);
                continue;
            }
            tickActive(player, entry.getValue());
        }
    }

    private void tickActive(ServerPlayer player, ActivationState state) {
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20f);
        player.setAirSupply(player.getMaxAirSupply());
        player.clearFire();
        player.setTicksFrozen(0);

        UUID uuid = player.getUUID();

        if (HahUeuh.LITTLE_KING.isIndefinite(player)) {
            dangerAnchorTick.remove(uuid);
            return;
        }

        int base = state.durationTicks();
        int effectiveDuration = base + HahUeuh.LITTLE_KING.activeHeartCount(player) * base;
        int elapsed = server.getTickCount() - state.startTick();
        int warningAtElapsed = effectiveDuration - WARNING_LEAD_TICKS;

        if (elapsed < warningAtElapsed) {
            dangerAnchorTick.remove(uuid);
            return;
        }

        Integer anchor = dangerAnchorTick.get(uuid);
        if (anchor == null) {
            dangerAnchorTick.put(uuid, server.getTickCount());
            player.displayClientMessage(Component.translatable("hahueuh.message.lions_heart_strain")
                    .withStyle(ChatFormatting.YELLOW), true);
            return;
        }

        int sinceAnchor = server.getTickCount() - anchor;
        if (sinceAnchor < WARNING_LEAD_TICKS) return;

        int burnoutElapsed = sinceAnchor - WARNING_LEAD_TICKS;
        if (burnoutElapsed % BURNOUT_DAMAGE_INTERVAL_TICKS == 0) {
            float progress = Math.min(1f, (float) burnoutElapsed / effectiveDuration);
            float damage = BURNOUT_DAMAGE_MIN + progress * (BURNOUT_DAMAGE_MAX - BURNOUT_DAMAGE_MIN);
            burnoutInProgress.add(uuid);
            player.hurt(player.damageSources().indirectMagic(player, player), damage);
            burnoutInProgress.remove(uuid);
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isActive(player.getUUID())) return;
        UUID uuid = player.getUUID();
        activations.remove(uuid);
        burnoutInProgress.remove(uuid);
        dangerAnchorTick.remove(uuid);
        PacketDistributor.sendToPlayer(player, new LionsHeartStatePayload(false));
        player.level().playSound(null, player, ModSounds.LIONSHEART_DEACTIVATE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        if (server != null && !player.isCreative()) {
            int cooldownSeconds = ConfigGreed.LIONS_HEART_COOLDOWN_SECONDS.getAsInt();
            if (cooldownSeconds > 0) {
                cooldownUntilTick.put(uuid, server.getTickCount() + cooldownSeconds * 20);
                PacketDistributor.sendToPlayer(player,
                        new AbilityCooldownPayload(HahUeuhAbilities.LIONS_HEART_ABILITY, cooldownSeconds * 20));
            }
        }
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer target && isActive(target.getUUID())
                && !burnoutInProgress.contains(target.getUUID())) {
            event.setCanceled(true);
            return;
        }

        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer attackingPlayer && isActive(attackingPlayer.getUUID())) {
            event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> 0f);
            event.addReductionModifier(DamageContainer.Reduction.ENCHANTMENTS, (container, reduction) -> 0f);
        }
    }

    @SubscribeEvent
    public void onShieldBlock(LivingShieldBlockEvent event) {
        Entity attacker = event.getDamageSource().getEntity();
        if (attacker instanceof ServerPlayer attackingPlayer && isActive(attackingPlayer.getUUID())) {
            event.setBlocked(false);
        }
    }
}
