package net.noiilive.hahueuh;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

import java.util.List;
import java.util.UUID;

public final class DragonSwordReidItem extends SwordItem {
    private static final double SHEATHED_DAMAGE = 10.0;
    private static final double UNSHEATHED_DAMAGE = 40.0;
    private static final double ATTACK_SPEED = -3.0;
    private static final double SHEATHED_KNOCKBACK = 1.0;
    private static final double UNSHEATHED_KNOCKBACK = 3.0;
    private static final String SHEATHED_TAG = "DragonSwordSheathed";
    private static final UUID KNOCKBACK_MODIFIER_ID =
            UUID.fromString("9c41a7d2-6b58-4e03-9f1d-2a7c85e6b310");

    public DragonSwordReidItem(Tier tier, Properties properties) {
        super(tier, 0, 0.0f, properties);
    }

    public static boolean isSheathed(ItemStack stack) {
        return !stack.hasTag() || !stack.getTag().contains(SHEATHED_TAG)
                || stack.getTag().getBoolean(SHEATHED_TAG);
    }

    public static void setSheathed(ItemStack stack, boolean sheathed) {
        stack.getOrCreateTag().putBoolean(SHEATHED_TAG, sheathed);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) return super.getAttributeModifiers(slot, stack);
        boolean sheathed = isSheathed(stack);
        double damage = sheathed ? SHEATHED_DAMAGE : UNSHEATHED_DAMAGE;
        Multimap<Attribute, AttributeModifier> map = HashMultimap.create();
        map.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID,
                "Weapon modifier", damage - 1.0, AttributeModifier.Operation.ADDITION));
        map.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID,
                "Weapon modifier", ATTACK_SPEED, AttributeModifier.Operation.ADDITION));
        map.put(Attributes.ATTACK_KNOCKBACK, new AttributeModifier(KNOCKBACK_MODIFIER_ID,
                "Weapon knockback", sheathed ? SHEATHED_KNOCKBACK : UNSHEATHED_KNOCKBACK,
                AttributeModifier.Operation.ADDITION));
        return map;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        if (ToolActions.SWORD_SWEEP.equals(toolAction) && isSheathed(stack)) return false;
        return super.canPerformAction(stack, toolAction);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                if (isSheathed(stack)) {
                    if (player.isCreative() || HahUeuh.DRAGON_SWORD_REID.canUnsheath(player)) {
                        setSheathed(stack, false);
                        level.playSound(null, player, SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS, 0.9f, 1.4f);
                    } else {
                        player.displayClientMessage(Component.translatable("hahueuh.message.dragon_sword_unworthy")
                                .withStyle(ChatFormatting.RED), true);
                    }
                } else {
                    setSheathed(stack, true);
                    level.playSound(null, player, SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.PLAYERS, 0.9f, 0.8f);
                }
            }
            return InteractionResultHolder.success(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.isInvulnerable()) {
            entity.setInvulnerable(true);
        }
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        for (int i = 1; i <= 5; i++) {
            tooltip.add(Component.translatable("item.hahueuh.dragon_sword_reid.lore_" + i));
        }
    }
}
