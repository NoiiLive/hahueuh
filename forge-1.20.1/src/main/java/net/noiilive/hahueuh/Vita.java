package net.noiilive.hahueuh;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class Vita {
    public void tryCast(ServerPlayer caster) {
        if (caster.isShiftKeyDown() && HahUeuh.INCREASED_GRAVITY.isActive(caster)) {
            clear(caster);
            actionBar(caster, "hahueuh.message.vita_self_ended", ChatFormatting.GRAY);
            return;
        }
        net.noiilive.hahueuh.magic.SpellRegistry.get(net.noiilive.hahueuh.magic.Spells.VITA)
                .ifPresent(spell -> HahUeuh.SPELL_CASTING.tryStart(caster, spell));
    }

    public void cast(ServerPlayer caster) {
        if (caster.isShiftKeyDown()) {
            if (HahUeuh.INCREASED_GRAVITY.isActive(caster)) {
                clear(caster);
                actionBar(caster, "hahueuh.message.vita_self_ended", ChatFormatting.GRAY);
            } else {
                apply(caster, SpellUpkeep.UNTIMED_TICKS);
                actionBar(caster, "hahueuh.message.vita_self_started", ChatFormatting.AQUA);
            }
            return;
        }

        LivingEntity target = resolveTarget(caster);
        if (target == null) {
            actionBar(caster, "hahueuh.message.vita_no_target", ChatFormatting.RED);
            return;
        }

        apply(target, ConfigMagicYin.VITA_TARGET_DURATION_SECONDS.get() * 20);
        actionBar(caster, "hahueuh.message.vita_target_started", ChatFormatting.AQUA);
    }

    private LivingEntity resolveTarget(ServerPlayer caster) {
        double range = ConfigMagicYin.VITA_RANGE.get();
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(caster,
                e -> e != caster && e.isAlive() && !e.isSpectator() && e instanceof LivingEntity, range);
        return hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le ? le : null;
    }

    private void apply(LivingEntity target, int durationTicks) {
        target.addEffect(new MobEffectInstance(ModEffects.INCREASED_GRAVITY.get(),
                durationTicks, 0, false, true, true));
        HahUeuh.INCREASED_GRAVITY.refreshModifier(target);

        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    20, 0.35, target.getBbHeight() * 0.3, 0.35, 0.01);
            level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 0.6f);
        }
    }

    public void clear(LivingEntity target) {
        target.removeEffect(ModEffects.INCREASED_GRAVITY.get());
        HahUeuh.INCREASED_GRAVITY.removeModifier(target);

        if (target.level() instanceof ServerLevel level) {
            level.playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4f, 1.4f);
        }
    }

    private static void actionBar(ServerPlayer player, String key, ChatFormatting style) {
        player.displayClientMessage(Component.translatable(key).withStyle(style), true);
    }
}
