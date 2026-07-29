package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EmtStatePayload(boolean active, double x, double y, double z, double radius,
                              ResourceLocation dimension) implements CustomPacketPayload {

    public static final Type<EmtStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "emt_state"));

    public static final EmtStatePayload INACTIVE =
            new EmtStatePayload(false, 0, 0, 0, 0, ResourceLocation.withDefaultNamespace("overworld"));

    public static final StreamCodec<FriendlyByteBuf, EmtStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.active);
                buf.writeDouble(p.x);
                buf.writeDouble(p.y);
                buf.writeDouble(p.z);
                buf.writeDouble(p.radius);
                buf.writeResourceLocation(p.dimension);
            },
            buf -> new EmtStatePayload(
                    buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readResourceLocation())
    );

    @Override
    public Type<EmtStatePayload> type() {
        return TYPE;
    }
}
