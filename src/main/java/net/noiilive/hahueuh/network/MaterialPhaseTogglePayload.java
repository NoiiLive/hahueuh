package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MaterialPhaseTogglePayload() implements CustomPacketPayload {
    public static final MaterialPhaseTogglePayload INSTANCE = new MaterialPhaseTogglePayload();

    public static final Type<MaterialPhaseTogglePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "material_phase_toggle"));

    public static final StreamCodec<ByteBuf, MaterialPhaseTogglePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<MaterialPhaseTogglePayload> type() {
        return TYPE;
    }
}
