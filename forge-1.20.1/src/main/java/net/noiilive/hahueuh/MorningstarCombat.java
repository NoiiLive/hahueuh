package net.noiilive.hahueuh;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public final class MorningstarCombat {
    private static final float MAX_RANGE_PENALTY = 5.0f;
    private static final float CLOSENESS_PER_BLOCK = 2.0f;
    private static final float DENSITY_PER_LEVEL_PER_BLOCK = 0.5f;
    private static final double WIND_BURST_BASE = 0.6;
    private static final double WIND_BURST_PER_LEVEL = 0.2;

    private MorningstarCombat() {}

    public static float headDamage(ServerPlayer player, LivingEntity target, double reach) {
        ItemStack stack = player.getMainHandItem();
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        damage += EnchantmentHelper.getDamageBonus(stack, target.getMobType());
        damage += closenessBonus(stack, reach);
        return damage;
    }

    public static float spinHeadDamage(ServerPlayer player, LivingEntity target,
                                       MorningstarPhysics.State state) {
        ItemStack stack = player.getMainHandItem();
        float damage = MorningstarPhysics.spinMomentumDamage(state);
        damage += EnchantmentHelper.getDamageBonus(stack, target.getMobType());
        return damage;
    }

    public static float chainDamage(ServerPlayer player) {
        return MorningstarPhysics.SPIN_DAMAGE;
    }

    private static float closenessBonus(ItemStack stack, double reach) {
        double clampedReach = Mth.clamp(reach, 0.0, MorningstarPhysics.CHAIN_LENGTH);
        double closeness = MorningstarPhysics.CHAIN_LENGTH - clampedReach;
        int level = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.DENSITY.get(), stack);
        float perBlock = CLOSENESS_PER_BLOCK + DENSITY_PER_LEVEL_PER_BLOCK * level;
        return (float) (perBlock * closeness) - MAX_RANGE_PENALTY;
    }

    public static void applyHeadEffects(ServerPlayer player, LivingEntity target) {
        ItemStack stack = player.getMainHandItem();
        ServerLevel level = player.serverLevel();

        int fire = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, stack);
        if (fire > 0) target.setSecondsOnFire(fire * 4);

        int wind = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.WIND_BURST.get(), stack);
        if (wind > 0) {
            double power = WIND_BURST_BASE + WIND_BURST_PER_LEVEL * wind;
            target.push(0.0, power, 0.0);
            target.hurtMarked = true;
            if (target instanceof ServerPlayer sp) {
                sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
            }
            level.sendParticles(ParticleTypes.CLOUD,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    12, 0.3, 0.2, 0.3, 0.05);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0f, 0.6f);
        }
    }

    public static void damageWeapon(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof MorningstarItem)) return;
        stack.hurtAndBreak(1, player, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
    }
}
