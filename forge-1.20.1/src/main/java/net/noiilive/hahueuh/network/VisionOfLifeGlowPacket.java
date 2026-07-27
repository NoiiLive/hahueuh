package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class VisionOfLifeGlowPacket {
    public final List<Integer> hostileIds;
    public final List<Integer> passiveIds;
    public final List<Integer> playerIds;
    public final List<Integer> witchFactorIds;

    public VisionOfLifeGlowPacket(List<Integer> hostileIds, List<Integer> passiveIds,
                                  List<Integer> playerIds, List<Integer> witchFactorIds) {
        this.hostileIds = hostileIds;
        this.passiveIds = passiveIds;
        this.playerIds = playerIds;
        this.witchFactorIds = witchFactorIds;
    }

    private static void writeIds(FriendlyByteBuf buf, List<Integer> ids) {
        buf.writeVarInt(ids.size());
        for (int id : ids) buf.writeVarInt(id);
    }

    private static List<Integer> readIds(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Integer> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) ids.add(buf.readVarInt());
        return ids;
    }

    public VisionOfLifeGlowPacket(FriendlyByteBuf buf) {
        this.hostileIds = readIds(buf);
        this.passiveIds = readIds(buf);
        this.playerIds = readIds(buf);
        this.witchFactorIds = readIds(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        writeIds(buf, hostileIds);
        writeIds(buf, passiveIds);
        writeIds(buf, playerIds);
        writeIds(buf, witchFactorIds);
    }

    public static void handle(VisionOfLifeGlowPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientVisionOfLifeGlowState.set(packet.hostileIds, packet.passiveIds,
                        packet.playerIds, packet.witchFactorIds)));
        ctx.setPacketHandled(true);
    }
}
