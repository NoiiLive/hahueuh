package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.noiilive.hahueuh.api.AbilityCooldowns;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AbilityCooldownPayload(ResourceLocation abilityId, int remainingTicks) implements CustomPacketPayload {
    public static final Type<AbilityCooldownPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "ability_cooldown"));

    public static final StreamCodec<ByteBuf, AbilityCooldownPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, AbilityCooldownPayload::abilityId,
            ByteBufCodecs.VAR_INT, AbilityCooldownPayload::remainingTicks,
            AbilityCooldownPayload::new);

    @Override
    public Type<AbilityCooldownPayload> type() {
        return TYPE;
    }
}
