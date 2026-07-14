package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BookOfWisdomTogglePayload() implements CustomPacketPayload {
    public static final BookOfWisdomTogglePayload INSTANCE = new BookOfWisdomTogglePayload();

    public static final Type<BookOfWisdomTogglePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "book_of_wisdom_toggle"));

    public static final StreamCodec<ByteBuf, BookOfWisdomTogglePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<BookOfWisdomTogglePayload> type() {
        return TYPE;
    }
}
