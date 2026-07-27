package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DomainStatePayload(boolean active, double x, double y, double z, double radius,
                                 ResourceLocation dimension) implements CustomPacketPayload {

    public static final Type<DomainStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "domain_state"));

    public static final DomainStatePayload INACTIVE =
            new DomainStatePayload(false, 0, 0, 0, 0, ResourceLocation.withDefaultNamespace("overworld"));

    public static final StreamCodec<FriendlyByteBuf, DomainStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.active);
                buf.writeDouble(p.x);
                buf.writeDouble(p.y);
                buf.writeDouble(p.z);
                buf.writeDouble(p.radius);
                buf.writeResourceLocation(p.dimension);
            },
            buf -> new DomainStatePayload(
                    buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readResourceLocation())
    );

    @Override
    public Type<DomainStatePayload> type() {
        return TYPE;
    }
}
