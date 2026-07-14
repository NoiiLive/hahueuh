package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BindVisionAbilityPayload(int abilityOrdinal) implements CustomPacketPayload {
    public static final Type<BindVisionAbilityPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "bind_vision_ability"));

    public static final StreamCodec<FriendlyByteBuf, BindVisionAbilityPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.abilityOrdinal()),
            buf -> new BindVisionAbilityPayload(buf.readVarInt()));

    @Override
    public Type<BindVisionAbilityPayload> type() {
        return TYPE;
    }
}
