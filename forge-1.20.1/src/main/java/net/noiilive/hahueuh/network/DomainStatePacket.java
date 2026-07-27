package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DomainStatePacket {
    public static final DomainStatePacket INACTIVE =
            new DomainStatePacket(false, 0, 0, 0, 0, new ResourceLocation("minecraft", "overworld"));

    private final boolean active;
    private final double x;
    private final double y;
    private final double z;
    private final double radius;
    private final ResourceLocation dimension;

    public DomainStatePacket(boolean active, double x, double y, double z, double radius,
                             ResourceLocation dimension) {
        this.active = active;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.dimension = dimension;
    }

    public DomainStatePacket(FriendlyByteBuf buf) {
        this.active = buf.readBoolean();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.radius = buf.readDouble();
        this.dimension = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(active);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(radius);
        buf.writeResourceLocation(dimension);
    }

    public boolean active() { return active; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public double radius() { return radius; }
    public ResourceLocation dimension() { return dimension; }

    public static void handle(DomainStatePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> DomainRenderState.update(packet)));
        ctx.setPacketHandled(true);
    }
}
