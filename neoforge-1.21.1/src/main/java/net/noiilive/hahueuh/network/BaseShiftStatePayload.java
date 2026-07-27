package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BaseShiftStatePayload(boolean active) implements CustomPacketPayload {
    public static final Type<BaseShiftStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "base_shift_state"));

    public static final StreamCodec<ByteBuf, BaseShiftStatePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(BaseShiftStatePayload::new, BaseShiftStatePayload::active);

    @Override
    public Type<BaseShiftStatePayload> type() {
        return TYPE;
    }
}
