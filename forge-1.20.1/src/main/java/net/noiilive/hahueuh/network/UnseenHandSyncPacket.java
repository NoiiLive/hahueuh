package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class UnseenHandSyncPacket {
    private final UUID owner;
    private final int entityId;
    private final boolean active;
    private final float distance;
    private final int mode;
    private final int variant;
    private final boolean mobility;
    private final int count;

    public UnseenHandSyncPacket(UUID owner, int entityId, boolean active, float distance,
                                int mode, int variant, boolean mobility, int count) {
        this.owner = owner;
        this.entityId = entityId;
        this.active = active;
        this.distance = distance;
        this.mode = mode;
        this.variant = variant;
        this.mobility = mobility;
        this.count = count;
    }

    public UnseenHandSyncPacket(FriendlyByteBuf buf) {
        this.owner = buf.readUUID();
        this.entityId = buf.readVarInt();
        this.active = buf.readBoolean();
        this.distance = buf.readFloat();
        this.mode = buf.readVarInt();
        this.variant = buf.readVarInt();
        this.mobility = buf.readBoolean();
        this.count = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(owner);
        buf.writeVarInt(entityId);
        buf.writeBoolean(active);
        buf.writeFloat(distance);
        buf.writeVarInt(mode);
        buf.writeVarInt(variant);
        buf.writeBoolean(mobility);
        buf.writeVarInt(count);
    }

    public static void handle(UnseenHandSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                RemoteUnseenHands.update(packet.owner, packet.entityId, packet.active, packet.distance,
                        packet.mode, packet.variant, packet.mobility, packet.count)));
        ctx.setPacketHandled(true);
    }
}
