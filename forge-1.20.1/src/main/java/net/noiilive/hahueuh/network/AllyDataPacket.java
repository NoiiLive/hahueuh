package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class AllyDataPacket {
    public record Effect(String id, int amplifier, int duration) {}

    public record Ally(UUID uuid, String name, int typeOrdinal, boolean online, boolean hasData,
                       float health, float maxHealth, double x, double y, double z,
                       double dx, double dz, boolean sameDimension,
                       float weight, List<Effect> effects) {
        public AllyType type() {
            return AllyType.byOrdinal(typeOrdinal);
        }
    }

    public final boolean open;
    public final float selfWeight;
    public final float selfHealth;
    public final float selfMaxHealth;
    public final double selfX;
    public final double selfY;
    public final double selfZ;
    public final List<Effect> selfEffects;
    public final List<Ally> allies;

    public AllyDataPacket(boolean open, float selfWeight, float selfHealth, float selfMaxHealth,
                          double selfX, double selfY, double selfZ, List<Effect> selfEffects,
                          List<Ally> allies) {
        this.open = open;
        this.selfWeight = selfWeight;
        this.selfHealth = selfHealth;
        this.selfMaxHealth = selfMaxHealth;
        this.selfX = selfX;
        this.selfY = selfY;
        this.selfZ = selfZ;
        this.selfEffects = selfEffects;
        this.allies = allies;
    }

    public boolean open() { return open; }

    public float selfWeight() { return selfWeight; }

    public float selfHealth() { return selfHealth; }

    public float selfMaxHealth() { return selfMaxHealth; }

    public double selfX() { return selfX; }

    public double selfY() { return selfY; }

    public double selfZ() { return selfZ; }

    public List<Effect> selfEffects() { return selfEffects; }

    public List<Ally> allies() { return allies; }

    private static void writeEffects(FriendlyByteBuf buf, List<Effect> effects) {
        buf.writeVarInt(effects.size());
        for (Effect effect : effects) {
            buf.writeUtf(effect.id());
            buf.writeVarInt(effect.amplifier());
            buf.writeVarInt(effect.duration());
        }
    }

    private static List<Effect> readEffects(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Effect> effects = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            effects.add(new Effect(buf.readUtf(), buf.readVarInt(), buf.readVarInt()));
        }
        return effects;
    }

    public AllyDataPacket(FriendlyByteBuf buf) {
        this.open = buf.readBoolean();
        this.selfWeight = buf.readFloat();
        this.selfHealth = buf.readFloat();
        this.selfMaxHealth = buf.readFloat();
        this.selfX = buf.readDouble();
        this.selfY = buf.readDouble();
        this.selfZ = buf.readDouble();
        this.selfEffects = readEffects(buf);
        int count = buf.readVarInt();
        List<Ally> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID uuid = buf.readUUID();
            String name = buf.readUtf();
            int typeOrdinal = buf.readVarInt();
            boolean online = buf.readBoolean();
            boolean hasData = buf.readBoolean();
            float health = buf.readFloat();
            float maxHealth = buf.readFloat();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            double dx = buf.readDouble();
            double dz = buf.readDouble();
            boolean sameDimension = buf.readBoolean();
            float weight = buf.readFloat();
            list.add(new Ally(uuid, name, typeOrdinal, online, hasData, health, maxHealth, x, y, z,
                    dx, dz, sameDimension, weight, readEffects(buf)));
        }
        this.allies = list;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(open);
        buf.writeFloat(selfWeight);
        buf.writeFloat(selfHealth);
        buf.writeFloat(selfMaxHealth);
        buf.writeDouble(selfX);
        buf.writeDouble(selfY);
        buf.writeDouble(selfZ);
        writeEffects(buf, selfEffects);
        buf.writeVarInt(allies.size());
        for (Ally ally : allies) {
            buf.writeUUID(ally.uuid());
            buf.writeUtf(ally.name());
            buf.writeVarInt(ally.typeOrdinal());
            buf.writeBoolean(ally.online());
            buf.writeBoolean(ally.hasData());
            buf.writeFloat(ally.health());
            buf.writeFloat(ally.maxHealth());
            buf.writeDouble(ally.x());
            buf.writeDouble(ally.y());
            buf.writeDouble(ally.z());
            buf.writeDouble(ally.dx());
            buf.writeDouble(ally.dz());
            buf.writeBoolean(ally.sameDimension());
            buf.writeFloat(ally.weight());
            writeEffects(buf, ally.effects());
        }
    }

    public static void handle(AllyDataPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                AllyTrackerData.accept(packet)));
        ctx.setPacketHandled(true);
    }
}
