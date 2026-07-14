package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.BoundVisionAbility;
import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.VisionInfoQueryPayload;
import net.noiilive.hahueuh.network.VisionInfoResultPayload;
import net.noiilive.hahueuh.snapshot.PlayerAuthorityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class VisionOfInformation {
    private MinecraftServer server;

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        this.server = null;
    }

    public void handleQuery(ServerPlayer viewer, VisionInfoQueryPayload payload) {
        if (server == null) return;
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        boolean asEchidna = am.canUseGreed(viewer.getUUID()) && am.getGreedVariant(viewer.getUUID()) == GreedVariant.ECHIDNA
                && HahUeuh.BOOK_OF_WISDOM.isHoldingOwnBook(viewer);
        boolean asBookOfWisdom = HahUeuh.BOOK_OF_WISDOM_COPY.isHoldingBoundCopy(viewer, BoundVisionAbility.VISION_OF_INFORMATION);
        if (!asEchidna && !asBookOfWisdom) {
            viewer.displayClientMessage(Component.translatable("hahueuh.message.echidna_needs_book")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        String query = payload.query().trim();
        if (query.isEmpty()) return;

        VisionInfoResultPayload result = lookUpPlayer(query);
        if (result.kind() == VisionInfoResultPayload.KIND_NOT_FOUND) {
            result = lookUpEntity(query);
        }
        PacketDistributor.sendToPlayer(viewer, result);
    }

    private VisionInfoResultPayload lookUpPlayer(String name) {
        Optional<com.mojang.authlib.GameProfile> profile = server.getProfileCache() == null
                ? Optional.empty() : server.getProfileCache().get(name);
        if (profile.isEmpty()) return VisionInfoResultPayload.notFound(name);
        UUID uuid = profile.get().getId();

        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        VisionInfoResultPayload.PlayerData data = online != null
                ? gatherOnline(online)
                : gatherOffline(uuid, profile.get().getName());
        if (data == null) return VisionInfoResultPayload.notFound(name);
        return VisionInfoResultPayload.ofPlayer(name, data);
    }

    private VisionInfoResultPayload.PlayerData gatherOnline(ServerPlayer player) {
        List<ItemStack> inventory = new ArrayList<>();
        var inv = player.getInventory();
        inventory.addAll(inv.items);
        inventory.addAll(inv.armor);
        inventory.addAll(inv.offhand);

        List<ItemStack> ender = new ArrayList<>();
        var ec = player.getEnderChestInventory();
        for (int i = 0; i < ec.getContainerSize(); i++) ender.add(ec.getItem(i));

        List<VisionInfoResultPayload.Effect> effects = new ArrayList<>();
        player.getActiveEffects().forEach(inst -> effects.add(new VisionInfoResultPayload.Effect(
                effectId(inst.getEffect()), inst.getAmplifier(), inst.getDuration())));

        return buildPlayerData(player.getGameProfile().getName(), player.getUUID(), true,
                player.getHealth(), player.getMaxHealth(), player.getArmorValue(),
                player.getFoodData().getFoodLevel(),
                player.getX(), player.getY(), player.getZ(), player.level().dimension().location().toString(),
                inventory, ender, effects);
    }

    private VisionInfoResultPayload.PlayerData gatherOffline(UUID uuid, String name) {
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("playerdata").resolve(uuid + ".dat");
        if (!Files.exists(file)) return null;
        CompoundTag root;
        try {
            root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        } catch (Exception e) {
            HahUeuh.LOGGER.warn("Vision of Information: failed to read offline player data {}", file, e);
            return null;
        }
        if (root == null) return null;

        ItemStack[] inv = new ItemStack[41];
        java.util.Arrays.fill(inv, ItemStack.EMPTY);
        ListTag invList = root.getList("Inventory", Tag.TAG_COMPOUND);
        for (int i = 0; i < invList.size(); i++) {
            CompoundTag entry = invList.getCompound(i);
            int slot = entry.getByte("Slot") & 0xFF;
            int idx;
            if (slot < 36) idx = slot;                    // main
            else if (slot >= 100 && slot <= 103) idx = 36 + (slot - 100); // armor
            else if (slot == 150 || slot == 0xFFFFFF9A) idx = 40;         // offhand (-106)
            else continue;
            parseStack(entry).ifPresent(s -> inv[idx] = s);
        }
        List<ItemStack> inventory = List.of(inv);

        ItemStack[] ec = new ItemStack[27];
        java.util.Arrays.fill(ec, ItemStack.EMPTY);
        ListTag ecList = root.getList("EnderItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < ecList.size(); i++) {
            CompoundTag entry = ecList.getCompound(i);
            int slot = entry.getByte("Slot") & 0xFF;
            if (slot < 27) {
                int fi = slot;
                parseStack(entry).ifPresent(s -> ec[fi] = s);
            }
        }
        List<ItemStack> ender = List.of(ec);

        List<VisionInfoResultPayload.Effect> effects = new ArrayList<>();
        ListTag effectList = root.contains("active_effects", Tag.TAG_LIST)
                ? root.getList("active_effects", Tag.TAG_COMPOUND)
                : root.getList("ActiveEffects", Tag.TAG_COMPOUND);
        for (int i = 0; i < effectList.size(); i++) {
            CompoundTag e = effectList.getCompound(i);
            effects.add(new VisionInfoResultPayload.Effect(
                    e.getString("id"), e.getByte("amplifier") & 0xFF, e.getInt("duration")));
        }

        float health = root.contains("Health") ? root.getFloat("Health") : 20f;
        int food = root.contains("foodLevel") ? root.getInt("foodLevel") : 20;
        float maxHealth = offlineMaxHealth(root);
        int armor = offlineArmor(inv);

        double x = 0, y = 0, z = 0;
        if (root.contains("Pos", Tag.TAG_LIST)) {
            ListTag pos = root.getList("Pos", Tag.TAG_DOUBLE);
            if (pos.size() == 3) { x = pos.getDouble(0); y = pos.getDouble(1); z = pos.getDouble(2); }
        }
        String dimension = root.contains("Dimension") ? root.getString("Dimension") : "minecraft:overworld";

        return buildPlayerData(name, uuid, false, health, maxHealth, armor, food, x, y, z, dimension,
                inventory, ender, effects);
    }

    private VisionInfoResultPayload.PlayerData buildPlayerData(String name, UUID uuid, boolean online,
            float health, float maxHealth, int armor, int food, double x, double y, double z, String dimension,
            List<ItemStack> inventory, List<ItemStack> ender, List<VisionInfoResultPayload.Effect> effects) {
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        return new VisionInfoResultPayload.PlayerData(name, uuid, online, health, maxHealth, armor, food,
                x, y, z, dimension, inventory, ender, effects,
                am.canReturnByDeath(uuid), am.canUseDomain(uuid),
                am.canUseSloth(uuid), am.getSlothVariant(uuid).ordinal(),
                am.canUseGreed(uuid), am.getGreedVariant(uuid).ordinal());
    }

    private Optional<ItemStack> parseStack(CompoundTag entry) {
        try {
            return ItemStack.parse(server.registryAccess(), entry);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private float offlineMaxHealth(CompoundTag root) {
        ListTag attrs = root.getList("attributes", Tag.TAG_COMPOUND);
        for (int i = 0; i < attrs.size(); i++) {
            CompoundTag a = attrs.getCompound(i);
            String id = a.getString("id");
            if (id.endsWith("max_health")) return (float) a.getDouble("base");
        }
        return 20f;
    }

    private int offlineArmor(ItemStack[] inv) {
        double total = 0;
        for (int i = 36; i <= 39 && i < inv.length; i++) {
            ItemStack stack = inv[i];
            if (stack.isEmpty()) continue;
            ItemAttributeModifiers mods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
            for (ItemAttributeModifiers.Entry entry : mods.modifiers()) {
                if (entry.attribute().equals(Attributes.ARMOR)) total += entry.modifier().amount();
            }
        }
        return (int) Math.round(total);
    }

    private VisionInfoResultPayload lookUpEntity(String query) {
        ResourceLocation id = ResourceLocation.tryParse(query.contains(":") ? query : "minecraft:" + query);
        if (id == null) return VisionInfoResultPayload.notFound(query);
        Optional<EntityType<?>> typeOpt = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
        if (typeOpt.isEmpty()) return VisionInfoResultPayload.notFound(query);
        EntityType<?> type = typeOpt.get();

        ServerLevel level = server.overworld();
        float maxHealth = 0;
        int armor = 0;
        ResourceKey<LootTable> lootKey = null;
        try {
            Entity dummy = type.create(level);
            if (dummy instanceof LivingEntity living) {
                maxHealth = living.getMaxHealth();
                armor = living.getArmorValue();
                lootKey = living.getLootTable();
            }
            if (dummy != null) dummy.discard();
        } catch (Exception e) {
            HahUeuh.LOGGER.warn("Vision of Information: failed to instantiate entity {}", id, e);
        }

        List<VisionInfoResultPayload.Drop> drops = lootKey == null ? List.of() : analyzeDrops(lootKey);
        int loadedCount = countLoaded(type);
        String name = type.getDescription().getString();
        return VisionInfoResultPayload.ofEntity(query, new VisionInfoResultPayload.EntityData(
                id.toString(), name, maxHealth, armor, drops, loadedCount));
    }

    private int countLoaded(EntityType<?> type) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (e.getType() == type) count++;
            }
        }
        return count;
    }

    private static final Field POOLS_FIELD = uncheckedField(LootTable.class, "pools");
    private static final Field ENTRIES_FIELD = uncheckedField(LootPool.class, "entries");
    private static final Field POOL_CONDITIONS_FIELD = uncheckedField(LootPool.class, "conditions");
    private static final Field CHILDREN_FIELD = uncheckedField(CompositeEntryBase.class, "children");
    private static final Field ENTRY_CONDITIONS_FIELD = uncheckedField(LootPoolEntryContainer.class, "conditions");
    private static final Field WEIGHT_FIELD = uncheckedField(LootPoolSingletonContainer.class, "weight");
    private static final Field FUNCTIONS_FIELD = uncheckedField(LootPoolSingletonContainer.class, "functions");
    private static final Field ITEM_FIELD = uncheckedField(LootItem.class, "item");
    private static final Field SET_COUNT_VALUE_FIELD = uncheckedField(SetItemCountFunction.class, "value");
    private static final Field SET_COUNT_ADD_FIELD = uncheckedField(SetItemCountFunction.class, "add");

    private static Field uncheckedField(Class<?> owner, String name) {
        try {
            Field f = owner.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private List<VisionInfoResultPayload.Drop> analyzeDrops(ResourceKey<LootTable> lootKey) {
        List<VisionInfoResultPayload.Drop> drops = new ArrayList<>();
        try {
            LootTable table = server.reloadableRegistries().getLootTable(lootKey);
            if (table == LootTable.EMPTY) return drops;

            @SuppressWarnings("unchecked")
            List<LootPool> pools = (List<LootPool>) POOLS_FIELD.get(table);

            for (LootPool pool : pools) {
                float poolChance = conditionChanceMultiplier(POOL_CONDITIONS_FIELD, pool);

                @SuppressWarnings("unchecked")
                List<LootPoolEntryContainer> entries = (List<LootPoolEntryContainer>) ENTRIES_FIELD.get(pool);

                List<LootPoolSingletonContainer> leaves = new ArrayList<>();
                for (LootPoolEntryContainer entry : entries) {
                    flattenEntries(entry, leaves);
                }

                int totalWeight = 0;
                for (LootPoolSingletonContainer s : leaves) totalWeight += WEIGHT_FIELD.getInt(s);
                if (totalWeight <= 0) continue;

                for (LootPoolSingletonContainer entry : leaves) {
                    if (!(entry instanceof LootItem item)) continue;
                    int weight = WEIGHT_FIELD.getInt(item);
                    float chance = poolChance * ((float) weight / totalWeight)
                            * conditionChanceMultiplier(ENTRY_CONDITIONS_FIELD, item);

                    @SuppressWarnings("unchecked")
                    Holder<Item> holder = (Holder<Item>) ITEM_FIELD.get(item);
                    ResourceLocation itemId = holder.unwrapKey()
                            .map(k -> k.location()).orElse(BuiltInRegistries.ITEM.getKey(holder.value()));
                    int[] countRange = countRange(item);
                    drops.add(new VisionInfoResultPayload.Drop(itemId.toString(), chance, countRange[0], countRange[1]));
                }
            }
        } catch (Exception e) {
            HahUeuh.LOGGER.warn("Vision of Information: could not analyze drops for {}", lootKey.location(), e);
        }
        return drops;
    }

    private void flattenEntries(LootPoolEntryContainer entry, List<LootPoolSingletonContainer> out) throws IllegalAccessException {
        if (entry instanceof LootPoolSingletonContainer singleton) {
            out.add(singleton);
        } else if (entry instanceof CompositeEntryBase composite) {
            @SuppressWarnings("unchecked")
            List<LootPoolEntryContainer> children = (List<LootPoolEntryContainer>) CHILDREN_FIELD.get(composite);
            for (LootPoolEntryContainer child : children) {
                flattenEntries(child, out);
            }
        }
        // Anything else (tag entries, dynamic entries, loot-table references) is skipped rather than guessed.
    }

    private float conditionChanceMultiplier(Field conditionsField, Object owner) throws IllegalAccessException {
        @SuppressWarnings("unchecked")
        List<LootItemCondition> conditions = (List<LootItemCondition>) conditionsField.get(owner);
        float chance = 1.0f;
        for (LootItemCondition condition : conditions) {
            if (condition instanceof LootItemRandomChanceCondition rc && rc.chance() instanceof ConstantValue cv) {
                chance *= cv.value();
            } else if (condition instanceof LootItemRandomChanceWithEnchantedBonusCondition rb) {
                chance *= rb.unenchantedChance();
            }
        }
        return chance;
    }

    private int[] countRange(LootPoolSingletonContainer entry) throws IllegalAccessException {
        float min = 1, max = 1;
        @SuppressWarnings("unchecked")
        List<LootItemFunction> functions = (List<LootItemFunction>) FUNCTIONS_FIELD.get(entry);
        for (LootItemFunction fn : functions) {
            if (fn instanceof SetItemCountFunction setCount) {
                NumberProvider value = (NumberProvider) SET_COUNT_VALUE_FIELD.get(setCount);
                boolean add = SET_COUNT_ADD_FIELD.getBoolean(setCount);
                float[] range = providerRange(value);
                if (range == null) continue;
                if (add) {
                    min += range[0];
                    max += range[1];
                } else {
                    min = range[0];
                    max = range[1];
                }
            }
        }
        int lo = Math.max(0, Math.round(min));
        int hi = Math.max(lo, Math.round(max));
        return new int[]{lo, hi};
    }

    private float[] providerRange(NumberProvider provider) {
        if (provider instanceof ConstantValue cv) {
            return new float[]{cv.value(), cv.value()};
        }
        if (provider instanceof UniformGenerator ug) {
            float[] lo = providerRange(ug.min());
            float[] hi = providerRange(ug.max());
            if (lo == null || hi == null) return null;
            return new float[]{lo[0], hi[1]};
        }
        return null;
    }

    private static String effectId(Holder<net.minecraft.world.effect.MobEffect> effect) {
        return effect.unwrapKey().map(k -> k.location().toString()).orElse("minecraft:unknown");
    }
}
