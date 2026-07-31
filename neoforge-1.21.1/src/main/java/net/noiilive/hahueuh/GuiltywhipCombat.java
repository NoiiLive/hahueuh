package net.noiilive.hahueuh;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public final class GuiltywhipCombat {
    private GuiltywhipCombat() {}

    public static float damageAt(ServerPlayer player, LivingEntity target, int node, DamageSource source) {
        ItemStack stack = player.getMainHandItem();
        ServerLevel level = player.serverLevel();
        float base = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * GuiltywhipPhysics.damageFraction(node));
        return EnchantmentHelper.modifyDamage(level, stack, target, source, base);
    }

    private static int enchantLevel(ServerLevel level, ResourceKey<Enchantment> key, ItemStack stack) {
        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(key)
                .map((Holder<Enchantment> holder) -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack))
                .orElse(0);
    }

    public static void applyHitEffects(ServerPlayer player, LivingEntity target) {
        ItemStack stack = player.getMainHandItem();
        int fire = enchantLevel(player.serverLevel(), Enchantments.FIRE_ASPECT, stack);
        if (fire > 0) target.igniteForSeconds(fire * 4.0f);
    }

    public static void damageWeapon(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GuiltywhipItem)) return;
        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
    }
}
