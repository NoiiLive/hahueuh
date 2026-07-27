package net.noiilive.hahueuh;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Crystallize {
    private static final double DRIFT_EPSILON_SQR = 0.0025;

    private record Root(double anchorX, double anchorZ, UUID casterId, int ticksRemaining, boolean prevNoGravity) {}

    private final Map<UUID, Root> rooted = new ConcurrentHashMap<>();
    private MinecraftServer server;

    public void crystallize(LivingEntity target, LivingEntity caster, int durationTicks) {
        if (target == null || !target.isAlive() || durationTicks <= 0) return;

        boolean prevNoGravity = target.isNoGravity();
        target.setNoGravity(false);
        Vec3 vel = target.getDeltaMovement();
        target.setDeltaMovement(0.0, Math.min(vel.y, 0.0), 0.0);
        target.addEffect(new MobEffectInstance(ModEffects.CRYSTALLIZED.get(), durationTicks, 0, false, true, true));
        rooted.put(target.getUUID(), new Root(target.getX(), target.getZ(),
                caster != null ? caster.getUUID() : null, durationTicks, prevNoGravity));

        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.INSTANT_EFFECT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    30, 0.3, target.getBbHeight() * 0.4, 0.3, 0.0);
            level.playSound(null, target.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.2f, 0.6f);
        }
    }

    public boolean isCrystallized(UUID uuid) {
        return rooted.containsKey(uuid);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (server == null || rooted.isEmpty()) return;
        for (Map.Entry<UUID, Root> entry : new ArrayList<>(rooted.entrySet())) {
            UUID uuid = entry.getKey();
            Root root = entry.getValue();
            Entity entity = findEntity(uuid);
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                rooted.remove(uuid);
                continue;
            }

            hold(living, root.anchorX(), root.anchorZ());

            int remaining = root.ticksRemaining() - 1;
            if (remaining <= 0) {
                shatter(living, root.casterId());
                rooted.remove(uuid);
                release(living, root);
            } else {
                rooted.put(uuid, new Root(root.anchorX(), root.anchorZ(), root.casterId(), remaining, root.prevNoGravity()));
            }
        }
    }

    private void hold(LivingEntity entity, double anchorX, double anchorZ) {
        entity.setNoGravity(false);
        Vec3 vel = entity.getDeltaMovement();
        entity.setDeltaMovement(0.0, Math.min(vel.y, 0.0), 0.0);
        entity.hasImpulse = false;

        double dx = entity.getX() - anchorX;
        double dz = entity.getZ() - anchorZ;
        if (dx * dx + dz * dz > DRIFT_EPSILON_SQR) {
            if (entity instanceof ServerPlayer sp) {
                sp.connection.teleport(anchorX, entity.getY(), anchorZ, sp.getYRot(), sp.getXRot());
            } else {
                entity.setPos(anchorX, entity.getY(), anchorZ);
            }
        }
    }

    private void shatter(LivingEntity target, UUID casterId) {
        double percent = ConfigMagicYin.MINYA_SHATTER_PERCENT.get() / 100.0;
        float burst = (float) (target.getHealth() * percent);

        Entity caster = casterId != null ? findEntity(casterId) : null;
        DamageSource source = ModDamageTypes.minya(target.level(), null, caster);
        if (burst > 0.0f) {
            target.invulnerableTime = 0;
            target.hurt(source, burst);
        }

        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    40, 0.4, target.getBbHeight() * 0.5, 0.4, 0.2);
            level.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    25, 0.4, target.getBbHeight() * 0.5, 0.4, 0.2);
            level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.4f, 0.8f);
        }
    }

    private void release(LivingEntity entity, Root root) {
        entity.setNoGravity(root.prevNoGravity());
        entity.removeEffect(ModEffects.CRYSTALLIZED.get());
    }

    public void forceRelease(UUID uuid) {
        Root root = rooted.remove(uuid);
        if (root == null) return;
        Entity entity = findEntity(uuid);
        if (entity instanceof LivingEntity living) release(living, root);
    }

    private Entity findEntity(UUID id) {
        if (server == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        rooted.clear();
        this.server = null;
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        forceRelease(event.getEntity().getUUID());
    }
}
