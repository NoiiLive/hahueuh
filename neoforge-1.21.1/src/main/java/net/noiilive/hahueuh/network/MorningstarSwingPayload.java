package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MorningstarSwingPayload(boolean spin) implements CustomPacketPayload {
    public static final Type<MorningstarSwingPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "morningstar_swing"));

    public static final StreamCodec<ByteBuf, MorningstarSwingPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, MorningstarSwingPayload::spin,
                    MorningstarSwingPayload::new);

    @Override
    public Type<MorningstarSwingPayload> type() {
        return TYPE;
    }
}
