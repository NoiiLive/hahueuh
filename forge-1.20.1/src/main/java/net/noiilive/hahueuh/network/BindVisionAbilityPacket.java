package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class BindVisionAbilityPacket {
    public final int abilityOrdinal;

    public BindVisionAbilityPacket(int abilityOrdinal) {
        this.abilityOrdinal = abilityOrdinal;
    }

    public BindVisionAbilityPacket(FriendlyByteBuf buf) {
        this.abilityOrdinal = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(abilityOrdinal);
    }

    public static void handle(BindVisionAbilityPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) HahUeuh.BOOK_OF_WISDOM_COPY.bind(sender, packet.abilityOrdinal);
        });
        ctx.setPacketHandled(true);
    }
}
