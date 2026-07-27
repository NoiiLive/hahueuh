package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.HahUeuh;

import java.util.function.Supplier;

public class BookOfWisdomTogglePacket {
    public static final BookOfWisdomTogglePacket INSTANCE = new BookOfWisdomTogglePacket();

    public BookOfWisdomTogglePacket() {}

    public BookOfWisdomTogglePacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(BookOfWisdomTogglePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null) HahUeuh.BOOK_OF_WISDOM.toggle(sender);
        });
        ctx.setPacketHandled(true);
    }
}
