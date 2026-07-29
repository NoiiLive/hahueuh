package net.noiilive.hahueuh;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

import java.util.List;

public final class ElVita {
    public void tryCast(ServerPlayer caster) {
        if (caster.isShiftKeyDown() && HahUeuh.INCREASED_GRAVITY.isActive(caster)) {
            clear(caster);
            actionBar(caster, "hahueuh.message.el_vita_self_ended", ChatFormatting.GRAY);
            return;
        }
        net.noiilive.hahueuh.magic.SpellRegistry.get(net.noiilive.hahueuh.magic.Spells.EL_VITA)
                .ifPresent(spell -> HahUeuh.SPELL_CASTING.tryStart(caster, spell));
    }

    public void cast(ServerPlayer caster) {
        if (caster.isShiftKeyDown()) {
            if (HahUeuh.INCREASED_GRAVITY.isActive(caster)) {
                clear(caster);
                actionBar(caster, "hahueuh.message.el_vita_self_ended", ChatFormatting.GRAY);
            } else {
                apply(caster, MobEffectInstance.INFINITE_DURATION);
                actionBar(caster, "hahueuh.message.el_vita_self_started", ChatFormatting.AQUA);
            }
            return;
        }

        LivingEntity target = resolveTarget(caster);
        if (target == null) {
            actionBar(caster, "hahueuh.message.el_vita_no_target", ChatFormatting.RED);
            return;
        }

        apply(target, ConfigMagicYin.EL_VITA_TARGET_DURATION_SECONDS.get() * 20);
        actionBar(caster, "hahueuh.message.el_vita_target_started", ChatFormatting.AQUA);
    }

    private LivingEntity resolveTarget(ServerPlayer caster) {
        double range = ConfigMagicYin.EL_VITA_RANGE.get();
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(caster,
                e -> e != caster && e.isAlive() && !e.isSpectator() && e instanceof LivingEntity, range);
        return hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le ? le : null;
    }

    private void apply(LivingEntity target, int durationTicks) {
        target.addEffect(new MobEffectInstance(ModEffects.INCREASED_GRAVITY,
                durationTicks, IncreasedGravity.EL_VITA_AMPLIFIER, false, true, true));
        HahUeuh.INCREASED_GRAVITY.refreshModifier(target);

        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    36, 0.4, target.getBbHeight() * 0.35, 0.4, 0.02);
            level.sendParticles(ParticleTypes.SQUID_INK,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    16, 0.3, target.getBbHeight() * 0.3, 0.3, 0.01);
            level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8f, 0.4f);
        }
    }

    public void clear(LivingEntity target) {
        target.removeEffect(ModEffects.INCREASED_GRAVITY);
        HahUeuh.INCREASED_GRAVITY.removeModifier(target);

        if (target.level() instanceof ServerLevel level) {
            level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 1.2f);
        }
    }

    @SubscribeEvent
    public void onFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (!HahUeuh.INCREASED_GRAVITY.isElVitaTier(entity)) return;
        float distance = event.getDistance();
        if (distance < ConfigMagicYin.EL_VITA_CRATER_MIN_FALL_BLOCKS.get()) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        slam(level, entity, distance);
    }

    private void slam(ServerLevel level, LivingEntity entity, float distance) {
        BlockPos impact = entity.blockPosition();
        boolean heavy = distance > ConfigMagicYin.EL_VITA_CRATER_MIN_FALL_BLOCKS.get() * 2;

        level.levelEvent(LevelEvent.PARTICLES_SMASH_ATTACK, entity.getOnPos(), heavy ? 750 : 500);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                impact.getX() + 0.5, impact.getY() + 0.2, impact.getZ() + 0.5,
                30, ConfigMagicYin.EL_VITA_CRATER_RADIUS.get() * 0.4, 0.2,
                ConfigMagicYin.EL_VITA_CRATER_RADIUS.get() * 0.4, 0.05);

        crush(level, entity, distance);
        shockwave(level, entity, heavy);

        if (level.getGameRules().getBoolean(ModGameRules.REZERO_BLOCK_DESTRUCTION)) {
            createCrater(level, impact);
        }
    }

    private void crush(ServerLevel level, LivingEntity entity, float distance) {
        double multiplier = ConfigMagicYin.EL_VITA_SMASH_DAMAGE_MULTIPLIER.get();
        if (multiplier <= 0.0) return;
        float damage = (float) (smashDamage(distance) * multiplier);
        if (damage <= 0.0f) return;

        DamageSource source = entity instanceof Player player
                ? entity.damageSources().playerAttack(player)
                : entity.damageSources().mobAttack(entity);

        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                entity.getBoundingBox().inflate(0.3, 0.5, 0.3), other -> isValidVictim(entity, other))) {
            victim.hurt(source, damage);
        }
    }

    private static float smashDamage(float distance) {
        if (distance <= 3.0f) return 4.0f * distance;
        if (distance <= 8.0f) return 12.0f + 2.0f * (distance - 3.0f);
        return 22.0f + distance - 8.0f;
    }

    private void shockwave(ServerLevel level, LivingEntity entity, boolean heavy) {
        double radius = ConfigMagicYin.EL_VITA_SMASH_RADIUS.get();
        double power = ConfigMagicYin.EL_VITA_SMASH_KNOCKBACK.get() * (heavy ? 2.0 : 1.0);
        if (power <= 0.0) return;

        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                entity.getBoundingBox().inflate(radius), other -> isValidVictim(entity, other));
        for (LivingEntity other : nearby) {
            Vec3 offset = other.position().subtract(entity.position());
            double distance = offset.length();
            if (distance > radius) continue;
            double strength = (radius - distance) * power
                    * (1.0 - other.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            if (strength <= 0.0) continue;
            Vec3 push = offset.normalize().scale(strength);
            other.push(push.x, 0.7, push.z);
            if (other instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
            }
        }
    }

    private static boolean isValidVictim(LivingEntity source, LivingEntity other) {
        if (other == source || other.isSpectator() || !other.isAlive()) return false;
        if (source.isAlliedTo(other)) return false;
        if (other instanceof ArmorStand stand && stand.isMarker()) return false;
        if (other instanceof TamableAnimal tamable && tamable.isTame()
                && source.getUUID().equals(tamable.getOwnerUUID())) return false;
        return true;
    }

    private void createCrater(ServerLevel level, BlockPos centre) {
        double radius = ConfigMagicYin.EL_VITA_CRATER_RADIUS.get();
        int depth = ConfigMagicYin.EL_VITA_CRATER_DEPTH.get();

        int r = (int) Math.ceil(radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (Math.sqrt(dx * dx + dz * dz) > radius) continue;
                for (int dy = 0; dy > -depth; dy--) {
                    BlockPos pos = centre.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty()) continue;
                    if (state.getDestroySpeed(level, pos) < 0) continue;
                    if (state.hasBlockEntity()) continue;

                    level.destroyBlock(pos, ModGameRules.rollDrops(level));
                }
            }
        }
    }

    private static void actionBar(ServerPlayer player, String key, ChatFormatting style) {
        player.displayClientMessage(Component.translatable(key).withStyle(style), true);
    }
}
