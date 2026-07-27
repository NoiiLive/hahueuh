package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AttackStrengthSyncPayload(boolean offhandNext, float scale) implements CustomPacketPayload {
    public static final Type<AttackStrengthSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "attack_strength_sync"));

    public static final StreamCodec<ByteBuf, AttackStrengthSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AttackStrengthSyncPayload::offhandNext,
            ByteBufCodecs.FLOAT, AttackStrengthSyncPayload::scale,
            AttackStrengthSyncPayload::new);

    @Override
    public Type<AttackStrengthSyncPayload> type() {
        return TYPE;
    }
}
