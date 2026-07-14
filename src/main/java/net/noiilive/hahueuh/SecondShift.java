package net.noiilive.hahueuh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.noiilive.hahueuh.network.AbilityCooldownPayload;
import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.SecondShiftStatePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SecondShift {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PERSIST_FILE_NAME = "hahueuh_second_shift.json";
    private static final Type PERSIST_TYPE = new TypeToken<Set<String>>() {}.getType();

    private final Set<UUID> active = ConcurrentHashMap.newKeySet();
    private final Set<UUID> persistedActive = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> cooldownUntilTick = new ConcurrentHashMap<>();
    private MinecraftServer server;
    private Path persistFilePath;
    private boolean redistributing;

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

        if (HahUeuh.BASE_SHIFT.isActive(uuid)) {
            king.displayClientMessage(Component.translatable("hahueuh.message.second_shift_conflict")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        int remainingCooldown = king.isCreative() ? 0 : cooldownRemainingTicks(uuid);
        if (remainingCooldown > 0) {
            int seconds = (int) Math.ceil(remainingCooldown / 20.0);
            king.displayClientMessage(Component.translatable("hahueuh.message.second_shift_cooldown", seconds)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        active.add(uuid);
        PacketDistributor.sendToPlayer(king, new SecondShiftStatePayload(true));
        king.displayClientMessage(Component.translatable("hahueuh.message.second_shift_activated")
                .withStyle(ChatFormatting.GOLD), true);
    }

    private void deactivate(ServerPlayer king, boolean startCooldown) {
        UUID uuid = king.getUUID();
        if (!active.remove(uuid)) return;
        PacketDistributor.sendToPlayer(king, new SecondShiftStatePayload(false));
        if (startCooldown) {
            startCooldown(king);
        }
        king.displayClientMessage(Component.translatable("hahueuh.message.second_shift_deactivated")
                .withStyle(ChatFormatting.GOLD), true);
    }

    public void forceResetOnRollback(ServerPlayer king) {
        if (active.remove(king.getUUID())) {
            PacketDistributor.sendToPlayer(king, new SecondShiftStatePayload(false));
        }
    }

    public Set<UUID> captureActive() {
        return new HashSet<>(active);
    }

    public void restoreActiveOnRollback(Set<UUID> activeAtSnapshot) {
        if (server == null) return;
        for (ServerPlayer king : server.getPlayerList().getPlayers()) {
            UUID uuid = king.getUUID();
            boolean want = activeAtSnapshot.contains(uuid);
            if (want == active.contains(uuid)) continue;
            if (want) {
                active.add(uuid);
                PacketDistributor.sendToPlayer(king, new SecondShiftStatePayload(true));
            } else {
                active.remove(uuid);
                PacketDistributor.sendToPlayer(king, new SecondShiftStatePayload(false));
            }
        }
        boolean changed = false;
        for (UUID uuid : activeAtSnapshot) {
            if (server.getPlayerList().getPlayer(uuid) == null) changed |= persistedActive.add(uuid);
        }
        if (changed) savePersisted();
    }

    private UUID findSharingKing(UUID entityUuid) {
        if (server == null || active.isEmpty()) return null;
        for (UUID kingUuid : active) {
            if (kingUuid.equals(entityUuid) || HahUeuh.ALLY_TRACKER.isAlly(kingUuid, entityUuid)) {
                return kingUuid;
            }
        }
        return null;
    }

    private Map<UUID, LivingEntity> participantsOf(UUID kingUuid) {
        Map<UUID, LivingEntity> participants = new HashMap<>();
        ServerPlayer king = server.getPlayerList().getPlayer(kingUuid);
        if (king != null) participants.put(kingUuid, king);
        for (LivingEntity ally : HahUeuh.ALLY_TRACKER.getLiveAllies(kingUuid)) {
            participants.put(ally.getUUID(), ally);
        }
        return participants;
    }

    @SubscribeEvent
    public void onSharedDamage(LivingDamageEvent.Pre event) {
        if (redistributing) return;
        float amount = event.getNewDamage();
        if (amount <= 0f) return;

        LivingEntity source = event.getEntity();
        UUID kingUuid = findSharingKing(source.getUUID());
        if (kingUuid == null) return;

        event.setNewDamage(0f);
        Map<UUID, Double> weights = HahUeuh.ALLY_TRACKER.effectiveWeights(kingUuid);
        Map<UUID, LivingEntity> participants = participantsOf(kingUuid);

        boolean prev = redistributing;
        redistributing = true;
        try {
            for (Map.Entry<UUID, Double> entry : weights.entrySet()) {
                float share = (float) (amount * (entry.getValue() / 100.0));
                if (share <= 0f) continue;

                LivingEntity participant = participants.get(entry.getKey());
                if (participant == null) {
                    HahUeuh.ALLY_TRACKER.applyToMob(entry.getKey(), mob -> applyShareGuarded(mob, share));
                    continue;
                }
                if (!participant.isAlive()) continue;

                hurtWithoutAggro(participant, share);
                if (participant != source && participant instanceof ServerPlayer sp) {
                    sp.displayClientMessage(Component.translatable("hahueuh.message.base_shift_received_damage",
                            String.format("%.1f", share), source.getName().getString()).withStyle(ChatFormatting.RED), true);
                }
            }
        } finally {
            redistributing = prev;
        }
    }

    private void applyShareGuarded(LivingEntity mob, float share) {
        boolean prev = redistributing;
        redistributing = true;
        try {
            hurtWithoutAggro(mob, share);
        } finally {
            redistributing = prev;
        }
    }

    private static final Field LAST_DAMAGE_SOURCE_FIELD = findField("lastDamageSource");
    private static final Field LAST_DAMAGE_STAMP_FIELD = findField("lastDamageStamp");

    private static Field findField(String name) {
        try {
            Field field = LivingEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static void hurtWithoutAggro(LivingEntity participant, float amount) {
        DamageSource previousLastDamageSource = getField(LAST_DAMAGE_SOURCE_FIELD, participant);
        Long previousLastDamageStamp = getField(LAST_DAMAGE_STAMP_FIELD, participant);

        if (participant instanceof Mob mob) {
            LivingEntity previousTarget = mob.getTarget();
            LivingEntity previousHurtBy = mob.getLastHurtByMob();
            participant.hurt(participant.damageSources().indirectMagic(participant, participant), amount);
            mob.setTarget(previousTarget);
            mob.setLastHurtByMob(previousHurtBy);
        } else {
            participant.hurt(participant.damageSources().indirectMagic(participant, participant), amount);
        }

        setField(LAST_DAMAGE_SOURCE_FIELD, participant, previousLastDamageSource);
        setField(LAST_DAMAGE_STAMP_FIELD, participant, previousLastDamageStamp);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Field field, LivingEntity target) {
        try {
            return (T) field.get(target);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Field field, LivingEntity target, Object value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SubscribeEvent
    public void onSharedEffectApplicable(MobEffectEvent.Applicable event) {
        if (redistributing) return;
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null) return;

        LivingEntity source = event.getEntity();
        UUID kingUuid = findSharingKing(source.getUUID());
        if (kingUuid == null) return;

        event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        Map<UUID, Double> weights = HahUeuh.ALLY_TRACKER.effectiveWeights(kingUuid);
        Map<UUID, LivingEntity> participants = participantsOf(kingUuid);

        int originalLevel = instance.getAmplifier() + 1;
        int originalDuration = instance.getDuration();
        double minShare = ConfigGreed.SECOND_SHIFT_MIN_EFFECT_SHARE.getAsDouble();

        boolean prev = redistributing;
        redistributing = true;
        try {
            for (Map.Entry<UUID, Double> entry : weights.entrySet()) {
                double weightFraction = entry.getValue() / 100.0;
                if (weightFraction <= 0.0) continue;

                Integer awardedAmplifier = splitAmplifier(originalLevel, weightFraction, minShare);
                if (awardedAmplifier == null) continue; // diluted below the floor — nullified for them

                int awardedDuration = originalDuration < 0 ? originalDuration
                        : Math.max(1, (int) Math.round(originalDuration * weightFraction));

                MobEffectInstance shared = new MobEffectInstance(instance.getEffect(), awardedDuration, awardedAmplifier,
                        instance.isAmbient(), instance.isVisible(), instance.showIcon());

                LivingEntity participant = participants.get(entry.getKey());
                if (participant == null) {
                    HahUeuh.ALLY_TRACKER.applyToMob(entry.getKey(), mob -> applyEffectGuarded(mob, shared));
                    continue;
                }
                if (!participant.isAlive()) continue;

                participant.addEffect(shared);
                if (participant != source && participant instanceof ServerPlayer sp) {
                    sp.displayClientMessage(Component.translatable("hahueuh.message.base_shift_received_effect",
                            effectDisplayName(shared), source.getName().getString()).withStyle(ChatFormatting.RED), true);
                }
            }
        } finally {
            redistributing = prev;
        }
    }

    private void applyEffectGuarded(LivingEntity mob, MobEffectInstance shared) {
        boolean prev = redistributing;
        redistributing = true;
        try {
            mob.addEffect(new MobEffectInstance(shared));
        } finally {
            redistributing = prev;
        }
    }

    private static Integer splitAmplifier(int originalLevel, double weightFraction, double minShare) {
        if (minShare <= 0.0) return originalLevel - 1;
        int roundsNeeded = (int) Math.ceil(minShare / weightFraction - 1.0e-9);
        if (roundsNeeded > originalLevel) return null;
        int awardedLevel = originalLevel - roundsNeeded + 1;
        return awardedLevel - 1;
    }

    private static Component effectDisplayName(MobEffectInstance instance) {
        Component name = Component.translatable(instance.getEffect().value().getDescriptionId());
        return instance.getAmplifier() > 0 ? name.copy().append(" " + (instance.getAmplifier() + 1)) : name;
    }

    private void startCooldown(ServerPlayer king) {
        if (server == null || king.isCreative()) return;
        int cooldownSeconds = ConfigGreed.BASE_SHIFT_COOLDOWN_SECONDS.getAsInt();
        if (cooldownSeconds <= 0) return;
        cooldownUntilTick.put(king.getUUID(), server.getTickCount() + HahUeuh.GREED_COMPAT.scaleCooldownTicks(king.getUUID(), cooldownSeconds * 20));
        PacketDistributor.sendToPlayer(king,
                new AbilityCooldownPayload(HahUeuhAbilities.SECOND_SHIFT_ABILITY, HahUeuh.GREED_COMPAT.scaleCooldownTicks(king.getUUID(), cooldownSeconds * 20)));
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
            PacketDistributor.sendToPlayer(player, new AbilityCooldownPayload(HahUeuhAbilities.SECOND_SHIFT_ABILITY, remaining));
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
        PacketDistributor.sendToPlayer(king, new SecondShiftStatePayload(true));
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
                    HahUeuh.LOGGER.warn("Ignoring malformed Second Shift persisted UUID '{}'", key);
                }
            }
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to load persisted Second Shift state from {}", persistFilePath, e);
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
            HahUeuh.LOGGER.error("Failed to save persisted Second Shift state to {}", persistFilePath, e);
        }
    }
}
