package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class EfficientEnchantSelectPacket {
    public final String enchantmentId;

    public EfficientEnchantSelectPacket(String enchantmentId) {
        this.enchantmentId = enchantmentId;
    }

    public EfficientEnchantSelectPacket(FriendlyByteBuf buf) {
        this.enchantmentId = buf.readUtf(256);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(enchantmentId, 256);
    }

    public static void handle(EfficientEnchantSelectPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) HahUeuh.EFFICIENT_ENCHANTING.select(sender, packet.enchantmentId);
        });
        ctx.setPacketHandled(true);
    }
}
