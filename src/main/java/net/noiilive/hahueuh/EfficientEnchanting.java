package net.noiilive.hahueuh;

import net.noiilive.hahueuh.menu.EfficientEnchantingMenu;
import net.noiilive.hahueuh.network.EfficientEnchantOptionsPayload;
import net.noiilive.hahueuh.network.GreedVariant;
import net.noiilive.hahueuh.snapshot.PlayerAuthorityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EfficientEnchanting {
    private static final int MAX_TABLE_BASE_LEVEL = 30;

    public void open(ServerPlayer player) {
        if (!gate(player)) {
            player.displayClientMessage(Component.translatable("hahueuh.message.echidna_needs_book")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos table = findNearbyTable(level, player.blockPosition());
        if (table == null) {
            player.displayClientMessage(Component.translatable("hahueuh.message.efficient_enchanting_no_table")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        ContainerLevelAccess access = ContainerLevelAccess.create(level, table);
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new EfficientEnchantingMenu(id, inv, access),
                Component.translatable("hahueuh.gui.efficient_enchanting.title")));
    }

    public void goBack(ServerPlayer player) {
        if (!(player.containerMenu instanceof EfficientEnchantingMenu menu)) return;
        ItemStack item = menu.itemSlot().copy();
        ItemStack lapis = menu.lapisSlot().copy();
        ContainerLevelAccess access = menu.access();
        menu.setItemSlot(ItemStack.EMPTY);
        menu.setLapisSlot(ItemStack.EMPTY);

        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new EnchantmentMenu(id, inv, access),
                Component.translatable("container.enchant")));
        if (player.containerMenu instanceof EnchantmentMenu enchantMenu) {
            enchantMenu.getSlot(0).set(item);
            enchantMenu.getSlot(1).set(lapis);
        }
    }

    public void pushOptions(ServerPlayer player, EfficientEnchantingMenu menu) {
        List<EfficientEnchantOptionsPayload.Option> options = optionsFor(player, menu.itemSlot());
        PacketDistributor.sendToPlayer(player, new EfficientEnchantOptionsPayload(options));
    }

    public void select(ServerPlayer player, String enchantmentId) {
        if (!(player.containerMenu instanceof EfficientEnchantingMenu menu)) return;
        if (!gate(player)) {
            player.displayClientMessage(Component.translatable("hahueuh.message.echidna_needs_book")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        ItemStack item = menu.itemSlot();
        if (item.isEmpty() || !isEligible(item)) return;

        int level = optionsFor(player, item).stream()
                .filter(o -> o.enchantmentId().equals(enchantmentId))
                .mapToInt(EfficientEnchantOptionsPayload.Option::level)
                .findFirst().orElse(0);
        if (level <= 0) return;

        ResourceLocation id = ResourceLocation.tryParse(enchantmentId);
        if (id == null) return;
        Holder<Enchantment> holder = player.serverLevel().registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ResourceKey.create(Registries.ENCHANTMENT, id)).orElse(null);
        if (holder == null) return;

        boolean free = player.hasInfiniteMaterials();
        ItemStack lapis = menu.lapisSlot();
        if (!free && (player.experienceLevel < EfficientEnchantingMenu.XP_COST
                || lapis.getCount() < EfficientEnchantingMenu.LAPIS_COST)) {
            player.displayClientMessage(Component.translatable("hahueuh.message.efficient_enchanting_cost",
                    EfficientEnchantingMenu.XP_COST, EfficientEnchantingMenu.LAPIS_COST).withStyle(ChatFormatting.RED), true);
            return;
        }

        List<EnchantmentInstance> applied = List.of(new EnchantmentInstance(holder, level));
        ItemStack result = item.getItem().applyEnchantments(item, applied);
        menu.setItemSlot(result);

        if (!free) {
            player.giveExperienceLevels(-EfficientEnchantingMenu.XP_COST);
            lapis.consume(EfficientEnchantingMenu.LAPIS_COST, player);
            if (lapis.isEmpty()) menu.setLapisSlot(ItemStack.EMPTY);
        }
        player.awardStat(Stats.ENCHANT_ITEM);
        menu.access().execute((lvl, pos) -> lvl.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.BLOCKS, 1.0F, lvl.random.nextFloat() * 0.1F + 0.9F));

        pushOptions(player, menu);
        menu.broadcastChanges();
    }

    public List<EfficientEnchantOptionsPayload.Option> optionsFor(ServerPlayer player, ItemStack stack) {
        List<EfficientEnchantOptionsPayload.Option> out = new ArrayList<>();
        if (stack.isEmpty() || !isEligible(stack)) return out;

        int enchantValue = stack.getEnchantmentValue();
        if (enchantValue <= 0) return out;

        var registry = player.serverLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Optional<HolderSet.Named<Enchantment>> tableSet = registry.getTag(EnchantmentTags.IN_ENCHANTING_TABLE);
        if (tableSet.isEmpty()) return out;

        // The strongest modified enchant level a table could ever produce for this item.
        int maxModified = Math.round((MAX_TABLE_BASE_LEVEL + 1 + 2 * (enchantValue / 4)) * 1.15f);
        ItemEnchantments existing = EnchantmentHelper.getEnchantmentsForCrafting(stack);

        for (Holder<Enchantment> holder : tableSet.get()) {
            if (!stack.isPrimaryItemFor(holder)) continue;
            if (!compatibleWithExisting(holder, existing)) continue;

            Enchantment enchantment = holder.value();
            int maxLevel = 0;
            for (int lvl = enchantment.getMaxLevel(); lvl >= enchantment.getMinLevel(); lvl--) {
                if (maxModified >= enchantment.getMinCost(lvl)) {
                    maxLevel = lvl;
                    break;
                }
            }
            if (maxLevel <= 0) continue;
            if (existing.getLevel(holder) >= maxLevel) continue; // already at/above the table cap

            final int level = maxLevel;
            holder.unwrapKey().ifPresent(key ->
                    out.add(new EfficientEnchantOptionsPayload.Option(key.location().toString(), level)));
        }

        out.sort((a, b) -> a.enchantmentId().compareTo(b.enchantmentId()));
        return out;
    }

    private static boolean isEligible(ItemStack stack) {
        return stack.getItem().isEnchantable(stack);
    }

    private static boolean compatibleWithExisting(Holder<Enchantment> candidate, ItemEnchantments existing) {
        for (Holder<Enchantment> present : existing.keySet()) {
            if (present.value() == candidate.value()) continue; // upgrading the same enchantment is fine
            if (!Enchantment.areCompatible(candidate, present)) return false;
        }
        return true;
    }

    private boolean gate(ServerPlayer player) {
        PlayerAuthorityManager am = HahUeuh.SNAPSHOT_MANAGER.getAuthorityManager();
        return am.canUseGreed(player.getUUID())
                && am.getGreedVariant(player.getUUID()) == GreedVariant.ECHIDNA
                && HahUeuh.BOOK_OF_WISDOM.isSummoned(player.getUUID());
    }

    private static BlockPos findNearbyTable(ServerLevel level, BlockPos origin) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -3, -4), origin.offset(4, 3, 4))) {
            if (!level.getBlockState(pos).is(Blocks.ENCHANTING_TABLE)) continue;
            double d = pos.distSqr(origin);
            if (d < bestDist) {
                bestDist = d;
                best = pos.immutable();
            }
        }
        return best;
    }
}
