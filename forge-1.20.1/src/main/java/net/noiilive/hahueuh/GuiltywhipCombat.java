package net.noiilive.hahueuh;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public final class GuiltywhipCombat {
    private GuiltywhipCombat() {}

    public static float damageAt(ServerPlayer player, LivingEntity target, int node) {
        ItemStack stack = player.getMainHandItem();
        float base = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * GuiltywhipPhysics.damageFraction(node));
        return base + EnchantmentHelper.getDamageBonus(stack, target.getMobType());
    }

    public static void applyHitEffects(ServerPlayer player, LivingEntity target) {
        ItemStack stack = player.getMainHandItem();
        int fire = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, stack);
        if (fire > 0) target.setSecondsOnFire(fire * 4);
    }

    public static void damageWeapon(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GuiltywhipItem)) return;
        stack.hurtAndBreak(1, player, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
    }
}
