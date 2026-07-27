package net.noiilive.hahueuh;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public final class SpikedClubItem extends Item {
    public static final float SMASH_ATTACK_FALL_THRESHOLD = 1.5f;
    private static final float SMASH_ATTACK_HEAVY_THRESHOLD = 5.0f;
    private static final double SMASH_ATTACK_KNOCKBACK_RADIUS = 3.5;
    private static final float SMASH_ATTACK_KNOCKBACK_POWER = 0.7f;

    private static final double ATTACK_DAMAGE = 24.0;
    private static final double ATTACK_SPEED = 0.6;
    private static final double ATTACK_KNOCKBACK = 4.0;
    private static final UUID KNOCKBACK_MODIFIER_ID =
            UUID.fromString("3d1f8a76-2c94-4b5e-8e07-1b6d4f2a9c88");

    public SpikedClubItem(Properties properties) {
        super(properties);
    }

    public static boolean canSmashAttack(LivingEntity entity) {
        return entity.fallDistance > SMASH_ATTACK_FALL_THRESHOLD && !entity.isFallFlying();
    }

    public static float smashBonusDamage(float fallDistance) {
        if (fallDistance <= 3.0f) return 4.0f * fallDistance;
        if (fallDistance <= 8.0f) return 12.0f + 2.0f * (fallDistance - 3.0f);
        return 22.0f + fallDistance - 8.0f;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) return super.getAttributeModifiers(slot, stack);
        Multimap<Attribute, AttributeModifier> map = HashMultimap.create();
        map.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID,
                "Weapon modifier", ATTACK_DAMAGE - 1.0, AttributeModifier.Operation.ADDITION));
        map.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID,
                "Weapon modifier", ATTACK_SPEED - 4.0, AttributeModifier.Operation.ADDITION));
        map.put(Attributes.ATTACK_KNOCKBACK, new AttributeModifier(KNOCKBACK_MODIFIER_ID,
                "Weapon knockback", ATTACK_KNOCKBACK, AttributeModifier.Operation.ADDITION));
        return map;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(Items.IRON_INGOT);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        if (attacker instanceof ServerPlayer serverplayer && canSmashAttack(serverplayer)) {
            ServerLevel serverlevel = (ServerLevel) attacker.level();
            serverplayer.setDeltaMovement(serverplayer.getDeltaMovement().multiply(1.0, 0.0, 1.0).add(0.0, 0.01, 0.0));
            serverplayer.connection.send(new ClientboundSetEntityMotionPacket(serverplayer));

            boolean heavy = serverplayer.fallDistance > SMASH_ATTACK_HEAVY_THRESHOLD;
            SoundEvent sound = target.onGround()
                    ? (heavy ? SoundEvents.ANVIL_LAND : SoundEvents.GENERIC_EXPLODE)
                    : SoundEvents.PLAYER_ATTACK_STRONG;
            serverlevel.playSound(null, serverplayer.getX(), serverplayer.getY(), serverplayer.getZ(),
                    sound, serverplayer.getSoundSource(), 1.0f, 1.0f);

            knockback(serverlevel, serverplayer, target);
            serverplayer.resetFallDistance();
        }
        return true;
    }

    private static void knockback(ServerLevel level, Player player, Entity struck) {
        level.sendParticles(ParticleTypes.EXPLOSION,
                struck.getX(), struck.getY(), struck.getZ(), 6, 0.4, 0.1, 0.4, 0.0);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                struck.getBoundingBox().inflate(SMASH_ATTACK_KNOCKBACK_RADIUS),
                knockbackPredicate(player, struck));
        for (LivingEntity victim : nearby) {
            Vec3 offset = victim.position().subtract(struck.position());
            double power = getKnockbackPower(player, victim, offset);
            if (power <= 0.0) continue;
            Vec3 push = offset.normalize().scale(power);
            victim.push(push.x, SMASH_ATTACK_KNOCKBACK_POWER, push.z);
            if (victim instanceof ServerPlayer sp) {
                sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
            }
        }
    }

    private static Predicate<LivingEntity> knockbackPredicate(Player player, Entity struck) {
        return candidate -> {
            if (candidate.isSpectator()) return false;
            if (candidate == player || candidate == struck) return false;
            if (player.isAlliedTo(candidate)) return false;
            if (candidate instanceof TamableAnimal tame && tame.isTame()
                    && player.getUUID().equals(tame.getOwnerUUID())) {
                return false;
            }
            if (candidate instanceof ArmorStand stand && stand.isMarker()) return false;
            return struck.distanceToSqr(candidate)
                    <= SMASH_ATTACK_KNOCKBACK_RADIUS * SMASH_ATTACK_KNOCKBACK_RADIUS;
        };
    }

    private static double getKnockbackPower(Player player, LivingEntity victim, Vec3 offset) {
        return (SMASH_ATTACK_KNOCKBACK_RADIUS - offset.length())
                * SMASH_ATTACK_KNOCKBACK_POWER
                * (player.fallDistance > SMASH_ATTACK_HEAVY_THRESHOLD ? 2 : 1)
                * (1.0 - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
    }
}
