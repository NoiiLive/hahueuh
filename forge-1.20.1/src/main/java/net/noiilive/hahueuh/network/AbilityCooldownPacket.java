package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.noiilive.hahueuh.api.AbilityCooldowns;

import java.util.function.Supplier;

public class AbilityCooldownPacket {
    private final ResourceLocation cooldownId;
    private final int ticks;

    public AbilityCooldownPacket(ResourceLocation cooldownId, int ticks) {
        this.cooldownId = cooldownId;
        this.ticks = ticks;
    }

    public AbilityCooldownPacket(FriendlyByteBuf buf) {
        this.cooldownId = buf.readResourceLocation();
        this.ticks = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(cooldownId);
        buf.writeVarInt(ticks);
    }

    public static void handle(AbilityCooldownPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> AbilityCooldowns.startCooldown(packet.cooldownId, packet.ticks / 20.0)));
        ctx.setPacketHandled(true);
    }
}
