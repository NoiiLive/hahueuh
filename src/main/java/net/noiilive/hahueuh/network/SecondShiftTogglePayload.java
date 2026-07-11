package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SecondShiftTogglePayload() implements CustomPacketPayload {
    public static final SecondShiftTogglePayload INSTANCE = new SecondShiftTogglePayload();

    public static final Type<SecondShiftTogglePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "second_shift_toggle"));

    public static final StreamCodec<ByteBuf, SecondShiftTogglePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<SecondShiftTogglePayload> type() {
        return TYPE;
    }
}
