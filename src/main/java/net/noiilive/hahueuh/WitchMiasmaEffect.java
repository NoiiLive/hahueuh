package net.noiilive.hahueuh;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class WitchMiasmaEffect extends MobEffect {
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final double BASE_RADIUS = 24.0;
    private static final double RADIUS_PER_LEVEL = 8.0;

    public WitchMiasmaEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayer player && !player.isCreative() && !player.isSpectator()
                && !HahUeuh.SNAPSHOT_MANAGER.isTargetingSuppressed()) {
            double radius = BASE_RADIUS + amplifier * RADIUS_PER_LEVEL;
            AABB box = player.getBoundingBox().inflate(radius);
            List<Mob> mobs = player.level().getEntitiesOfClass(Mob.class, box,
                    m -> m instanceof Enemy && !(m instanceof NeutralMob) && m.isAlive()
                            && (m.getTarget() == null || !m.getTarget().isAlive()));
            for (Mob mob : mobs) {
                if (mob.getSensing().hasLineOfSight(player)) {
                    mob.setTarget(player);
                }
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % SCAN_INTERVAL_TICKS == 0;
    }
}
