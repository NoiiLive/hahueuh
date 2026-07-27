package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.BoundVisionAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class BookOfWisdomCopyItem extends Item {
    private static final String NBT_BOUND = "BoundVisionAbility";
    private static final String NBT_COOLDOWN_UNTIL = "VisionCooldownUntil";

    public BookOfWisdomCopyItem(Properties properties) {
        super(properties);
    }

    public static Integer boundAbility(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(NBT_BOUND)) return null;
        return stack.getTag().getInt(NBT_BOUND);
    }

    public static void setBoundAbility(ItemStack stack, int ordinal) {
        stack.getOrCreateTag().putInt(NBT_BOUND, ordinal);
    }

    public static Long visionCooldownUntil(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(NBT_COOLDOWN_UNTIL)) return null;
        return stack.getTag().getLong(NBT_COOLDOWN_UNTIL);
    }

    public static void setVisionCooldownUntil(ItemStack stack, long gameTime) {
        stack.getOrCreateTag().putLong(NBT_COOLDOWN_UNTIL, gameTime);
    }

    @Override
    public Component getName(ItemStack stack) {
        Integer bound = boundAbility(stack);
        if (bound == null) return super.getName(stack);
        BoundVisionAbility ability = BoundVisionAbility.byOrdinal(bound);
        if (ability == null) return super.getName(stack);
        return Component.translatable("hahueuh.item.book_of_wisdom.bound", Component.translatable(ability.translationKey));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        if (boundAbility(stack) == null) {
            tooltip.add(Component.translatable("hahueuh.item.book_of_wisdom.tooltip_unbound")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}
