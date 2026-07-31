package net.noiilive.hahueuh;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.ItemAttributeModifiers;
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

    public static final DeferredItem<MorningstarItem> MORNINGSTAR = ITEMS.registerItem(
            "morningstar",
            MorningstarItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).durability(500)
                    .attributes(MorningstarItem.attributes()));

    public static final DeferredItem<GuiltywhipItem> GUILTYWHIP = ITEMS.registerItem(
            "guiltywhip",
            GuiltywhipItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<DragonSwordReidItem> DRAGON_SWORD_REID = ITEMS.registerItem(
            "dragon_sword_reid",
            props -> new DragonSwordReidItem(Tiers.NETHERITE, props),
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant()
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(false))
                    .attributes(DragonSwordReidItem.attributesFor(true))
                    .component(DataComponents.LORE, new ItemLore(List.of(
                            Component.translatable("item.hahueuh.dragon_sword_reid.lore_1"),
                            Component.translatable("item.hahueuh.dragon_sword_reid.lore_2"),
                            Component.translatable("item.hahueuh.dragon_sword_reid.lore_3"),
                            Component.translatable("item.hahueuh.dragon_sword_reid.lore_4"),
                            Component.translatable("item.hahueuh.dragon_sword_reid.lore_5")))));

    public static final DeferredItem<SwordItem> BOWEL_HUNTER_KUKRI = ITEMS.registerItem(
            "bowel_hunter_kukri",
            props -> new SwordItem(Tiers.IRON, props),
            new Item.Properties().rarity(Rarity.EPIC).attributes(swordAttributes(8.0, 2.0)));

    public static final DeferredItem<SwordItem> BOWEL_HUNTER_WHITE_BLADE = ITEMS.registerItem(
            "bowel_hunter_white_blade",
            props -> new SwordItem(Tiers.DIAMOND, props),
            new Item.Properties().rarity(Rarity.EPIC).attributes(swordAttributes(16.0, 1.2)));

    public static final DeferredItem<SwordItem> BOWEL_HUNTER_BLACK_BLADE = ITEMS.registerItem(
            "bowel_hunter_black_blade",
            props -> new SwordItem(Tiers.DIAMOND, props),
            new Item.Properties().rarity(Rarity.EPIC).attributes(swordAttributes(16.0, 1.2)));

    public static final DeferredItem<MaceItem> SPIKED_CLUB = ITEMS.registerItem(
            "spiked_club",
            MaceItem::new,
            new Item.Properties().durability(500).attributes(spikedClubAttributes())
                    .component(DataComponents.TOOL, MaceItem.createToolProperties()));

    private static ItemAttributeModifiers spikedClubAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 24.0 - 1.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, 0.6 - 4.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_KNOCKBACK,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "spiked_club_knockback"),
                                4.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    private static ItemAttributeModifiers swordAttributes(double totalDamage, double totalSpeed) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, totalDamage - 1.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, totalSpeed - 4.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    private ModItems() {}
}
