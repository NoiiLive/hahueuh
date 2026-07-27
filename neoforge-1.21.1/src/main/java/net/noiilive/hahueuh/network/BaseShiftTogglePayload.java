package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BaseShiftTogglePayload() implements CustomPacketPayload {
    public static final BaseShiftTogglePayload INSTANCE = new BaseShiftTogglePayload();

    public static final Type<BaseShiftTogglePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "base_shift_toggle"));

    public static final StreamCodec<ByteBuf, BaseShiftTogglePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<BaseShiftTogglePayload> type() {
        return TYPE;
    }
}
