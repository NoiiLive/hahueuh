package net.noiilive.hahueuh;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

public final class DragonSwordReidItem extends SwordItem {
    private static final double SHEATHED_DAMAGE = 10.0;
    private static final double UNSHEATHED_DAMAGE = 40.0;
    private static final double ATTACK_SPEED = -2.4; // vanilla sword speed → 1.6 attacks/sec
    private static final double SHEATHED_KNOCKBACK = 1.0;
    private static final double UNSHEATHED_KNOCKBACK = 3.0;
    private static final ResourceLocation KNOCKBACK_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(HahUeuh.MODID, "dragon_sword_knockback");

    public DragonSwordReidItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    public static ItemAttributeModifiers attributesFor(boolean sheathed) {
        double total = sheathed ? SHEATHED_DAMAGE : UNSHEATHED_DAMAGE;
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, total - 1.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, ATTACK_SPEED, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_KNOCKBACK,
                        new AttributeModifier(KNOCKBACK_MODIFIER_ID,
                                sheathed ? SHEATHED_KNOCKBACK : UNSHEATHED_KNOCKBACK, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public static boolean isSheathed(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.DRAGON_SWORD_SHEATHED.get(), Boolean.TRUE);
    }

    public static void setSheathed(ItemStack stack, boolean sheathed) {
        stack.set(ModDataComponents.DRAGON_SWORD_SHEATHED.get(), sheathed);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, attributesFor(sheathed));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                if (isSheathed(stack)) {
                    if (HahUeuh.DRAGON_SWORD_REID.canUnsheath(player)) {
                        setSheathed(stack, false);
                        level.playSound(null, player, SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.PLAYERS, 0.9f, 1.4f);
                    } else {
                        player.displayClientMessage(Component.translatable("hahueuh.message.dragon_sword_unworthy")
                                .withStyle(ChatFormatting.RED), true);
                    }
                } else {
                    setSheathed(stack, true);
                    level.playSound(null, player, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 0.9f, 0.8f);
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
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
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
        return false; // unbreakable — never show a durability bar
    }
}
