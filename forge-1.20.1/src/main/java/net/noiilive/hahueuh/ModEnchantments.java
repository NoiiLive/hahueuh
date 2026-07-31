package net.noiilive.hahueuh;

import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, HahUeuh.MODID);

    public static final EnchantmentCategory MACE_CATEGORY =
            EnchantmentCategory.create("hahueuh_mace",
                    item -> item instanceof MorningstarItem || item instanceof SpikedClubItem);

    private static final float BREACH_PER_LEVEL = 0.15f;

    public static boolean isMaceWeapon(ItemStack stack) {
        return stack.getItem() instanceof MorningstarItem || stack.getItem() instanceof SpikedClubItem;
    }

    public static int levelOn(RegistryObject<Enchantment> enchantment, ItemStack stack) {
        if (!isMaceWeapon(stack)) return 0;
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment.get(), stack);
    }

    public static int breachLevel(DamageSource source) {
        if (!(source.getDirectEntity() instanceof LivingEntity living)) return 0;
        return levelOn(BREACH, living.getMainHandItem());
    }

    public static float armorEffectiveness(float base, int breachLevel) {
        return Mth.clamp(base - BREACH_PER_LEVEL * breachLevel, 0.0f, 1.0f);
    }

    private static final EquipmentSlot[] HAND = new EquipmentSlot[]{EquipmentSlot.MAINHAND};

    public static final RegistryObject<Enchantment> DENSITY =
            ENCHANTMENTS.register("density", () -> new MorningstarEnchantment(
                    Enchantment.Rarity.COMMON, 5, 5, 8));

    public static final RegistryObject<Enchantment> BREACH =
            ENCHANTMENTS.register("breach", () -> new MorningstarEnchantment(
                    Enchantment.Rarity.RARE, 4, 15, 20));

    public static final RegistryObject<Enchantment> WIND_BURST =
            ENCHANTMENTS.register("wind_burst", () -> new MorningstarEnchantment(
                    Enchantment.Rarity.RARE, 3, 15, 25));

    public static class MorningstarEnchantment extends Enchantment {
        private final int maxLevel;
        private final int minCost;
        private final int costStep;

        MorningstarEnchantment(Rarity rarity, int maxLevel, int minCost, int costStep) {
            super(rarity, MACE_CATEGORY, HAND);
            this.maxLevel = maxLevel;
            this.minCost = minCost;
            this.costStep = costStep;
        }

        @Override
        public int getMaxLevel() {
            return maxLevel;
        }

        @Override
        public int getMinCost(int level) {
            return minCost + (level - 1) * costStep;
        }

        @Override
        public int getMaxCost(int level) {
            return getMinCost(level) + 50;
        }

        @Override
        protected boolean checkCompatibility(Enchantment other) {
            if (!super.checkCompatibility(other)) return false;
            boolean selfExclusive = this == DENSITY.get() || this == BREACH.get();
            boolean otherExclusive = other == DENSITY.get() || other == BREACH.get();
            return !(selfExclusive && otherExclusive);
        }
    }

    private ModEnchantments() {}
}
