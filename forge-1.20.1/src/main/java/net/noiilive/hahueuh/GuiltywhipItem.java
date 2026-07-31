package net.noiilive.hahueuh;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;

public final class GuiltywhipItem extends Item {
    public static final double ATTACK_DAMAGE = 10.0;
    public static final double ATTACK_SPEED = 2.0;

    public GuiltywhipItem(Properties properties) {
        super(properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) return super.getAttributeModifiers(slot, stack);
        Multimap<Attribute, AttributeModifier> map = HashMultimap.create();
        map.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID,
                "Weapon modifier", ATTACK_DAMAGE - 1.0, AttributeModifier.Operation.ADDITION));
        map.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID,
                "Weapon modifier", ATTACK_SPEED - 4.0, AttributeModifier.Operation.ADDITION));
        return map;
    }

    @Override
    public int getEnchantmentValue() {
        return 14;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(Items.LEATHER);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (enchantment == Enchantments.SWEEPING_EDGE) return false;
        if (enchantment.category == EnchantmentCategory.WEAPON) return true;
        return super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state,
                                  net.minecraft.world.level.Level level,
                                  net.minecraft.core.BlockPos pos,
                                  net.minecraft.world.entity.player.Player player) {
        return player.isCreative();
    }
}
