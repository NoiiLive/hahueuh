package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ActivateBookOfWisdomVisionPayload() implements CustomPacketPayload {
    public static final ActivateBookOfWisdomVisionPayload INSTANCE = new ActivateBookOfWisdomVisionPayload();

    public static final Type<ActivateBookOfWisdomVisionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "activate_book_of_wisdom_vision"));

    public static final StreamCodec<FriendlyByteBuf, ActivateBookOfWisdomVisionPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<ActivateBookOfWisdomVisionPayload> type() {
        return TYPE;
    }
}
