package net.noiilive.hahueuh;

import net.noiilive.hahueuh.network.WitchFactorAuthority;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class SagesBoxItem extends Item {
    public SagesBoxItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        WitchFactorAuthority authority = trappedAuthority(stack);
        if (authority == null) return super.getName(stack);
        return Component.translatable("hahueuh.item.sages_box.filled", Component.translatable(authority.translationKey));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        WitchFactorAuthority authority = trappedAuthority(stack);
        tooltip.add(Component.translatable(authority == null
                        ? "hahueuh.item.sages_box.tooltip_empty" : "hahueuh.item.sages_box.tooltip_filled")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof WitchFactorEntity witchFactor)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.CONSUME;

        if (trappedAuthority(stack) != null) {
            player.displayClientMessage(Component.translatable("hahueuh.message.sages_box_full")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        WitchFactorAuthority authority = witchFactor.getAssignedAuthority();
        if (authority == WitchFactorAuthority.NONE) {
            player.displayClientMessage(Component.translatable("hahueuh.message.sages_box_no_sin")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        stack.set(ModDataComponents.SAGES_BOX_SIN.get(), authority.id);
        HahUeuh.MOB_WITCH_FACTOR.unregisterWandering(witchFactor);
        witchFactor.discard();
        player.displayClientMessage(Component.translatable("hahueuh.message.sages_box_trapped",
                Component.translatable(authority.translationKey)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        WitchFactorAuthority authority = trappedAuthority(stack);
        if (authority == null) return InteractionResultHolder.pass(stack);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.consume(stack);
        }

        stack.remove(ModDataComponents.SAGES_BOX_SIN.get());

        if (player.isShiftKeyDown()) {
            WitchFactorGrant.revokeAllForRelease(serverPlayer.serverLevel().getServer(), authority);

            WitchFactorEntity entity = new WitchFactorEntity(ModEntities.WITCH_FACTOR.get(), (ServerLevel) level);
            Vec3 pos = releasePosition(player);
            entity.moveTo(pos.x, pos.y, pos.z, 0.0f, 0.0f);
            entity.setAssignedAuthority(authority);
            level.addFreshEntity(entity);
            HahUeuh.MOB_WITCH_FACTOR.registerWandering(entity);
            player.displayClientMessage(Component.translatable("hahueuh.message.sages_box_released",
                    Component.translatable(authority.translationKey)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        } else {
            WitchFactorGrant.grant(serverPlayer, authority);
            player.displayClientMessage(Component.translatable("hahueuh.message.sages_box_absorbed",
                    Component.translatable(authority.translationKey)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        return InteractionResultHolder.success(stack);
    }

    private static final double RELEASE_REACH = 5.0;

    private static Vec3 releasePosition(Player player) {
        HitResult hit = player.pick(RELEASE_REACH, 1.0f, false);
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
            return Vec3.atCenterOf(blockHit.getBlockPos().relative(blockHit.getDirection()));
        }
        return player.getEyePosition().add(player.getLookAngle().scale(RELEASE_REACH));
    }

    private static WitchFactorAuthority trappedAuthority(ItemStack stack) {
        String id = stack.get(ModDataComponents.SAGES_BOX_SIN.get());
        return id == null ? null : WitchFactorAuthority.byId(id);
    }
}
