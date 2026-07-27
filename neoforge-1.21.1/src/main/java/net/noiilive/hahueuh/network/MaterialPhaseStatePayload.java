package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MaterialPhaseStatePayload(boolean active) implements CustomPacketPayload {
    public static final Type<MaterialPhaseStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "material_phase_state"));

    public static final StreamCodec<ByteBuf, MaterialPhaseStatePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(MaterialPhaseStatePayload::new, MaterialPhaseStatePayload::active);

    @Override
    public Type<MaterialPhaseStatePayload> type() {
        return TYPE;
    }
}
