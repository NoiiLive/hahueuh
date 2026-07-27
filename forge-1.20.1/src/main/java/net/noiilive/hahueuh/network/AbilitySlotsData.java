package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record AbilitySlotsData(List<String> slots, int cycleGroup, boolean hudHidden) {
    public static final int SLOT_COUNT = 9;

    public static AbilitySlotsData empty() {
        return new AbilitySlotsData(new ArrayList<>(Collections.nCopies(SLOT_COUNT, "")), 0, false);
    }

    public static AbilitySlotsData decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> slots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            slots.add(buf.readUtf());
        }
        return new AbilitySlotsData(slots, buf.readVarInt(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(slots.size());
        for (String slot : slots) {
            buf.writeUtf(slot);
        }
        buf.writeVarInt(cycleGroup);
        buf.writeBoolean(hudHidden);
    }
}
