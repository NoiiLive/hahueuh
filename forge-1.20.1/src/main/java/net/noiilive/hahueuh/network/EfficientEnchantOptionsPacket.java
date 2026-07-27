package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class EfficientEnchantOptionsPacket {
    public record Option(String enchantmentId, int level) {}

    public final List<Option> options;

    public EfficientEnchantOptionsPacket(List<Option> options) {
        this.options = options;
    }

    public List<Option> options() {
        return options;
    }

    public EfficientEnchantOptionsPacket(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Option> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new Option(buf.readUtf(), buf.readVarInt()));
        }
        this.options = list;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(options.size());
        for (Option option : options) {
            buf.writeUtf(option.enchantmentId());
            buf.writeVarInt(option.level());
        }
    }

    public static void handle(EfficientEnchantOptionsPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                EfficientEnchantOptionsData.set(packet.options)));
        ctx.setPacketHandled(true);
    }
}
