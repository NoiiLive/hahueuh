package net.noiilive.hahueuh.network;

import net.noiilive.hahueuh.HahUeuh;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record VisionInfoResultPayload(int kind, String query, PlayerData player, EntityData entity)
        implements CustomPacketPayload {
    public static final int KIND_NOT_FOUND = 0;
    public static final int KIND_PLAYER = 1;
    public static final int KIND_ENTITY = 2;

    public record Effect(String id, int amplifier, int duration) {}

    public record Drop(String itemId, float chance, int minCount, int maxCount) {}

    public record PlayerData(String name, UUID uuid, boolean online,
                             float health, float maxHealth, int armor, int food,
                             double x, double y, double z, String dimension,
                             List<ItemStack> inventory, List<ItemStack> enderChest, List<Effect> effects,
                             boolean returnByDeath, boolean domain,
                             boolean sloth, int slothVariant, boolean greed, int greedVariant) {}

    public record EntityData(String typeId, String name, float maxHealth, int armor, List<Drop> drops,
                             int loadedCount) {}

    public static final Type<VisionInfoResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "vision_info_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VisionInfoResultPayload> STREAM_CODEC =
            StreamCodec.of(VisionInfoResultPayload::write, VisionInfoResultPayload::read);

    public static VisionInfoResultPayload notFound(String query) {
        return new VisionInfoResultPayload(KIND_NOT_FOUND, query, null, null);
    }

    public static VisionInfoResultPayload ofPlayer(String query, PlayerData data) {
        return new VisionInfoResultPayload(KIND_PLAYER, query, data, null);
    }

    public static VisionInfoResultPayload ofEntity(String query, EntityData data) {
        return new VisionInfoResultPayload(KIND_ENTITY, query, null, data);
    }

    private static void writeItems(RegistryFriendlyByteBuf buf, List<ItemStack> items) {
        buf.writeVarInt(items.size());
        for (ItemStack stack : items) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
    }

    private static List<ItemStack> readItems(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<ItemStack> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) items.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        return items;
    }

    private static void writeEffects(RegistryFriendlyByteBuf buf, List<Effect> effects) {
        buf.writeVarInt(effects.size());
        for (Effect e : effects) {
            buf.writeUtf(e.id());
            buf.writeVarInt(e.amplifier());
            buf.writeVarInt(e.duration());
        }
    }

    private static List<Effect> readEffects(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Effect> effects = new ArrayList<>(count);
        for (int i = 0; i < count; i++) effects.add(new Effect(buf.readUtf(), buf.readVarInt(), buf.readVarInt()));
        return effects;
    }

    private static void write(RegistryFriendlyByteBuf buf, VisionInfoResultPayload payload) {
        buf.writeVarInt(payload.kind);
        buf.writeUtf(payload.query, 256);
        if (payload.kind == KIND_PLAYER) {
            PlayerData p = payload.player;
            buf.writeUtf(p.name());
            buf.writeUUID(p.uuid());
            buf.writeBoolean(p.online());
            buf.writeFloat(p.health());
            buf.writeFloat(p.maxHealth());
            buf.writeVarInt(p.armor());
            buf.writeVarInt(p.food());
            buf.writeDouble(p.x());
            buf.writeDouble(p.y());
            buf.writeDouble(p.z());
            buf.writeUtf(p.dimension());
            writeItems(buf, p.inventory());
            writeItems(buf, p.enderChest());
            writeEffects(buf, p.effects());
            buf.writeBoolean(p.returnByDeath());
            buf.writeBoolean(p.domain());
            buf.writeBoolean(p.sloth());
            buf.writeVarInt(p.slothVariant());
            buf.writeBoolean(p.greed());
            buf.writeVarInt(p.greedVariant());
        } else if (payload.kind == KIND_ENTITY) {
            EntityData e = payload.entity;
            buf.writeUtf(e.typeId());
            buf.writeUtf(e.name());
            buf.writeFloat(e.maxHealth());
            buf.writeVarInt(e.armor());
            buf.writeVarInt(e.drops().size());
            for (Drop d : e.drops()) {
                buf.writeUtf(d.itemId());
                buf.writeFloat(d.chance());
                buf.writeVarInt(d.minCount());
                buf.writeVarInt(d.maxCount());
            }
            buf.writeVarInt(e.loadedCount());
        }
    }

    private static VisionInfoResultPayload read(RegistryFriendlyByteBuf buf) {
        int kind = buf.readVarInt();
        String query = buf.readUtf(256);
        if (kind == KIND_PLAYER) {
            String name = buf.readUtf();
            UUID uuid = buf.readUUID();
            boolean online = buf.readBoolean();
            float health = buf.readFloat();
            float maxHealth = buf.readFloat();
            int armor = buf.readVarInt();
            int food = buf.readVarInt();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            String dimension = buf.readUtf();
            List<ItemStack> inventory = readItems(buf);
            List<ItemStack> enderChest = readItems(buf);
            List<Effect> effects = readEffects(buf);
            boolean rbd = buf.readBoolean();
            boolean domain = buf.readBoolean();
            boolean sloth = buf.readBoolean();
            int slothVariant = buf.readVarInt();
            boolean greed = buf.readBoolean();
            int greedVariant = buf.readVarInt();
            return ofPlayer(query, new PlayerData(name, uuid, online, health, maxHealth, armor, food,
                    x, y, z, dimension, inventory, enderChest, effects,
                    rbd, domain, sloth, slothVariant, greed, greedVariant));
        } else if (kind == KIND_ENTITY) {
            String typeId = buf.readUtf();
            String name = buf.readUtf();
            float maxHealth = buf.readFloat();
            int armor = buf.readVarInt();
            int dropCount = buf.readVarInt();
            List<Drop> drops = new ArrayList<>(dropCount);
            for (int i = 0; i < dropCount; i++) {
                drops.add(new Drop(buf.readUtf(), buf.readFloat(), buf.readVarInt(), buf.readVarInt()));
            }
            int loadedCount = buf.readVarInt();
            return ofEntity(query, new EntityData(typeId, name, maxHealth, armor, drops, loadedCount));
        }
        return notFound(query);
    }

    @Override
    public Type<VisionInfoResultPayload> type() {
        return TYPE;
    }
}
