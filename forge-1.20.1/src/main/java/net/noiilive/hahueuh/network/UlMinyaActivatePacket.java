package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class UlMinyaActivatePacket {
    public final int targetEntityId;

    public UlMinyaActivatePacket(int targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    public UlMinyaActivatePacket(FriendlyByteBuf buf) {
        this.targetEntityId = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(targetEntityId);
    }

    public static void handle(UlMinyaActivatePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            HahUeuh.SPELL_CASTING.beginUlMinya(player, packet.targetEntityId);
        });
        ctx.setPacketHandled(true);
    }
}
