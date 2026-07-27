package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class AlShamakActivatePacket {
    public static final int RELEASE = 0;
    public static final int BANISH = 1;
    public static final int DISCARD = 2;

    public final int mode;
    public final int targetEntityId;

    public AlShamakActivatePacket(int mode, int targetEntityId) {
        this.mode = mode;
        this.targetEntityId = targetEntityId;
    }

    public AlShamakActivatePacket(FriendlyByteBuf buf) {
        this.mode = buf.readVarInt();
        this.targetEntityId = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(mode);
        buf.writeVarInt(targetEntityId);
    }

    public static void handle(AlShamakActivatePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (packet.mode == RELEASE) {
                HahUeuh.SPELL_CASTING.releaseStoredSpell(player, packet.targetEntityId);
            } else if (packet.mode == DISCARD) {
                HahUeuh.SPELL_CASTING.discardAlShamak(player);
            } else {
                HahUeuh.SPELL_CASTING.beginBanish(player, packet.targetEntityId);
            }
        });
        ctx.setPacketHandled(true);
    }
}
