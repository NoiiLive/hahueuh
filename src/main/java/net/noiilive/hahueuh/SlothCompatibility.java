package net.noiilive.hahueuh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SlothCompatibility {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();
    static final String FILE_NAME = "hahueuh_sloth_compat.json";

    private static final int VEHICLE_TICKS_PER_AWARD = 600;

    private final Map<UUID, Integer> score = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> vehicleTicks = new ConcurrentHashMap<>();
    private MinecraftServer server;
    private Path filePath;

    private static boolean enabled() { return ConfigSloth.SLOTH_COMPAT_ENABLED.get(); }
    private static int threshold() { return ConfigSloth.SLOTH_COMPAT_THRESHOLD.getAsInt(); }

    public boolean isCompatible(UUID uuid) {
        return !enabled() || score.getOrDefault(uuid, 0) >= threshold();
    }

    public int getScore(UUID uuid) {
        return score.getOrDefault(uuid, 0);
    }

    public void setScore(UUID uuid, int value) {
        score.put(uuid, Math.max(0, value));
        save();
    }


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
        this.filePath = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        reload();
    }

    public void reload() {
        if (filePath == null) return;
        score.clear();
        vehicleTicks.clear();
        if (Files.exists(filePath)) {
            try {
                Map<String, Integer> raw = GSON.fromJson(Files.readString(filePath, StandardCharsets.UTF_8), MAP_TYPE);
                if (raw != null) raw.forEach((k, v) -> {
                    try { score.put(UUID.fromString(k), v); } catch (IllegalArgumentException ignored) {}
                });
            } catch (IOException e) {
                LOGGER.error("Failed to load Sloth compatibility data", e);
            }
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        save();
        this.server = null;
    }

    private void save() {
        if (filePath == null) return;
        try {
            Map<String, Integer> raw = new HashMap<>();
            score.forEach((uuid, v) -> raw.put(uuid.toString(), v));
            Files.writeString(filePath, GSON.toJson(raw, MAP_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to save Sloth compatibility data", e);
        }
    }

    private void addScore(ServerPlayer player, int points, String reason) {
        if (!enabled() || points <= 0) return;
        UUID id = player.getUUID();
        int old = score.getOrDefault(id, 0);
        if (old >= threshold()) return;
        int now = Math.min(threshold(), old + points);
        score.put(id, now);
        save();
        player.displayClientMessage(Component.translatable("hahueuh.message.sloth_attunement",
                now - old, now, threshold(), Component.translatable(reason)).withStyle(ChatFormatting.DARK_PURPLE), true);
        if (now >= threshold()) {
            player.sendSystemMessage(Component.translatable("hahueuh.message.sloth_compatible")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }


    @SubscribeEvent
    public void onTamedKill(LivingDeathEvent event) {
        if (!enabled() || server == null) return;
        Entity killer = event.getSource().getEntity();
        if (killer == null || killer == event.getEntity()) return;
        if (killer instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            ServerPlayer owner = server.getPlayerList().getPlayer(ownable.getOwnerUUID());
            if (owner != null) addScore(owner, ConfigSloth.SLOTH_POINTS_TAMED_KILL.getAsInt(), "hahueuh.reason.companion_kill");
        }
    }

    @SubscribeEvent
    public void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!enabled() || server == null) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getDayTime() % 24000L < 2000L) {
            addScore(player, ConfigSloth.SLOTH_POINTS_NIGHT_SLEEP.getAsInt(), "hahueuh.reason.night_sleep");
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!enabled() || server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Entity vehicle = player.getVehicle();
            boolean ridingTransport = vehicle instanceof AbstractMinecart || vehicle instanceof Boat
                    || vehicle instanceof AbstractHorse || vehicle instanceof Pig;
            if (ridingTransport && vehicle.getDeltaMovement().horizontalDistanceSqr() > 0.0025) {
                int t = vehicleTicks.merge(player.getUUID(), 1, Integer::sum);
                if (t >= VEHICLE_TICKS_PER_AWARD) {
                    vehicleTicks.put(player.getUUID(), 0);
                    addScore(player, ConfigSloth.SLOTH_POINTS_VEHICLE_TRAVEL.getAsInt(), "hahueuh.reason.vehicle_travel");
                }
            }
        }
    }


    public void applyDrawbacks(ServerPlayer player) {
        if (!enabled() || server == null) return;
        int threshold = threshold();
        int current = score.getOrDefault(player.getUUID(), 0);
        if (threshold <= 0 || current >= threshold) return;
        double pct = (double) current / threshold;

        boolean blindness, nausea, hunger;
        int damageIntervalTicks;

        if (pct < 0.25) {
            blindness = true;  nausea = true;  hunger = true; damageIntervalTicks = 20;
        } else if (pct < 0.50) {
            blindness = false; nausea = true;  hunger = true; damageIntervalTicks = 20;
        } else if (pct < 0.75) {
            blindness = false; nausea = false; hunger = true; damageIntervalTicks = 60;
        } else if (pct < 0.90) {
            blindness = false; nausea = false; hunger = true; damageIntervalTicks = 100;
        } else {
            blindness = false; nausea = false; hunger = true; damageIntervalTicks = 0;
        }

        if (blindness) player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, true));
        if (hunger)    player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 1, false, false, true));
        if (nausea)    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, false, true));
        if (damageIntervalTicks > 0 && server.getTickCount() % damageIntervalTicks == 0) {
            player.hurt(player.damageSources().magic(), 1.0f);
        }
    }
}
