package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record AllyDataPayload(boolean open, float selfWeight, float selfHealth, float selfMaxHealth,
                               double selfX, double selfY, double selfZ, List<Effect> selfEffects,
                               List<Ally> allies) implements CustomPacketPayload {

    public record Effect(String id, int amplifier, int duration) {}

    public record Ally(UUID uuid, String name, int typeOrdinal, boolean online, boolean hasData,
                       float health, float maxHealth, double x, double y, double z,
                       double dx, double dz, boolean sameDimension,
                       float weight, List<Effect> effects) {
        public AllyType type() {
            return AllyType.byOrdinal(typeOrdinal);
        }
    }

    public static final Type<AllyDataPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "ally_tracker_data"));

    public static final StreamCodec<FriendlyByteBuf, AllyDataPayload> STREAM_CODEC =
            StreamCodec.of(AllyDataPayload::write, AllyDataPayload::read);

    private static void writeEffect(FriendlyByteBuf buf, Effect effect) {
        buf.writeUtf(effect.id);
        buf.writeVarInt(effect.amplifier);
        buf.writeVarInt(effect.duration);
    }

    private static Effect readEffect(FriendlyByteBuf buf) {
        return new Effect(buf.readUtf(), buf.readVarInt(), buf.readVarInt());
    }

    private static void writeEffects(FriendlyByteBuf buf, List<Effect> effects) {
        buf.writeVarInt(effects.size());
        for (Effect effect : effects) writeEffect(buf, effect);
    }

    private static List<Effect> readEffects(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Effect> effects = new ArrayList<>(count);
        for (int i = 0; i < count; i++) effects.add(readEffect(buf));
        return effects;
    }

    private static void write(FriendlyByteBuf buf, AllyDataPayload payload) {
        buf.writeBoolean(payload.open);
        buf.writeFloat(payload.selfWeight);
        buf.writeFloat(payload.selfHealth);
        buf.writeFloat(payload.selfMaxHealth);
        buf.writeDouble(payload.selfX);
        buf.writeDouble(payload.selfY);
        buf.writeDouble(payload.selfZ);
        writeEffects(buf, payload.selfEffects);
        buf.writeVarInt(payload.allies.size());
        for (Ally ally : payload.allies) {
            buf.writeUUID(ally.uuid);
            buf.writeUtf(ally.name);
            buf.writeVarInt(ally.typeOrdinal);
            buf.writeBoolean(ally.online);
            buf.writeBoolean(ally.hasData);
            buf.writeFloat(ally.health);
            buf.writeFloat(ally.maxHealth);
            buf.writeDouble(ally.x);
            buf.writeDouble(ally.y);
            buf.writeDouble(ally.z);
            buf.writeDouble(ally.dx);
            buf.writeDouble(ally.dz);
            buf.writeBoolean(ally.sameDimension);
            buf.writeFloat(ally.weight);
            writeEffects(buf, ally.effects);
        }
    }

    private static AllyDataPayload read(FriendlyByteBuf buf) {
        boolean open = buf.readBoolean();
        float selfWeight = buf.readFloat();
        float selfHealth = buf.readFloat();
        float selfMaxHealth = buf.readFloat();
        double selfX = buf.readDouble();
        double selfY = buf.readDouble();
        double selfZ = buf.readDouble();
        List<Effect> selfEffects = readEffects(buf);
        int count = buf.readVarInt();
        List<Ally> allies = new ArrayList<>(count);
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
            List<Effect> effects = readEffects(buf);
            allies.add(new Ally(uuid, name, typeOrdinal, online, hasData, health, maxHealth, x, y, z,
                    dx, dz, sameDimension, weight, effects));
        }
        return new AllyDataPayload(open, selfWeight, selfHealth, selfMaxHealth, selfX, selfY, selfZ,
                selfEffects, allies);
    }

    @Override
    public Type<AllyDataPayload> type() {
        return TYPE;
    }
}
