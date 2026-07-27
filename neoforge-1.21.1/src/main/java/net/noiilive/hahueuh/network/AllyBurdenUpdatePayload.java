package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record AllyBurdenUpdatePayload(float selfWeight, Map<UUID, Float> allyWeights) implements CustomPacketPayload {

    public static final Type<AllyBurdenUpdatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "ally_tracker_burden_update"));

    public static final StreamCodec<FriendlyByteBuf, AllyBurdenUpdatePayload> STREAM_CODEC =
            StreamCodec.of(AllyBurdenUpdatePayload::write, AllyBurdenUpdatePayload::read);

    private static void write(FriendlyByteBuf buf, AllyBurdenUpdatePayload payload) {
        buf.writeFloat(payload.selfWeight);
        buf.writeVarInt(payload.allyWeights.size());
        payload.allyWeights.forEach((uuid, weight) -> {
            buf.writeUUID(uuid);
            buf.writeFloat(weight);
        });
    }

    private static AllyBurdenUpdatePayload read(FriendlyByteBuf buf) {
        float selfWeight = buf.readFloat();
        int count = buf.readVarInt();
        Map<UUID, Float> weights = new LinkedHashMap<>(count);
        for (int i = 0; i < count; i++) {
            UUID uuid = buf.readUUID();
            weights.put(uuid, buf.readFloat());
        }
        return new AllyBurdenUpdatePayload(selfWeight, weights);
    }

    @Override
    public Type<AllyBurdenUpdatePayload> type() {
        return TYPE;
    }
}
