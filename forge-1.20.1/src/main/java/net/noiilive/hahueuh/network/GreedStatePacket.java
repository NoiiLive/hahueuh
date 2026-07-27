package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GreedStatePacket {
    private final boolean canGreed;
    private final int variantId;

    public GreedStatePacket(boolean canGreed, int variantId) {
        this.canGreed = canGreed;
        this.variantId = variantId;
    }

    public GreedStatePacket(FriendlyByteBuf buf) {
        this.canGreed = buf.readBoolean();
        this.variantId = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(canGreed);
        buf.writeVarInt(variantId);
    }

    public static void handle(GreedStatePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientGreedState.update(packet.canGreed, packet.variantId)));
        ctx.setPacketHandled(true);
    }
}
