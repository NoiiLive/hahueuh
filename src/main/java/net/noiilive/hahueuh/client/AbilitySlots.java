package net.noiilive.hahueuh.client;

import net.noiilive.hahueuh.api.Ability;
import net.noiilive.hahueuh.api.AbilityRegistry;
import net.noiilive.hahueuh.network.AbilitySlotsData;
import net.noiilive.hahueuh.network.AbilitySlotsUpdatePayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class AbilitySlots {
    public static final int SLOT_COUNT = AbilitySlotsData.SLOT_COUNT;
    public static final int GROUP_SIZE = 3;
    public static final int GROUP_COUNT = SLOT_COUNT / GROUP_SIZE;

    private static final ResourceLocation[] slots = new ResourceLocation[SLOT_COUNT];
    private static int cycleGroup;
    private static boolean hudHidden;

    private AbilitySlots() {}

    public static void reset() {
        for (int i = 0; i < SLOT_COUNT; i++) slots[i] = null;
        cycleGroup = 0;
        hudHidden = false;
    }

    public static void applyFromServer(AbilitySlotsData data) {
        List<String> raw = data.slots();
        for (int i = 0; i < SLOT_COUNT; i++) {
            String id = i < raw.size() ? raw.get(i) : null;
            slots[i] = (id == null || id.isEmpty()) ? null : ResourceLocation.tryParse(id);
        }
        cycleGroup = Math.floorMod(data.cycleGroup(), GROUP_COUNT);
        hudHidden = data.hudHidden();
    }

    public static Ability get(int index) {
        ResourceLocation id = slots[index];
        if (id == null) return null;
        Ability ability = AbilityRegistry.get(id).orElse(null);
        return (ability != null && ability.isAvailable()) ? ability : null;
    }

    public static void bind(int index, Ability ability) {
        slots[index] = ability.id();
        syncToServer();
    }

    public static void unbind(int index) {
        slots[index] = null;
        syncToServer();
    }

    public static int cycleGroup() {
        return cycleGroup;
    }

    public static void advanceCycleGroup() {
        cycleGroup = (cycleGroup + 1) % GROUP_COUNT;
        syncToServer();
    }

    public static boolean hudHidden() {
        return hudHidden;
    }

    public static void toggleHudHidden() {
        hudHidden = !hudHidden;
        syncToServer();
    }

    private static void syncToServer() {
        List<String> raw = new ArrayList<>(SLOT_COUNT);
        for (ResourceLocation id : slots) raw.add(id == null ? "" : id.toString());
        PacketDistributor.sendToServer(new AbilitySlotsUpdatePayload(new AbilitySlotsData(raw, cycleGroup, hudHidden)));
    }
}
