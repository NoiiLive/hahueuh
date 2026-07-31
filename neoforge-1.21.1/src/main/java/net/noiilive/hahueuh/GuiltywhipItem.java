package net.noiilive.hahueuh;

import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public final class GuiltywhipItem extends Item {
    public static final double ATTACK_DAMAGE = 10.0;
    public static final double ATTACK_SPEED = 2.0;

    public GuiltywhipItem(Properties properties) {
        super(properties);
    }

    public static ItemAttributeModifiers attributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, ATTACK_DAMAGE - 1.0,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, ATTACK_SPEED - 4.0,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(Items.LEATHER);
    }

    @Override
    public int getEnchantmentValue() {
        return 14;
    }

    @Override
    public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state,
                                  net.minecraft.world.level.Level level,
                                  net.minecraft.core.BlockPos pos,
                                  net.minecraft.world.entity.player.Player player) {
        return player.isCreative();
    }
}
