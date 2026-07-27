package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AbilitySlotsUpdatePayload(AbilitySlotsData data) implements CustomPacketPayload {
    public static final Type<AbilitySlotsUpdatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "ability_slots_update"));

    public static final StreamCodec<ByteBuf, AbilitySlotsUpdatePayload> STREAM_CODEC =
            AbilitySlotsData.STREAM_CODEC.map(AbilitySlotsUpdatePayload::new, AbilitySlotsUpdatePayload::data);

    @Override
    public Type<AbilitySlotsUpdatePayload> type() {
        return TYPE;
    }
}
