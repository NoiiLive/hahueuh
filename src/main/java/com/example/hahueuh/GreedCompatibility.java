package com.example.hahueuh;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
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

public final class GreedCompatibility {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();
    static final String FILE_NAME = "hahueuh_greed_compat.json";

    private final Map<UUID, Integer> score = new ConcurrentHashMap<>();
    private Path filePath;

    public int getScore(UUID uuid) {
        return score.getOrDefault(uuid, 0);
    }

    public void setScore(UUID uuid, int value) {
        score.put(uuid, Math.max(0, value));
        save();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.filePath = event.getServer().getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        reload();
    }

    public void reload() {
        if (filePath == null) return;
        score.clear();
        if (Files.exists(filePath)) {
            try {
                Map<String, Integer> raw = GSON.fromJson(Files.readString(filePath, StandardCharsets.UTF_8), MAP_TYPE);
                if (raw != null) raw.forEach((k, v) -> {
                    try { score.put(UUID.fromString(k), v); } catch (IllegalArgumentException ignored) {}
                });
            } catch (IOException e) {
                LOGGER.error("Failed to load Greed compatibility data", e);
            }
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        save();
        filePath = null;
    }

    private void save() {
        if (filePath == null) return;
        try {
            Map<String, Integer> raw = new HashMap<>();
            score.forEach((uuid, v) -> raw.put(uuid.toString(), v));
            Files.writeString(filePath, GSON.toJson(raw, MAP_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to save Greed compatibility data", e);
        }
    }
}
