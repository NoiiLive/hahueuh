package com.example.hahueuh.client;

import com.example.hahueuh.HahUeuh;
import com.example.hahueuh.HahUeuhAbilities;
import com.example.hahueuh.api.Ability;
import com.example.hahueuh.api.AbilityRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AbilitySlots {
    public static final int SLOT_COUNT = 9;
    public static final int GROUP_SIZE = 3;
    public static final int GROUP_COUNT = SLOT_COUNT / GROUP_SIZE;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final ResourceLocation[] slots = new ResourceLocation[SLOT_COUNT];
    private static int cycleGroup;
    private static boolean hudHidden;
    private static boolean loaded;

    private AbilitySlots() {}

    public static Ability get(int index) {
        ensureLoaded();
        ResourceLocation id = slots[index];
        return id == null ? null : AbilityRegistry.get(id).orElse(null);
    }

    public static void bind(int index, Ability ability) {
        ensureLoaded();
        slots[index] = ability.id();
        save();
    }

    public static void unbind(int index) {
        ensureLoaded();
        slots[index] = null;
        save();
    }

    public static int cycleGroup() {
        ensureLoaded();
        return cycleGroup;
    }

    public static void advanceCycleGroup() {
        ensureLoaded();
        cycleGroup = (cycleGroup + 1) % GROUP_COUNT;
        save();
    }

    public static boolean hudHidden() {
        ensureLoaded();
        return hudHidden;
    }

    public static void toggleHudHidden() {
        ensureLoaded();
        hudHidden = !hudHidden;
        save();
    }


    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("hahueuh_ability_slots.json");
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        Path path = file();
        if (!Files.exists(path)) return;
        try {
            Data data = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), Data.class);
            if (data == null) return;
            boolean migrated = false;
            if (data.slots != null) {
                for (int i = 0; i < SLOT_COUNT && i < data.slots.length; i++) {
                    String raw = data.slots[i];
                    if (raw == null) continue;
                    if (!raw.contains(":")) {
                        raw = legacyIdFor(raw);
                        migrated = true;
                        if (raw == null) continue;
                    }
                    try {
                        slots[i] = ResourceLocation.parse(raw);
                    } catch (Exception ignored) {
                    }
                }
            }
            cycleGroup = Math.floorMod(data.cycleGroup, GROUP_COUNT);
            hudHidden = data.hudHidden;
            if (migrated) save();
        } catch (Exception e) {
            HahUeuh.LOGGER.warn("Failed to load ability slot bindings", e);
        }
    }

    private static String legacyIdFor(String enumName) {
        return switch (enumName) {
            case "DOMAIN" -> HahUeuhAbilities.DOMAIN_ABILITY.toString();
            case "RETURN_BY_DEATH" -> HahUeuhAbilities.RETURN_BY_DEATH_ABILITY.toString();
            case "SLOTH_HAND" -> HahUeuhAbilities.SLOTH_HAND_ABILITY.toString();
            default -> null;
        };
    }

    private static void save() {
        try {
            Data data = new Data();
            for (int i = 0; i < SLOT_COUNT; i++) {
                data.slots[i] = slots[i] == null ? null : slots[i].toString();
            }
            data.cycleGroup = cycleGroup;
            data.hudHidden = hudHidden;
            Files.writeString(file(), GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (Exception e) {
            HahUeuh.LOGGER.warn("Failed to save ability slot bindings", e);
        }
    }

    private static final class Data {
        String[] slots = new String[SLOT_COUNT];
        int cycleGroup;
        boolean hudHidden;
    }
}
