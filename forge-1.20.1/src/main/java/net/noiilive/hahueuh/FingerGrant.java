package net.noiilive.hahueuh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.noiilive.hahueuh.network.AbilityCooldownPacket;
import net.noiilive.hahueuh.network.ModNetworking;
import net.noiilive.hahueuh.capability.MobWitchFactorData;
import net.noiilive.hahueuh.capability.ModCapabilities;
import net.noiilive.hahueuh.network.FingerHighlightPacket;
import net.noiilive.hahueuh.network.SlothVariant;
import net.noiilive.hahueuh.snapshot.PlayerAuthorityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.TickEvent;


import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FingerGrant {
    private static final String FILE_NAME = "hahueuh_finger_grant.json";
    private static final double GRANT_REACH = 6.0;
    private static final int HIGHLIGHT_REFRESH_INTERVAL_TICKS = 10;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type STORE_TYPE = new TypeToken<Map<String, Map<String, Integer>>>() {}.getType();

    private final Map<UUID, Map<UUID, Integer>> grants = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> subordinateOwner = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldownUntilTick = new ConcurrentHashMap<>();
    private final Map<UUID, List<Integer>> lastSentHighlight = new ConcurrentHashMap<>();
    private MinecraftServer server;
    private Path filePath;


    public int totalGranted(UUID owner) {
        Map<UUID, Integer> map = grants.get(owner);
        if (map == null) return 0;
        int sum = 0;
        for (int v : map.values()) sum += v;
        return sum;
    }

    public int baseHandCount(UUID owner) {
        return SlothVariant.unseenHandCount(owner);
    }

    public int effectiveCount(UUID owner) {
        return Math.max(0, baseHandCount(owner) - totalGranted(owner));
    }

    public int receivedHands(UUID subordinate) {
        UUID owner = subordinateOwner.get(subordinate);
        if (owner == null) return 0;
        Map<UUID, Integer> map = grants.get(owner);
        return map == null ? 0 : map.getOrDefault(subordinate, 0);
    }

    public UUID ownerOf(UUID subordinate) {
        return subordinateOwner.get(subordinate);
    }


    public void grant(ServerPlayer owner) {
        if (server == null) return;
        UUID ownerId = owner.getUUID();
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();

        if (!am.canUseSloth(ownerId) || am.getSlothVariant(ownerId) != SlothVariant.UNSEEN_HANDS) {
            owner.displayClientMessage(Component.translatable("hahueuh.message.finger_no_authority")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (owner.isShiftKeyDown()) {
            reclaim(owner);
            return;
        }

        int remainingCooldown = owner.isCreative() ? 0 : cooldownRemainingTicks(ownerId);
        if (remainingCooldown > 0) {
            int seconds = (int) Math.ceil(remainingCooldown / 20.0);
            owner.displayClientMessage(Component.translatable("hahueuh.message.finger_cooldown", seconds)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (effectiveCount(ownerId) <= 1) {
            owner.displayClientMessage(Component.translatable("hahueuh.message.finger_min_hands")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        LivingEntity target = raycastLiving(owner);
        if (target == null) {
            owner.displayClientMessage(Component.translatable("hahueuh.message.finger_no_target")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!isSubordinate(owner, target)) {
            owner.displayClientMessage(Component.translatable("hahueuh.message.finger_not_subordinate")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        UUID subId = target.getUUID();
        UUID existingOwner = subordinateOwner.get(subId);
        if (existingOwner != null && !existingOwner.equals(ownerId)) {
            owner.displayClientMessage(Component.translatable("hahueuh.message.finger_occupied",
                    target.getName()).withStyle(ChatFormatting.RED), true);
            return;
        }

        grants.computeIfAbsent(ownerId, k -> new ConcurrentHashMap<>()).merge(subId, 1, Integer::sum);
        subordinateOwner.put(subId, ownerId);
        save();

        if (!owner.isCreative()) {
            int cooldownSeconds = ConfigSloth.FINGER_GRANT_COOLDOWN_SECONDS.get();
            if (cooldownSeconds > 0) {
                cooldownUntilTick.put(ownerId, worldTime() + cooldownSeconds * 20);
                ModNetworking.sendToPlayer(owner,
                        new AbilityCooldownPacket(HahUeuhAbilities.FINGER_GRANT_ABILITY, cooldownSeconds * 20));
            }
        }

        owner.level().playSound(null, owner.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.4f, 1.3f);
        resyncOwner(owner);
        resyncSubordinate(target);
        owner.displayClientMessage(Component.translatable("hahueuh.message.finger_granted",
                receivedHands(subId), target.getName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    private void reclaim(ServerPlayer owner) {
        UUID ownerId = owner.getUUID();
        LivingEntity target = raycastLiving(owner);
        Map<UUID, Integer> map = grants.get(ownerId);
        if (target == null || map == null || !map.containsKey(target.getUUID())) {
            owner.displayClientMessage(Component.translatable("hahueuh.message.finger_no_hand_here")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        UUID subId = target.getUUID();
        int left = map.merge(subId, -1, Integer::sum);
        if (left <= 0) {
            map.remove(subId);
            subordinateOwner.remove(subId);
            if (map.isEmpty()) grants.remove(ownerId);
        }
        save();

        owner.level().playSound(null, owner.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.4f, 0.8f);
        resyncOwner(owner);
        resyncSubordinate(target);
        owner.displayClientMessage(Component.translatable("hahueuh.message.finger_reclaimed",
                Math.max(0, left), target.getName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    private boolean isSubordinate(ServerPlayer owner, LivingEntity target) {
        if (target == owner) return false;
        if (target instanceof ServerPlayer) {
            return HahUeuh.PLAYER_ALLIES.areAllies(owner.getUUID(), target.getUUID());
        }
        return target instanceof OwnableEntity ownable && owner.getUUID().equals(ownable.getOwnerUUID());
    }

    private LivingEntity raycastLiving(ServerPlayer owner) {
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(owner,
                e -> e != owner && e.isAlive() && !e.isSpectator() && e instanceof LivingEntity, GRANT_REACH);
        if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }


    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (server == null) return;
        UUID dead = event.getEntity().getUUID();
        UUID owner = subordinateOwner.remove(dead);
        if (owner == null) return;

        int returned = 0;
        Map<UUID, Integer> map = grants.get(owner);
        if (map != null) {
            Integer had = map.remove(dead);
            returned = had == null ? 0 : had;
            if (map.isEmpty()) grants.remove(owner);
        }
        save();

        if (event.getEntity() instanceof ServerPlayer deadPlayer) {
            resyncSubordinate(deadPlayer);
        }
        ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(owner);
        if (ownerPlayer != null) {
            resyncOwner(ownerPlayer);
            ownerPlayer.displayClientMessage(Component.translatable("hahueuh.message.finger_returned",
                    returned, event.getEntity().getName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }


    private void resyncOwner(ServerPlayer owner) {
        HahUeuh.SNAPSHOT_MANAGER.sendAuthoritiesTo(owner);
        sendHighlight(owner);
    }

    private void sendHighlight(ServerPlayer owner) {
        Map<UUID, Integer> subs = grants.get(owner.getUUID());
        List<Integer> ids = new ArrayList<>();
        if (subs != null && !subs.isEmpty()) {
            for (UUID subId : subs.keySet()) {
                Entity e = findEntity(subId);
                if (e != null && e.isAlive() && e.level() == owner.level()) {
                    ids.add(e.getId());
                }
            }
        }
        ids.sort(null);
        List<Integer> previous = lastSentHighlight.get(owner.getUUID());
        if (ids.equals(previous)) return;
        lastSentHighlight.put(owner.getUUID(), ids);
        ModNetworking.sendToPlayer(owner, new FingerHighlightPacket(ids));
    }

    private Entity findEntity(UUID id) {
        if (server == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }

    private void resyncSubordinate(LivingEntity subordinate) {
        if (subordinate instanceof ServerPlayer sp) {
            HahUeuh.SNAPSHOT_MANAGER.sendAuthoritiesTo(sp);
        } else {
            subordinate.getCapability(ModCapabilities.MOB_WITCH_FACTOR).ifPresent(d -> d.setFingerHands(receivedHands(subordinate.getUUID())));
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
        grants.clear();
        subordinateOwner.clear();
        cooldownUntilTick.clear();
        lastSentHighlight.clear();
        this.server = null;
        this.filePath = null;
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            lastSentHighlight.remove(player.getUUID());
            resyncOwner(player);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            lastSentHighlight.remove(player.getUUID());
            resyncOwner(player);
        }
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            lastSentHighlight.remove(player.getUUID());
            sendHighlight(player);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (server == null || grants.isEmpty()) return;
        if (server.getTickCount() % HIGHLIGHT_REFRESH_INTERVAL_TICKS != 0) return;
        for (UUID ownerId : grants.keySet()) {
            ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
            if (owner != null) sendHighlight(owner);
        }
    }


    public void refreshAllOnRollback() {
        if (server == null) return;
        load();
        cooldownUntilTick.clear();
        lastSentHighlight.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            resyncOwner(player);
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (e instanceof ServerPlayer) continue;
                int hands = receivedHands(e.getUUID());
                if (hands > 0 || MobWitchFactorData.get(e).getFingerHands() > 0) {
                    e.getCapability(ModCapabilities.MOB_WITCH_FACTOR).ifPresent(d -> d.setFingerHands(hands));
                }
            }
        }
    }

    public Map<UUID, Integer> captureCooldownRemaining() {
        Map<UUID, Integer> result = new HashMap<>();
        if (server == null) return result;
        long tick = worldTime();
        cooldownUntilTick.forEach((uuid, until) -> {
            int remaining = (int) (until - tick);
            if (remaining > 0) result.put(uuid, remaining);
        });
        return result;
    }

    public void restoreCooldownRemaining(Map<UUID, Integer> remainingByUuid) {
        if (server == null) return;
        cooldownUntilTick.clear();
        long tick = worldTime();
        remainingByUuid.forEach((uuid, remaining) -> cooldownUntilTick.put(uuid, tick + remaining));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int remaining = remainingByUuid.getOrDefault(player.getUUID(), 0);
            ModNetworking.sendToPlayer(player, new AbilityCooldownPacket(HahUeuhAbilities.FINGER_GRANT_ABILITY, remaining));
        }
    }

    private long worldTime() {
        return server == null ? 0L : server.overworld().getGameTime();
    }

    private int cooldownRemainingTicks(UUID uuid) {
        Long until = cooldownUntilTick.get(uuid);
        if (until == null || server == null) return 0;
        return (int) Math.max(0L, until - worldTime());
    }


    private void load() {
        grants.clear();
        subordinateOwner.clear();
        if (filePath == null || !Files.exists(filePath)) return;
        try {
            Map<String, Map<String, Integer>> raw = GSON.fromJson(Files.readString(filePath, StandardCharsets.UTF_8), STORE_TYPE);
            if (raw == null) return;
            raw.forEach((ownerStr, subs) -> {
                try {
                    UUID owner = UUID.fromString(ownerStr);
                    Map<UUID, Integer> parsed = new ConcurrentHashMap<>();
                    if (subs != null) {
                        subs.forEach((subStr, hands) -> {
                            try {
                                UUID sub = UUID.fromString(subStr);
                                if (hands != null && hands > 0) {
                                    parsed.put(sub, hands);
                                    subordinateOwner.put(sub, owner);
                                }
                            } catch (IllegalArgumentException ignored) {
                            }
                        });
                    }
                    if (!parsed.isEmpty()) grants.put(owner, parsed);
                } catch (IllegalArgumentException e) {
                    HahUeuh.LOGGER.warn("Ignoring malformed Finger Grant UUID '{}'", ownerStr);
                }
            });
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to load Finger Grant data from {}", filePath, e);
        }
    }

    private void save() {
        if (filePath == null) return;
        try {
            Map<String, Map<String, Integer>> raw = new HashMap<>();
            grants.forEach((owner, subs) -> {
                if (subs.isEmpty()) return;
                Map<String, Integer> out = new HashMap<>();
                subs.forEach((sub, hands) -> out.put(sub.toString(), hands));
                raw.put(owner.toString(), out);
            });
            Files.writeString(filePath, GSON.toJson(raw, STORE_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to save Finger Grant data to {}", filePath, e);
        }
    }
}
