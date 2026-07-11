package net.noiilive.hahueuh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.noiilive.hahueuh.network.AbilityCooldownPayload;
import net.noiilive.hahueuh.network.BaseShiftStatePayload;
import net.noiilive.hahueuh.network.GreedVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
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

public final class BaseShift {
    private static final int SAFETY_NET_INTERVAL_TICKS = 20;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PERSIST_FILE_NAME = "hahueuh_base_shift.json";
    private static final Type PERSIST_TYPE = new TypeToken<Set<String>>() {}.getType();

    private final Set<UUID> active = ConcurrentHashMap.newKeySet();
    private final Set<UUID> persistedActive = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> cooldownUntilTick = new ConcurrentHashMap<>();
    private MinecraftServer server;
    private Path persistFilePath;

    public boolean isActive(UUID uuid) {
        return active.contains(uuid);
    }

    public void toggle(ServerPlayer king) {
        if (server == null) return;
        UUID uuid = king.getUUID();

        if (active.contains(uuid)) {
            deactivate(king, true);
            return;
        }

        if (!HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().canUseGreed(uuid)
                || HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().getGreedVariant(uuid) != GreedVariant.CORLEONIS) {
            king.displayClientMessage(Component.translatable("hahueuh.message.no_greed_authority")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (HahUeuh.SECOND_SHIFT.isActive(uuid)) {
            king.displayClientMessage(Component.translatable("hahueuh.message.base_shift_conflict")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        int remainingCooldown = king.isCreative() ? 0 : cooldownRemainingTicks(uuid);
        if (remainingCooldown > 0) {
            int seconds = (int) Math.ceil(remainingCooldown / 20.0);
            king.displayClientMessage(Component.translatable("hahueuh.message.base_shift_cooldown", seconds)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        active.add(uuid);
        PacketDistributor.sendToPlayer(king, new BaseShiftStatePayload(true));
        king.displayClientMessage(Component.translatable("hahueuh.message.base_shift_activated")
                .withStyle(ChatFormatting.GOLD), true);

        for (LivingEntity ally : HahUeuh.ALLY_TRACKER.getLiveAllies(uuid)) {
            syncAlly(king, ally, true);
        }
    }

    private void deactivate(ServerPlayer king, boolean startCooldown) {
        UUID uuid = king.getUUID();
        if (!active.remove(uuid)) return;
        PacketDistributor.sendToPlayer(king, new BaseShiftStatePayload(false));
        if (startCooldown) {
            startCooldown(king);
        }
        king.displayClientMessage(Component.translatable("hahueuh.message.base_shift_deactivated")
                .withStyle(ChatFormatting.GOLD), true);
    }

    public void forceResetOnRollback(ServerPlayer king) {
        if (active.remove(king.getUUID())) {
            PacketDistributor.sendToPlayer(king, new BaseShiftStatePayload(false));
        }
    }

    private ServerPlayer findShieldingKing(UUID entityUuid) {
        if (server == null || active.isEmpty()) return null;
        for (UUID kingUuid : active) {
            if (!HahUeuh.ALLY_TRACKER.isAlly(kingUuid, entityUuid)) continue;
            ServerPlayer king = server.getPlayerList().getPlayer(kingUuid);
            if (king != null) return king;
        }
        return null;
    }

    @SubscribeEvent
    public void onAllyDamage(LivingDamageEvent.Pre event) {
        float amount = event.getNewDamage();
        if (amount <= 0f) return;
        LivingEntity ally = event.getEntity();
        ServerPlayer king = findShieldingKing(ally.getUUID());
        if (king == null || king == ally) return;

        event.setNewDamage(0f);
        king.hurt(king.damageSources().indirectMagic(king, king), amount);
        king.displayClientMessage(Component.translatable("hahueuh.message.base_shift_received_damage",
                String.format("%.1f", amount), ally.getName().getString()).withStyle(ChatFormatting.RED), true);
    }

    @SubscribeEvent
    public void onAllyEffectApplicable(MobEffectEvent.Applicable event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || isBeneficial(instance)) return;
        LivingEntity ally = event.getEntity();
        ServerPlayer king = findShieldingKing(ally.getUUID());
        if (king == null || king == ally) return;

        event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        king.addEffect(new MobEffectInstance(instance));
        king.displayClientMessage(Component.translatable("hahueuh.message.base_shift_received_effect",
                effectDisplayName(instance), ally.getName().getString()).withStyle(ChatFormatting.RED), true);
    }

    private static boolean isBeneficial(MobEffectInstance instance) {
        return instance.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL;
    }

    private static Component effectDisplayName(MobEffectInstance instance) {
        Component name = Component.translatable(instance.getEffect().value().getDescriptionId());
        return instance.getAmplifier() > 0 ? name.copy().append(" " + (instance.getAmplifier() + 1)) : name;
    }

    private void syncAlly(ServerPlayer king, LivingEntity ally, boolean announce) {
        float missing = ally.getMaxHealth() - ally.getHealth();
        if (missing > 0.01f) {
            ally.setHealth(ally.getMaxHealth());
            king.hurt(king.damageSources().indirectMagic(king, king), missing);
            if (announce) {
                king.displayClientMessage(Component.translatable("hahueuh.message.base_shift_received_damage",
                        String.format("%.1f", missing), ally.getName().getString()).withStyle(ChatFormatting.RED), true);
            }
        }

        for (MobEffectInstance instance : new ArrayList<>(ally.getActiveEffects())) {
            if (isBeneficial(instance)) continue;
            ally.removeEffect(instance.getEffect());
            king.addEffect(new MobEffectInstance(instance));
            if (announce) {
                king.displayClientMessage(Component.translatable("hahueuh.message.base_shift_received_effect",
                        effectDisplayName(instance), ally.getName().getString()).withStyle(ChatFormatting.RED), true);
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (server == null || active.isEmpty()) return;
        if (server.getTickCount() % SAFETY_NET_INTERVAL_TICKS != 0) return;
        for (UUID uuid : new ArrayList<>(active)) {
            ServerPlayer king = server.getPlayerList().getPlayer(uuid);
            if (king == null) {
                active.remove(uuid);
                continue;
            }
            for (LivingEntity ally : HahUeuh.ALLY_TRACKER.getLiveAllies(uuid)) {
                syncAlly(king, ally, false);
            }
        }
    }

    private void startCooldown(ServerPlayer king) {
        if (server == null || king.isCreative()) return;
        int cooldownSeconds = ConfigGreed.BASE_SHIFT_COOLDOWN_SECONDS.getAsInt();
        if (cooldownSeconds <= 0) return;
        cooldownUntilTick.put(king.getUUID(), server.getTickCount() + cooldownSeconds * 20);
        PacketDistributor.sendToPlayer(king,
                new AbilityCooldownPayload(HahUeuhAbilities.BASE_SHIFT_ABILITY, cooldownSeconds * 20));
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
            PacketDistributor.sendToPlayer(player, new AbilityCooldownPayload(HahUeuhAbilities.BASE_SHIFT_ABILITY, remaining));
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
        cooldownUntilTick.clear();
        this.server = null;
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer king)) return;
        UUID uuid = king.getUUID();
        if (active.remove(uuid)) {
            persistedActive.add(uuid);
            savePersisted();
        }
    }

    public void restoreOnLogin(ServerPlayer king) {
        if (server == null) return;
        UUID uuid = king.getUUID();
        if (!persistedActive.remove(uuid)) return;
        savePersisted();

        active.add(uuid);
        PacketDistributor.sendToPlayer(king, new BaseShiftStatePayload(true));
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
                    HahUeuh.LOGGER.warn("Ignoring malformed Base Shift persisted UUID '{}'", key);
                }
            }
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to load persisted Base Shift state from {}", persistFilePath, e);
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
            HahUeuh.LOGGER.error("Failed to save persisted Base Shift state to {}", persistFilePath, e);
        }
    }

}
