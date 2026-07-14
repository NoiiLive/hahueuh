package net.noiilive.hahueuh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.noiilive.hahueuh.network.AbilityCooldownPayload;
import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.LittleKingHighlightPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
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

public final class LittleKing {
    private static final ResourceLocation HEART_DEBT_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "little_king_heart_debt");
    private static final String FILE_NAME = "hahueuh_little_king.json";
    private static final double IMPLANT_REACH = 6.0;
    private static final double MIN_MAX_HEALTH = 1.0; // half a heart
    private static final int HEARTBEAT_INTERVAL_TICKS = 60;
    private static final int HIGHLIGHT_REFRESH_INTERVAL_TICKS = 10;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type STORE_TYPE = new TypeToken<Map<String, List<String>>>() {}.getType();

    private final Map<UUID, List<UUID>> implants = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> cooldownUntilTick = new ConcurrentHashMap<>();
    private final Map<UUID, List<Integer>> lastSentHighlight = new ConcurrentHashMap<>();
    private MinecraftServer server;
    private Path filePath;

    public int totalImplanted(UUID king) {
        List<UUID> list = implants.get(king);
        return list == null ? 0 : list.size();
    }

    public int activeHeartCount(ServerPlayer king) {
        List<UUID> list = implants.get(king.getUUID());
        if (list == null || list.isEmpty() || server == null) return 0;
        double range = ConfigGreed.LITTLE_KING_RANGE_BLOCKS.getAsInt();
        double rangeSq = range * range;
        int count = 0;
        for (UUID target : list) {
            Entity e = findEntity(target);
            if (e == null || !e.isAlive() || e.level() != king.level()) continue;
            if (e.distanceToSqr(king) <= rangeSq) count++;
        }
        return count;
    }

    public boolean isIndefinite(ServerPlayer king) {
        if (!ConfigGreed.LITTLE_KING_INDEFINITE.get()) return false;
        int total = totalImplanted(king.getUUID());
        if (total == 0) return false;
        if (activeHeartCount(king) != total) return false;
        return king.getMaxHealth() <= MIN_MAX_HEALTH + 1.0e-4;
    }

    public void implant(ServerPlayer king) {
        if (server == null) return;
        UUID uuid = king.getUUID();

        if (!HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().canUseGreed(uuid)
                || HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager().getGreedVariant(uuid) != GreedVariant.LIONSHEART) {
            king.displayClientMessage(Component.translatable("hahueuh.message.no_greed_authority")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (king.isShiftKeyDown()) {
            releaseImplant(king);
            return;
        }

        int remainingCooldown = king.isCreative() ? 0 : cooldownRemainingTicks(uuid);
        if (remainingCooldown > 0) {
            int seconds = (int) Math.ceil(remainingCooldown / 20.0);
            king.displayClientMessage(Component.translatable("hahueuh.message.little_king_cooldown", seconds)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        if (king.getMaxHealth() - 1.0 < MIN_MAX_HEALTH) {
            king.displayClientMessage(Component.translatable("hahueuh.message.little_king_no_hearts")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        LivingEntity target = raycastTarget(king);
        if (target == null) {
            king.displayClientMessage(Component.translatable("hahueuh.message.little_king_no_target")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        List<UUID> list = implants.computeIfAbsent(uuid, k -> new ArrayList<>());
        list.add(target.getUUID());
        reapplyHeartDebt(king);
        save();

        if (!king.isCreative()) {
            int cooldownSeconds = ConfigGreed.LITTLE_KING_COOLDOWN_SECONDS.getAsInt();
            if (cooldownSeconds > 0) {
                cooldownUntilTick.put(uuid, server.getTickCount() + HahUeuh.GREED_COMPAT.scaleCooldownTicks(uuid, cooldownSeconds * 20));
                PacketDistributor.sendToPlayer(king,
                        new AbilityCooldownPayload(HahUeuhAbilities.LITTLE_KING_ABILITY, HahUeuh.GREED_COMPAT.scaleCooldownTicks(uuid, cooldownSeconds * 20)));
            }
        }

        king.level().playSound(null, king.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.3f, 0.7f);
        king.displayClientMessage(Component.translatable("hahueuh.message.little_king_implanted", list.size())
                .withStyle(ChatFormatting.GOLD), true);
        sendHighlight(king);
    }

    private void releaseImplant(ServerPlayer king) {
        UUID uuid = king.getUUID();
        List<UUID> list = implants.get(uuid);
        LivingEntity target = raycastLiving(king, e -> true);

        if (list == null || list.isEmpty() || target == null || !list.remove(target.getUUID())) {
            king.displayClientMessage(Component.translatable("hahueuh.message.little_king_no_heart_here")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        reapplyHeartDebt(king);
        save();
        king.level().playSound(null, king.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.3f, 0.7f);
        king.displayClientMessage(Component.translatable("hahueuh.message.little_king_heart_returned", list.size())
                .withStyle(ChatFormatting.GOLD), true);
        sendHighlight(king);
    }

    public boolean crushImplant(Entity target) {
        if (server == null || implants.isEmpty()) return false;
        UUID targetUuid = target.getUUID();
        boolean any = false;
        for (Map.Entry<UUID, List<UUID>> entry : implants.entrySet()) {
            List<UUID> list = entry.getValue();
            int before = list.size();
            list.removeIf(targetUuid::equals);
            if (list.size() == before) continue;
            any = true;
            ServerPlayer king = server.getPlayerList().getPlayer(entry.getKey());
            if (king != null) {
                reapplyHeartDebt(king);
                king.displayClientMessage(Component.translatable("hahueuh.message.little_king_heart_crushed", list.size())
                        .withStyle(ChatFormatting.RED), true);
                sendHighlight(king);
            }
        }
        if (any) save();
        return any;
    }

    private LivingEntity raycastTarget(ServerPlayer king) {
        return raycastLiving(king, LittleKing::isImplantable);
    }

    private LivingEntity raycastLiving(ServerPlayer king, java.util.function.Predicate<Entity> filter) {
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(king,
                e -> e != king && e.isAlive() && !e.isSpectator() && e instanceof LivingEntity && filter.test(e), IMPLANT_REACH);
        if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private static boolean isImplantable(Entity e) {
        if (!(e instanceof LivingEntity)) return false;
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        for (String allowed : ConfigGreed.LITTLE_KING_IMPLANTABLE_ENTITIES.get()) {
            if (typeId.equals(ResourceLocation.tryParse(allowed))) return true;
        }
        return false;
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (server == null || implants.isEmpty()) return;
        UUID dead = event.getEntity().getUUID();
        for (Map.Entry<UUID, List<UUID>> entry : implants.entrySet()) {
            List<UUID> list = entry.getValue();
            int before = list.size();
            list.removeIf(dead::equals);
            int returned = before - list.size();
            if (returned <= 0) continue;
            ServerPlayer king = server.getPlayerList().getPlayer(entry.getKey());
            if (king != null) {
                reapplyHeartDebt(king);
                king.displayClientMessage(Component.translatable("hahueuh.message.little_king_heart_returned", list.size())
                        .withStyle(ChatFormatting.GOLD), true);
                sendHighlight(king);
            }
        }
        save();
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
        implants.clear();
        cooldownUntilTick.clear();
        lastSentHighlight.clear();
        this.server = null;
        this.filePath = null;
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer king) {
            reapplyHeartDebt(king);
            lastSentHighlight.remove(king.getUUID());
            sendHighlight(king);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer king) {
            reapplyHeartDebt(king);
            lastSentHighlight.remove(king.getUUID());
            sendHighlight(king);
        }
    }

    @SubscribeEvent
    public void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer king) {
            lastSentHighlight.remove(king.getUUID());
            sendHighlight(king);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (server == null || implants.isEmpty()) return;
        int tick = server.getTickCount();
        boolean heartbeatPulse = tick % HEARTBEAT_INTERVAL_TICKS == 0;
        boolean refreshHighlight = tick % HIGHLIGHT_REFRESH_INTERVAL_TICKS == 0;
        Set<UUID> beatenThisPulse = heartbeatPulse ? new HashSet<>() : null;

        for (Map.Entry<UUID, List<UUID>> entry : new ArrayList<>(implants.entrySet())) {
            List<UUID> list = entry.getValue();
            if (list.isEmpty()) continue;
            if (heartbeatPulse) {
                for (UUID target : list) {
                    if (!beatenThisPulse.add(target)) continue;
                    Entity e = findEntity(target);
                    if (e != null && e.isAlive()) {
                        e.level().playSound(null, e.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                                SoundSource.HOSTILE, 0.18f, 1.0f);
                    }
                }
            }
            if (refreshHighlight) {
                ServerPlayer king = server.getPlayerList().getPlayer(entry.getKey());
                if (king != null) sendHighlight(king);
            }
        }
    }

    public void releaseAllImplants(UUID kingUuid) {
        if (implants.remove(kingUuid) == null) return;
        save();
        if (server == null) return;
        ServerPlayer king = server.getPlayerList().getPlayer(kingUuid);
        if (king != null) {
            reapplyHeartDebt(king);
            lastSentHighlight.remove(kingUuid);
            sendHighlight(king);
        }
    }

    public void refreshAllOnRollback() {
        if (server == null) return;
        load();
        cooldownUntilTick.clear();
        for (ServerPlayer king : server.getPlayerList().getPlayers()) {
            reapplyHeartDebt(king);
            lastSentHighlight.remove(king.getUUID());
            sendHighlight(king);
        }
    }

    private void reapplyHeartDebt(ServerPlayer king) {
        AttributeInstance inst = king.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) return;
        int count = totalImplanted(king.getUUID());
        if (count <= 0) {
            inst.removeModifier(HEART_DEBT_MODIFIER_ID);
        } else {
            inst.addOrUpdateTransientModifier(
                    new AttributeModifier(HEART_DEBT_MODIFIER_ID, -count, AttributeModifier.Operation.ADD_VALUE));
        }
        if (king.getHealth() > king.getMaxHealth()) {
            king.setHealth(king.getMaxHealth());
        }
    }

    private void sendHighlight(ServerPlayer king) {
        List<UUID> list = implants.get(king.getUUID());
        List<Integer> ids = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            Set<UUID> seen = new HashSet<>();
            for (UUID target : list) {
                if (!seen.add(target)) continue;
                Entity e = findEntity(target);
                if (e != null && e.isAlive() && e.level() == king.level()) {
                    ids.add(e.getId());
                }
            }
        }
        List<Integer> previous = lastSentHighlight.get(king.getUUID());
        if (ids.equals(previous)) return;
        lastSentHighlight.put(king.getUUID(), ids);
        PacketDistributor.sendToPlayer(king, new LittleKingHighlightPayload(ids));
    }

    private Entity findEntity(UUID id) {
        if (server == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) return e;
        }
        return null;
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
            PacketDistributor.sendToPlayer(player, new AbilityCooldownPayload(HahUeuhAbilities.LITTLE_KING_ABILITY, remaining));
        }
    }

    private void load() {
        implants.clear();
        if (filePath == null || !Files.exists(filePath)) return;
        try {
            Map<String, List<String>> raw = GSON.fromJson(Files.readString(filePath, StandardCharsets.UTF_8), STORE_TYPE);
            if (raw == null) return;
            raw.forEach((kingStr, targets) -> {
                try {
                    UUID king = UUID.fromString(kingStr);
                    List<UUID> parsed = new ArrayList<>();
                    if (targets != null) {
                        for (String t : targets) {
                            try {
                                parsed.add(UUID.fromString(t));
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                    if (!parsed.isEmpty()) implants.put(king, parsed);
                } catch (IllegalArgumentException e) {
                    HahUeuh.LOGGER.warn("Ignoring malformed Little King UUID '{}'", kingStr);
                }
            });
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to load Little King data from {}", filePath, e);
        }
    }

    private void save() {
        if (filePath == null) return;
        try {
            Map<String, List<String>> raw = new HashMap<>();
            implants.forEach((king, targets) -> {
                if (targets.isEmpty()) return;
                List<String> strs = new ArrayList<>(targets.size());
                for (UUID t : targets) strs.add(t.toString());
                raw.put(king.toString(), strs);
            });
            Files.writeString(filePath, GSON.toJson(raw, STORE_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            HahUeuh.LOGGER.error("Failed to save Little King data to {}", filePath, e);
        }
    }
}
