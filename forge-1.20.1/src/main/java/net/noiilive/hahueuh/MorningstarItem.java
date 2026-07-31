package net.noiilive.hahueuh;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

public final class MorningstarItem extends SwordItem {
    public static final double ATTACK_DAMAGE = 20.0;
    public static final double ATTACK_SPEED = 1.0;

    public MorningstarItem(Tier tier, Properties properties) {
        super(tier, 0, 0.0f, properties);
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
        return 15;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(Items.IRON_INGOT);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        return 1.0f;
    }

    @Override
    public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state,
                                  net.minecraft.world.level.Level level,
                                  net.minecraft.core.BlockPos pos,
                                  net.minecraft.world.entity.player.Player player) {
        return player.isCreative() && super.canAttackBlock(state, level, pos, player);
    }
}
