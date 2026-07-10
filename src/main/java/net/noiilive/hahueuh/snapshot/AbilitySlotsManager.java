package net.noiilive.hahueuh.snapshot;

import net.noiilive.hahueuh.HahUeuhAbilities;
import net.noiilive.hahueuh.network.AbilitySlotsData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

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

public final class AbilitySlotsManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "hahueuh_ability_slots.json";
    private static final Type STORE_TYPE = new TypeToken<Map<String, StoredSlots>>() {}.getType();

    private final Map<UUID, StoredSlots> data = new ConcurrentHashMap<>();
    private Path filePath;

    public void load(MinecraftServer server) {
        data.clear();
        filePath = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        if (!Files.exists(filePath)) return;
        try {
            Map<String, StoredSlots> raw = GSON.fromJson(Files.readString(filePath, StandardCharsets.UTF_8), STORE_TYPE);
            if (raw == null) return;
            raw.forEach((key, value) -> {
                try {
                    data.put(UUID.fromString(key), migrate(value));
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Ignoring malformed ability-slots UUID '{}'", key);
                }
            });
        } catch (IOException e) {
            LOGGER.error("Failed to load ability slot bindings from {}", filePath, e);
        }
    }

    public AbilitySlotsData get(UUID uuid) {
        StoredSlots stored = data.get(uuid);
        return stored == null ? AbilitySlotsData.empty() : stored.toData();
    }

    public void update(UUID uuid, AbilitySlotsData incoming) {
        data.put(uuid, StoredSlots.from(incoming));
        save();
    }

    private void save() {
        if (filePath == null) return;
        try {
            Map<String, StoredSlots> raw = new HashMap<>();
            data.forEach((uuid, slots) -> raw.put(uuid.toString(), slots));
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, GSON.toJson(raw, STORE_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to save ability slot bindings to {}", filePath, e);
        }
    }

    private static StoredSlots migrate(StoredSlots stored) {
        if (stored == null || stored.slots == null) return stored;
        for (int i = 0; i < stored.slots.length; i++) {
            String raw = stored.slots[i];
            if (raw == null || raw.isEmpty()) continue;
            if (!raw.contains(":")) {
                stored.slots[i] = legacyIdFor(raw);
                continue;
            }
            if (raw.equals(HahUeuhAbilities.SLOTH_HAND_ABILITY.toString())) {
                stored.slots[i] = HahUeuhAbilities.SUMMON_HAND_ABILITY.toString();
            }
        }
        return stored;
    }

    private static String legacyIdFor(String enumName) {
        return switch (enumName) {
            case "RETURN_BY_DEATH" -> HahUeuhAbilities.RETURN_BY_DEATH_ABILITY.toString();
            case "SLOTH_HAND" -> HahUeuhAbilities.SLOTH_HAND_ABILITY.toString();
            default -> "";
        };
    }

    private static final class StoredSlots {
        String[] slots = new String[AbilitySlotsData.SLOT_COUNT];
        int cycleGroup;
        boolean hudHidden;

        AbilitySlotsData toData() {
            List<String> list = new ArrayList<>(AbilitySlotsData.SLOT_COUNT);
            for (String s : slots) list.add(s == null ? "" : s);
            return new AbilitySlotsData(list, cycleGroup, hudHidden);
        }

        static StoredSlots from(AbilitySlotsData data) {
            StoredSlots s = new StoredSlots();
            List<String> incoming = data.slots();
            for (int i = 0; i < s.slots.length; i++) {
                s.slots[i] = i < incoming.size() ? incoming.get(i) : "";
            }
            s.cycleGroup = data.cycleGroup();
            s.hudHidden = data.hudHidden();
            return s;
        }
    }
}
