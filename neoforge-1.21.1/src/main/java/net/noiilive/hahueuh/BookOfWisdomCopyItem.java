package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.BoundVisionAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public final class BookOfWisdomCopyItem extends Item {
    public BookOfWisdomCopyItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Integer bound = stack.get(ModDataComponents.BOUND_VISION_ABILITY.get());
        if (bound == null) return super.getName(stack);
        BoundVisionAbility ability = BoundVisionAbility.byOrdinal(bound);
        if (ability == null) return super.getName(stack);
        return Component.translatable("hahueuh.item.book_of_wisdom.bound", Component.translatable(ability.translationKey));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Integer bound = stack.get(ModDataComponents.BOUND_VISION_ABILITY.get());
        if (bound == null) {
            tooltip.add(Component.translatable("hahueuh.item.book_of_wisdom.tooltip_unbound").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}
