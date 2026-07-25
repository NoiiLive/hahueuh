package net.noiilive.hahueuh;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ElMinyaChain {
    private final Map<UUID, Deque<Long>> hitsByTarget = new HashMap<>();

    public void recordHit(ServerLevel level, LivingEntity target, LivingEntity caster) {
        long now = level.getGameTime();
        long window = Math.max(1L, (long) (ConfigMagicYin.EL_MINYA_CHAIN_WINDOW_SECONDS.get() * 20.0));
        int threshold = ConfigMagicYin.EL_MINYA_CHAIN_THRESHOLD.getAsInt();

        Deque<Long> hits = hitsByTarget.computeIfAbsent(target.getUUID(), k -> new ArrayDeque<>());
        while (!hits.isEmpty() && now - hits.peekFirst() > window) {
            hits.pollFirst();
        }
        hits.addLast(now);

        if (hits.size() >= threshold) {
            hitsByTarget.remove(target.getUUID());
            detonate(level, target, caster);
        }
    }

    private void detonate(ServerLevel level, LivingEntity center, LivingEntity caster) {
        double radius = ConfigMagicYin.EL_MINYA_AOE_RADIUS.get();
        float damage = (float) ConfigMagicYin.EL_MINYA_AOE_DAMAGE.get().doubleValue();
        Vec3 c = center.position();
        DamageSource source = ModDamageTypes.minya(level, null, caster);

        if (radius > 0.0 && damage > 0.0f) {
            double r2 = radius * radius;
            AABB box = center.getBoundingBox().inflate(radius);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                    x -> x.isAlive() && !x.isSpectator() && x != caster && x.distanceToSqr(c) <= r2)) {
                e.invulnerableTime = 0;
                e.hurt(source, damage);
            }
        }

        double cy = center.getY() + center.getBbHeight() * 0.5;
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, c.x, cy, c.z, 60, radius * 0.4, radius * 0.4, radius * 0.4, 0.3);
        level.sendParticles(ParticleTypes.CRIT, c.x, cy, c.z, 50, radius * 0.4, radius * 0.4, radius * 0.4, 0.4);
        level.sendParticles(ParticleTypes.WITCH, c.x, cy, c.z, 40, radius * 0.5, radius * 0.5, radius * 0.5, 0.0);
        level.playSound(null, center.blockPosition(), ModSounds.MINYA_EXPLODE.get(), SoundSource.PLAYERS, 1.6f, 0.7f);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        hitsByTarget.clear();
    }
}
