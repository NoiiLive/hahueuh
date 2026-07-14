package net.noiilive.hahueuh;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.Unbreakable;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HahUeuh.MODID);

    public static final DeferredItem<BookOfWisdomItem> MEMORIES_OF_THE_WORLD = ITEMS.registerItem(
            "memories_of_the_world", BookOfWisdomItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    public static final DeferredItem<BookOfWisdomCopyItem> BOOK_OF_WISDOM_COPY = ITEMS.registerItem(
            "book_of_wisdom", BookOfWisdomCopyItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<SagesBoxItem> SAGES_BOX = ITEMS.registerItem(
            "sages_box", SagesBoxItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<DragonSwordReidItem> DRAGON_SWORD_REID = ITEMS.registerItem(
            "dragon_sword_reid",
            props -> new DragonSwordReidItem(Tiers.NETHERITE, props),
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(false))
                    .attributes(DragonSwordReidItem.attributesFor(true))
                    .component(DataComponents.LORE, new ItemLore(List.of(
                            Component.translatable("item.hahueuh.dragon_sword_reid.lore_1"),
                            Component.translatable("item.hahueuh.dragon_sword_reid.lore_2"),
                            Component.translatable("item.hahueuh.dragon_sword_reid.lore_3"),
                            Component.translatable("item.hahueuh.dragon_sword_reid.lore_4"),
                            Component.translatable("item.hahueuh.dragon_sword_reid.lore_5")))));

    private ModItems() {}
}
