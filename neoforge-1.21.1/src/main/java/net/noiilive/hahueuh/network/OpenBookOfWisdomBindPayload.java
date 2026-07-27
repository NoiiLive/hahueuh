package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenBookOfWisdomBindPayload() implements CustomPacketPayload {
    public static final OpenBookOfWisdomBindPayload INSTANCE = new OpenBookOfWisdomBindPayload();

    public static final Type<OpenBookOfWisdomBindPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "open_book_of_wisdom_bind"));

    public static final StreamCodec<FriendlyByteBuf, OpenBookOfWisdomBindPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<OpenBookOfWisdomBindPayload> type() {
        return TYPE;
    }
}
