package net.noiilive.hahueuh.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class VisionInfoResultPacket {
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

    private final int kind;
    private final String query;
    private final PlayerData player;
    private final EntityData entity;

    public VisionInfoResultPacket(int kind, String query, PlayerData player, EntityData entity) {
        this.kind = kind;
        this.query = query;
        this.player = player;
        this.entity = entity;
    }

    public int kind() { return kind; }

    public String query() { return query; }

    public PlayerData player() { return player; }

    public EntityData entity() { return entity; }

    public static VisionInfoResultPacket notFound(String query) {
        return new VisionInfoResultPacket(KIND_NOT_FOUND, query, null, null);
    }

    public static VisionInfoResultPacket ofPlayer(String query, PlayerData data) {
        return new VisionInfoResultPacket(KIND_PLAYER, query, data, null);
    }

    public static VisionInfoResultPacket ofEntity(String query, EntityData data) {
        return new VisionInfoResultPacket(KIND_ENTITY, query, null, data);
    }

    private static void writeItems(FriendlyByteBuf buf, List<ItemStack> items) {
        buf.writeVarInt(items.size());
        for (ItemStack stack : items) buf.writeItem(stack);
    }

    private static List<ItemStack> readItems(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<ItemStack> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) items.add(buf.readItem());
        return items;
    }

    private static void writeEffects(FriendlyByteBuf buf, List<Effect> effects) {
        buf.writeVarInt(effects.size());
        for (Effect e : effects) {
            buf.writeUtf(e.id());
            buf.writeVarInt(e.amplifier());
            buf.writeVarInt(e.duration());
        }
    }

    private static List<Effect> readEffects(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Effect> effects = new ArrayList<>(count);
        for (int i = 0; i < count; i++) effects.add(new Effect(buf.readUtf(), buf.readVarInt(), buf.readVarInt()));
        return effects;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(kind);
        buf.writeUtf(query, 256);
        if (kind == KIND_PLAYER) {
            buf.writeUtf(player.name());
            buf.writeUUID(player.uuid());
            buf.writeBoolean(player.online());
            buf.writeFloat(player.health());
            buf.writeFloat(player.maxHealth());
            buf.writeVarInt(player.armor());
            buf.writeVarInt(player.food());
            buf.writeDouble(player.x());
            buf.writeDouble(player.y());
            buf.writeDouble(player.z());
            buf.writeUtf(player.dimension());
            writeItems(buf, player.inventory());
            writeItems(buf, player.enderChest());
            writeEffects(buf, player.effects());
            buf.writeBoolean(player.returnByDeath());
            buf.writeBoolean(player.domain());
            buf.writeBoolean(player.sloth());
            buf.writeVarInt(player.slothVariant());
            buf.writeBoolean(player.greed());
            buf.writeVarInt(player.greedVariant());
        } else if (kind == KIND_ENTITY) {
            buf.writeUtf(entity.typeId());
            buf.writeUtf(entity.name());
            buf.writeFloat(entity.maxHealth());
            buf.writeVarInt(entity.armor());
            buf.writeVarInt(entity.drops().size());
            for (Drop d : entity.drops()) {
                buf.writeUtf(d.itemId());
                buf.writeFloat(d.chance());
                buf.writeVarInt(d.minCount());
                buf.writeVarInt(d.maxCount());
            }
            buf.writeVarInt(entity.loadedCount());
        }
    }

    public VisionInfoResultPacket(FriendlyByteBuf buf) {
        this.kind = buf.readVarInt();
        this.query = buf.readUtf(256);
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
            boolean returnByDeath = buf.readBoolean();
            boolean domain = buf.readBoolean();
            boolean sloth = buf.readBoolean();
            int slothVariant = buf.readVarInt();
            boolean greed = buf.readBoolean();
            int greedVariant = buf.readVarInt();
            this.player = new PlayerData(name, uuid, online, health, maxHealth, armor, food, x, y, z, dimension,
                    inventory, enderChest, effects, returnByDeath, domain, sloth, slothVariant, greed, greedVariant);
            this.entity = null;
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
            this.player = null;
            this.entity = new EntityData(typeId, name, maxHealth, armor, drops, loadedCount);
        } else {
            this.player = null;
            this.entity = null;
        }
    }

    public static void handle(VisionInfoResultPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                VisionInfoClientData.set(packet)));
        ctx.setPacketHandled(true);
    }
}
