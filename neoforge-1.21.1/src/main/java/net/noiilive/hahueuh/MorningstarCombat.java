package net.noiilive.hahueuh;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public final class MorningstarCombat {
    private static final float MAX_RANGE_PENALTY = 5.0f;
    private static final float CLOSENESS_PER_BLOCK = 2.0f;
    private static final float DENSITY_PER_LEVEL_PER_BLOCK = 0.5f;
    private static final double WIND_BURST_BASE = 0.6;
    private static final double WIND_BURST_PER_LEVEL = 0.2;

    private MorningstarCombat() {}

    public static float headDamage(ServerPlayer player, LivingEntity target, double reach,
                                   DamageSource source) {
        ItemStack stack = player.getMainHandItem();
        ServerLevel level = player.serverLevel();
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        damage = EnchantmentHelper.modifyDamage(level, stack, target, source, damage);
        damage += closenessBonus(level, stack, reach);
        return damage;
    }

    public static float spinHeadDamage(ServerPlayer player, LivingEntity target,
                                       MorningstarPhysics.State state, DamageSource source) {
        ItemStack stack = player.getMainHandItem();
        ServerLevel level = player.serverLevel();
        float damage = MorningstarPhysics.spinMomentumDamage(state);
        return EnchantmentHelper.modifyDamage(level, stack, target, source, damage);
    }

    public static float chainDamage(ServerPlayer player) {
        return MorningstarPhysics.SPIN_DAMAGE;
    }

    private static float closenessBonus(ServerLevel level, ItemStack stack, double reach) {
        double clampedReach = Mth.clamp(reach, 0.0, MorningstarPhysics.CHAIN_LENGTH);
        double closeness = MorningstarPhysics.CHAIN_LENGTH - clampedReach;
        int density = enchantLevel(level, Enchantments.DENSITY, stack);
        float perBlock = CLOSENESS_PER_BLOCK + DENSITY_PER_LEVEL_PER_BLOCK * density;
        return (float) (perBlock * closeness) - MAX_RANGE_PENALTY;
    }

    private static int enchantLevel(ServerLevel level, ResourceKey<Enchantment> key, ItemStack stack) {
        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(key)
                .map((Holder<Enchantment> holder) -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack))
                .orElse(0);
    }

    public static void applyHeadEffects(ServerPlayer player, LivingEntity target) {
        ItemStack stack = player.getMainHandItem();
        ServerLevel level = player.serverLevel();

        int fire = enchantLevel(level, Enchantments.FIRE_ASPECT, stack);
        if (fire > 0) target.igniteForSeconds(fire * 4.0f);

        int wind = enchantLevel(level, Enchantments.WIND_BURST, stack);
        if (wind > 0) {
            double power = WIND_BURST_BASE + WIND_BURST_PER_LEVEL * wind;
            target.push(0.0, power, 0.0);
            target.hurtMarked = true;
            if (target instanceof ServerPlayer sp) {
                sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
            }
            level.sendParticles(ParticleTypes.GUST,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.0f, 0.9f);
        }
    }

    public static void damageWeapon(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof MorningstarItem)) return;
        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
    }
}
