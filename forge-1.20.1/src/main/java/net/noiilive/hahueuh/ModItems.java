package net.noiilive.hahueuh;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HahUeuh.MODID);

    public static final RegistryObject<SagesBoxItem> SAGES_BOX =
            ITEMS.register("sages_box", () -> new SagesBoxItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<BookOfWisdomCopyItem> BOOK_OF_WISDOM_COPY =
            ITEMS.register("book_of_wisdom", () -> new BookOfWisdomCopyItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<BookOfWisdomItem> MEMORIES_OF_THE_WORLD =
            ITEMS.register("memories_of_the_world", () -> new BookOfWisdomItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<DragonSwordReidItem> DRAGON_SWORD_REID =
            ITEMS.register("dragon_sword_reid", () -> new DragonSwordReidItem(Tiers.NETHERITE,
                    new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant()));

    public static final RegistryObject<SwordItem> BOWEL_HUNTER_KUKRI =
            ITEMS.register("bowel_hunter_kukri", () -> new SwordItem(Tiers.IRON,
                    swordDamage(8.0, Tiers.IRON), swordSpeed(2.0), new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<SwordItem> BOWEL_HUNTER_WHITE_BLADE =
            ITEMS.register("bowel_hunter_white_blade", () -> new SwordItem(Tiers.DIAMOND,
                    swordDamage(16.0, Tiers.DIAMOND), swordSpeed(1.2), new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<SwordItem> BOWEL_HUNTER_BLACK_BLADE =
            ITEMS.register("bowel_hunter_black_blade", () -> new SwordItem(Tiers.DIAMOND,
                    swordDamage(16.0, Tiers.DIAMOND), swordSpeed(1.2), new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<MorningstarItem> MORNINGSTAR =
            ITEMS.register("morningstar", () -> new MorningstarItem(Tiers.IRON,
                    new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).durability(500)));

    public static final RegistryObject<GuiltywhipItem> GUILTYWHIP =
            ITEMS.register("guiltywhip", () -> new GuiltywhipItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).durability(400)));

    public static final RegistryObject<SpikedClubItem> SPIKED_CLUB =
            ITEMS.register("spiked_club", () -> new SpikedClubItem(new Item.Properties().durability(500)));

    private static int swordDamage(double total, Tiers tier) {
        return (int) Math.round(total - 1.0 - tier.getAttackDamageBonus());
    }

    private static float swordSpeed(double total) {
        return (float) (total - 4.0);
    }

    private ModItems() {}
}
