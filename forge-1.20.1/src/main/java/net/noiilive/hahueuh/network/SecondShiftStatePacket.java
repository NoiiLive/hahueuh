package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SecondShiftStatePacket {
    public final boolean active;

    public SecondShiftStatePacket(boolean active) {
        this.active = active;
    }

    public SecondShiftStatePacket(FriendlyByteBuf buf) {
        this.active = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(active);
    }

    public static void handle(SecondShiftStatePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientSecondShiftState.setActive(packet.active)));
        ctx.setPacketHandled(true);
    }
}
