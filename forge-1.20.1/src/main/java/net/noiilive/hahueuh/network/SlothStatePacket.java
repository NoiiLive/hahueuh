package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SlothStatePacket {
    private final boolean canSloth;
    private final int variantId;
    private final int handCount;
    private final int fingerHandCount;

    public SlothStatePacket(boolean canSloth, int variantId, int handCount, int fingerHandCount) {
        this.canSloth = canSloth;
        this.variantId = variantId;
        this.handCount = handCount;
        this.fingerHandCount = fingerHandCount;
    }

    public SlothStatePacket(FriendlyByteBuf buf) {
        this.canSloth = buf.readBoolean();
        this.variantId = buf.readVarInt();
        this.handCount = buf.readVarInt();
        this.fingerHandCount = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(canSloth);
        buf.writeVarInt(variantId);
        buf.writeVarInt(handCount);
        buf.writeVarInt(fingerHandCount);
    }

    public static void handle(SlothStatePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientSlothState.update(packet.canSloth, packet.variantId, packet.handCount);
            ClientFingerState.setHands(packet.fingerHandCount);
        }));
        ctx.setPacketHandled(true);
    }
}
