package net.noiilive.hahueuh;

import net.noiilive.hahueuh.mixin.CompositeEntryAccessor;
import net.noiilive.hahueuh.mixin.LootItemAccessor;
import net.noiilive.hahueuh.mixin.LootPoolAccessor;
import net.noiilive.hahueuh.mixin.LootPoolEntryContainerAccessor;
import net.noiilive.hahueuh.mixin.LootPoolSingletonAccessor;
import net.noiilive.hahueuh.mixin.LootTableAccessor;
import net.noiilive.hahueuh.mixin.NumberProviderAccessors;
import net.noiilive.hahueuh.mixin.RandomChanceConditionAccessor;
import net.noiilive.hahueuh.mixin.RandomChanceLootingConditionAccessor;
import net.noiilive.hahueuh.mixin.SetItemCountFunctionAccessor;
import net.noiilive.hahueuh.mixin.UniformGeneratorAccessor;
import net.noiilive.hahueuh.network.BoundVisionAbility;
import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.network.VisionInfoQueryPacket;
import net.noiilive.hahueuh.network.VisionInfoResultPacket;
import net.noiilive.hahueuh.snapshot.PlayerAuthorityManager;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithLootingCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.noiilive.hahueuh.network.ModNetworking;
import org.jetbrains.annotations.Nullable;

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

    public void handleQuery(ServerPlayer viewer, String rawQuery) {
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

        String query = rawQuery.trim();
        if (query.isEmpty()) return;

        VisionInfoResultPacket result = lookUpPlayer(query);
        if (result.kind() == VisionInfoResultPacket.KIND_NOT_FOUND) {
            result = lookUpEntity(query);
        }
        ModNetworking.sendToPlayer(viewer, result);
    }

    private VisionInfoResultPacket lookUpPlayer(String name) {
        Optional<com.mojang.authlib.GameProfile> profile = server.getProfileCache() == null
                ? Optional.empty() : server.getProfileCache().get(name);
        if (profile.isEmpty()) return VisionInfoResultPacket.notFound(name);
        UUID uuid = profile.get().getId();

        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        VisionInfoResultPacket.PlayerData data = online != null
                ? gatherOnline(online)
                : gatherOffline(uuid, profile.get().getName());
        if (data == null) return VisionInfoResultPacket.notFound(name);
        return VisionInfoResultPacket.ofPlayer(name, data);
    }

    private VisionInfoResultPacket.PlayerData gatherOnline(ServerPlayer player) {
        List<ItemStack> inventory = new ArrayList<>();
        var inv = player.getInventory();
        inventory.addAll(inv.items);
        inventory.addAll(inv.armor);
        inventory.addAll(inv.offhand);

        List<ItemStack> ender = new ArrayList<>();
        var ec = player.getEnderChestInventory();
        for (int i = 0; i < ec.getContainerSize(); i++) ender.add(ec.getItem(i));

        List<VisionInfoResultPacket.Effect> effects = new ArrayList<>();
        player.getActiveEffects().forEach(inst -> effects.add(new VisionInfoResultPacket.Effect(
                effectId(inst.getEffect()), inst.getAmplifier(), inst.getDuration())));

        return buildPlayerData(player.getGameProfile().getName(), player.getUUID(), true,
                player.getHealth(), player.getMaxHealth(), player.getArmorValue(),
                player.getFoodData().getFoodLevel(),
                player.getX(), player.getY(), player.getZ(), player.level().dimension().location().toString(),
                inventory, ender, effects);
    }

    private VisionInfoResultPacket.PlayerData gatherOffline(UUID uuid, String name) {
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("playerdata").resolve(uuid + ".dat");
        if (!Files.exists(file)) return null;
        CompoundTag root;
        try {
            root = NbtIo.readCompressed(file.toFile());
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
            if (slot < 36) idx = slot;
            else if (slot >= 100 && slot <= 103) idx = 36 + (slot - 100);
            else if (slot == 150 || slot == 0xFFFFFF9A) idx = 40;
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

        List<VisionInfoResultPacket.Effect> effects = new ArrayList<>();
        ListTag effectList = root.contains("active_effects", Tag.TAG_LIST)
                ? root.getList("active_effects", Tag.TAG_COMPOUND)
                : root.getList("ActiveEffects", Tag.TAG_COMPOUND);
        for (int i = 0; i < effectList.size(); i++) {
            CompoundTag e = effectList.getCompound(i);
            effects.add(new VisionInfoResultPacket.Effect(
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

    private VisionInfoResultPacket.PlayerData buildPlayerData(String name, UUID uuid, boolean online,
            float health, float maxHealth, int armor, int food, double x, double y, double z, String dimension,
            List<ItemStack> inventory, List<ItemStack> ender, List<VisionInfoResultPacket.Effect> effects) {
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        return new VisionInfoResultPacket.PlayerData(name, uuid, online, health, maxHealth, armor, food,
                x, y, z, dimension, inventory, ender, effects,
                am.canReturnByDeath(uuid), am.canUseDomain(uuid),
                am.canUseSloth(uuid), am.getSlothVariant(uuid).ordinal(),
                am.canUseGreed(uuid), am.getGreedVariant(uuid).ordinal());
    }

    private Optional<ItemStack> parseStack(CompoundTag entry) {
        try {
            return Optional.of(ItemStack.of(entry));
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
            net.minecraft.world.entity.EquipmentSlot slot =
                    net.minecraft.world.entity.LivingEntity.getEquipmentSlotForItem(stack);
            for (net.minecraft.world.entity.ai.attributes.AttributeModifier modifier
                    : stack.getAttributeModifiers(slot).get(Attributes.ARMOR)) {
                total += modifier.getAmount();
            }
        }
        return (int) Math.round(total);
    }

    private VisionInfoResultPacket lookUpEntity(String query) {
        EntityType<?> type = resolveEntityType(query);
        if (type == null) return VisionInfoResultPacket.notFound(query);
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);

        ServerLevel level = server.overworld();
        float maxHealth = 0;
        int armor = 0;
        ResourceLocation lootKey = null;
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

        List<VisionInfoResultPacket.Drop> drops = lootKey == null ? List.of() : analyzeDrops(lootKey);
        int loadedCount = countLoaded(type);
        String name = type.getDescription().getString();
        return VisionInfoResultPacket.ofEntity(query, new VisionInfoResultPacket.EntityData(
                id.toString(), name, maxHealth, armor, drops, loadedCount));
    }

    @Nullable
    private EntityType<?> resolveEntityType(String query) {
        ResourceLocation id = ResourceLocation.tryParse(query.contains(":") ? query : "minecraft:" + query);
        EntityType<?> byId = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (byId != null) return byId;

        String normalizedQuery = normalizeEntityName(query);
        if (normalizedQuery.isEmpty()) return null;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (normalizeEntityName(type.getDescription().getString()).equals(normalizedQuery)) {
                return type;
            }
        }
        return null;
    }

    private static String normalizeEntityName(String name) {
        return name.toLowerCase(java.util.Locale.ROOT).replaceAll("[\\s_]+", "");
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


    private List<VisionInfoResultPacket.Drop> analyzeDrops(ResourceLocation lootKey) {
        List<VisionInfoResultPacket.Drop> drops = new ArrayList<>();
        try {
            LootTable table = server.getLootData().getLootTable(lootKey);
            if (table == LootTable.EMPTY) return drops;

            for (LootPool pool : ((LootTableAccessor) table).hahueuh$getPools()) {
                float poolChance = conditionChanceMultiplier(((LootPoolAccessor) pool).hahueuh$getConditions());

                List<LootPoolSingletonContainer> leaves = new ArrayList<>();
                for (LootPoolEntryContainer entry : ((LootPoolAccessor) pool).hahueuh$getEntries()) {
                    flattenEntries(entry, leaves);
                }

                int totalWeight = 0;
                for (LootPoolSingletonContainer s : leaves) {
                    totalWeight += ((LootPoolSingletonAccessor) s).hahueuh$getWeight();
                }
                if (totalWeight <= 0) continue;

                for (LootPoolSingletonContainer entry : leaves) {
                    if (!(entry instanceof LootItem item)) continue;
                    int weight = ((LootPoolSingletonAccessor) item).hahueuh$getWeight();
                    float chance = poolChance * ((float) weight / totalWeight)
                            * conditionChanceMultiplier(((LootPoolEntryContainerAccessor) item).hahueuh$getConditions());

                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(
                            ((LootItemAccessor) item).hahueuh$getItem());
                    int[] countRange = countRange(item);
                    drops.add(new VisionInfoResultPacket.Drop(itemId.toString(), chance, countRange[0], countRange[1]));
                }
            }
        } catch (Exception e) {
            HahUeuh.LOGGER.warn("Vision of Information: could not analyze drops for {}", lootKey, e);
        }
        return drops;
    }

    private void flattenEntries(LootPoolEntryContainer entry, List<LootPoolSingletonContainer> out) {
        if (entry instanceof LootPoolSingletonContainer singleton) {
            out.add(singleton);
        } else if (entry instanceof CompositeEntryBase composite) {
            for (LootPoolEntryContainer child : ((CompositeEntryAccessor) composite).hahueuh$getChildren()) {
                flattenEntries(child, out);
            }
        }
    }

    private float conditionChanceMultiplier(LootItemCondition[] conditions) {
        float chance = 1.0f;
        for (LootItemCondition condition : conditions) {
            if (condition instanceof LootItemRandomChanceCondition rc) {
                chance *= ((RandomChanceConditionAccessor) rc).hahueuh$getProbability();
            } else if (condition instanceof LootItemRandomChanceWithLootingCondition rb) {
                chance *= ((RandomChanceLootingConditionAccessor) rb).hahueuh$getPercent();
            }
        }
        return chance;
    }

    private int[] countRange(LootPoolSingletonContainer entry) {
        float min = 1, max = 1;
        for (LootItemFunction fn : ((LootPoolSingletonAccessor) entry).hahueuh$getFunctions()) {
            if (fn instanceof SetItemCountFunction setCount) {
                NumberProvider value = ((SetItemCountFunctionAccessor) setCount).hahueuh$getValue();
                boolean add = ((SetItemCountFunctionAccessor) setCount).hahueuh$isAdd();
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
            float v = ((NumberProviderAccessors) (Object) cv).hahueuh$getValue();
            return new float[]{v, v};
        }
        if (provider instanceof UniformGenerator ug) {
            NumberProvider minProvider = ((UniformGeneratorAccessor) ug).hahueuh$getMin();
            NumberProvider maxProvider = ((UniformGeneratorAccessor) ug).hahueuh$getMax();
            if (minProvider == null || maxProvider == null) return null;
            float[] lo = providerRange(minProvider);
            float[] hi = providerRange(maxProvider);
            if (lo == null || hi == null) return null;
            return new float[]{lo[0], hi[1]};
        }
        return null;
    }

    private static String effectId(net.minecraft.world.effect.MobEffect effect) {
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
        return id == null ? "minecraft:unknown" : id.toString();
    }
}
