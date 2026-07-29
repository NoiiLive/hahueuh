package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MurakStatePacket {
    private final boolean reducedGravity;
    private final boolean flying;

    public MurakStatePacket(boolean reducedGravity, boolean flying) {
        this.reducedGravity = reducedGravity;
        this.flying = flying;
    }

    public MurakStatePacket(FriendlyByteBuf buf) {
        this.reducedGravity = buf.readBoolean();
        this.flying = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(reducedGravity);
        buf.writeBoolean(flying);
    }

    public static void handle(MurakStatePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientMurakState.update(packet.reducedGravity, packet.flying)));
        ctx.setPacketHandled(true);
    }
}
