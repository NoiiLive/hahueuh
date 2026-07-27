package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DeathFadePacket {
    private final boolean toBlack;

    public DeathFadePacket(boolean toBlack) {
        this.toBlack = toBlack;
    }

    public DeathFadePacket(FriendlyByteBuf buf) {
        this.toBlack = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(toBlack);
    }

    public static void handle(DeathFadePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> DeathFadeState.onSignal(packet.toBlack)));
        ctx.setPacketHandled(true);
    }
}
