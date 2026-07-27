package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class LittleKingHighlightPacket {
    private final List<Integer> entityIds;

    public LittleKingHighlightPacket(List<Integer> entityIds) {
        this.entityIds = entityIds;
    }

    public LittleKingHighlightPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.entityIds = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entityIds.add(buf.readVarInt());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityIds.size());
        for (int id : entityIds) {
            buf.writeVarInt(id);
        }
    }

    public static void handle(LittleKingHighlightPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientLittleKingState.set(packet.entityIds)));
        ctx.setPacketHandled(true);
    }
}
