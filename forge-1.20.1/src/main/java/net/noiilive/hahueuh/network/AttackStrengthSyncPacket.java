package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AttackStrengthSyncPacket {
    public final boolean offhandNext;
    public final float scale;

    public AttackStrengthSyncPacket(boolean offhandNext, float scale) {
        this.offhandNext = offhandNext;
        this.scale = scale;
    }

    public AttackStrengthSyncPacket(FriendlyByteBuf buf) {
        this.offhandNext = buf.readBoolean();
        this.scale = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(offhandNext);
        buf.writeFloat(scale);
    }

    public static void handle(AttackStrengthSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                net.noiilive.hahueuh.client.DualWieldClientHandler.applyAttackStrength(
                        packet.offhandNext, packet.scale)));
        ctx.setPacketHandled(true);
    }
}
