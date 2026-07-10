package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AbilitySlotsSyncPayload(AbilitySlotsData data) implements CustomPacketPayload {
    public static final Type<AbilitySlotsSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "ability_slots_sync"));

    public static final StreamCodec<ByteBuf, AbilitySlotsSyncPayload> STREAM_CODEC =
            AbilitySlotsData.STREAM_CODEC.map(AbilitySlotsSyncPayload::new, AbilitySlotsSyncPayload::data);

    @Override
    public Type<AbilitySlotsSyncPayload> type() {
        return TYPE;
    }
}
