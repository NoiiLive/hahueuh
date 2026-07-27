package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class UnseenHandGrabSyncPacket {
    private final UUID owner;
    private final List<Integer> grabbed;

    public UnseenHandGrabSyncPacket(UUID owner, List<Integer> grabbed) {
        this.owner = owner;
        this.grabbed = grabbed;
    }

    public UnseenHandGrabSyncPacket(FriendlyByteBuf buf) {
        this.owner = buf.readUUID();
        int size = buf.readVarInt();
        List<Integer> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) ids.add(buf.readVarInt());
        this.grabbed = ids;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(owner);
        buf.writeVarInt(grabbed.size());
        for (int id : grabbed) buf.writeVarInt(id);
    }

    public static void handle(UnseenHandGrabSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                RemoteUnseenHands.updateGrabbed(packet.owner, packet.grabbed)));
        ctx.setPacketHandled(true);
    }
}
