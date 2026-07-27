package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class AllyBurdenUpdatePacket {
    public final float selfWeight;
    public final Map<UUID, Float> allyWeights;

    public AllyBurdenUpdatePacket(float selfWeight, Map<UUID, Float> allyWeights) {
        this.selfWeight = selfWeight;
        this.allyWeights = allyWeights;
    }

    public AllyBurdenUpdatePacket(FriendlyByteBuf buf) {
        this.selfWeight = buf.readFloat();
        int count = buf.readVarInt();
        Map<UUID, Float> weights = new LinkedHashMap<>(count);
        for (int i = 0; i < count; i++) {
            UUID uuid = buf.readUUID();
            weights.put(uuid, buf.readFloat());
        }
        this.allyWeights = weights;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(selfWeight);
        buf.writeVarInt(allyWeights.size());
        allyWeights.forEach((uuid, weight) -> {
            buf.writeUUID(uuid);
            buf.writeFloat(weight);
        });
    }

    public static void handle(AllyBurdenUpdatePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) HahUeuh.ALLY_TRACKER.updateBurden(sender, packet.selfWeight, packet.allyWeights);
        });
        ctx.setPacketHandled(true);
    }
}
