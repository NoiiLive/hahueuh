package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SecondShiftStatePayload(boolean active) implements CustomPacketPayload {
    public static final Type<SecondShiftStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "second_shift_state"));

    public static final StreamCodec<ByteBuf, SecondShiftStatePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(SecondShiftStatePayload::new, SecondShiftStatePayload::active);

    @Override
    public Type<SecondShiftStatePayload> type() {
        return TYPE;
    }
}
