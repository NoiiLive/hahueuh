package net.noiilive.hahueuh.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.noiilive.hahueuh.HahUeuh;

public record MurakFlightTogglePayload(boolean wantsFlight) implements CustomPacketPayload {
    public static final Type<MurakFlightTogglePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "murak_flight_toggle"));

    public static final StreamCodec<ByteBuf, MurakFlightTogglePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(MurakFlightTogglePayload::new, MurakFlightTogglePayload::wantsFlight);

    @Override
    public Type<MurakFlightTogglePayload> type() {
        return TYPE;
    }
}
