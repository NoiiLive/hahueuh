package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AbilitySlotsSyncPacket {
    private final AbilitySlotsData data;

    public AbilitySlotsSyncPacket(AbilitySlotsData data) {
        this.data = data;
    }

    public AbilitySlotsSyncPacket(FriendlyByteBuf buf) {
        this.data = AbilitySlotsData.decode(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        data.encode(buf);
    }

    public static void handle(AbilitySlotsSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> net.noiilive.hahueuh.client.AbilitySlots.acceptFromServer(packet.data)));
        ctx.setPacketHandled(true);
    }
}
