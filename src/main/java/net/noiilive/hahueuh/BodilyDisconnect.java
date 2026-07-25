package net.noiilive.hahueuh;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class BodilyDisconnect {
    private static final double WANDER_SPEED = 0.13;
    private static final float JUMP_CHANCE = 0.15f;
    private static final int MOVE_REROLL_TICKS = 20;
    private static final int LOOK_REROLL_TICKS = 30;
    private static final float LOOK_ARC = 90f;
    private static final float YAW_APPROACH = 8f;
    private static final float PITCH_APPROACH = 5f;
    private static final double ATTACK_REACH = 2.5;
    private static final float FALLBACK_ATTACK_DAMAGE = 2.0f;
    private static final int STALE_TICKS = 40;

    private final Map<Integer, State> states = new HashMap<>();

    public void driveMob(Mob mob) {
        MinecraftServer server = mob.getServer();
        int now = server != null ? server.getTickCount() : 0;
        State s = states.computeIfAbsent(mob.getId(), k -> new State(mob.getRandom()));
        s.lastTick = now;
        RandomSource rnd = mob.getRandom();

        MobTargetUtil.clearTarget(mob);
        mob.getNavigation().stop();

        if (s.tick % MOVE_REROLL_TICKS == 0) {
            double angle = rnd.nextDouble() * Math.PI * 2.0;
            s.moveDx = Math.cos(angle) * WANDER_SPEED;
            s.moveDz = Math.sin(angle) * WANDER_SPEED;
            s.wantJump = mob.onGround() && rnd.nextFloat() < JUMP_CHANCE;
        }
        Vec3 vel = mob.getDeltaMovement();
        double vy = vel.y;
        if (s.wantJump && mob.onGround()) {
            vy = 0.42;
            s.wantJump = false;
        }
        mob.setDeltaMovement(s.moveDx, vy, s.moveDz);
        mob.hasImpulse = true;

        if (s.tick % LOOK_REROLL_TICKS == 0) {
            s.targetYaw = mob.getYRot() + (rnd.nextFloat() * LOOK_ARC - LOOK_ARC / 2f);
            s.targetPitch = rnd.nextFloat() * 60f - 30f;
        }
        float yaw = approachDegrees(mob.getYRot(), s.targetYaw, YAW_APPROACH);
        float pitch = approachDegrees(mob.getXRot(), s.targetPitch, PITCH_APPROACH);
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setXRot(pitch);

        if (--s.attackTimer <= 0) {
            s.attackTimer = 10 + rnd.nextInt(31);
            performAttack(mob);
        }
        s.tick++;
    }

    private static void performAttack(Mob mob) {
        mob.swing(InteractionHand.MAIN_HAND);
        LivingEntity target = nearestVictim(mob);
        if (target == null) return;
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            mob.doHurtTarget(target);
        } else {
            target.hurt(mob.damageSources().mobAttack(mob), FALLBACK_ATTACK_DAMAGE);
        }
    }

    private static LivingEntity nearestVictim(Mob mob) {
        AABB box = mob.getBoundingBox().inflate(ATTACK_REACH);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : mob.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != mob && e.isAlive())) {
            double d = mob.distanceToSqr(e);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    private static float approachDegrees(float current, float target, float maxDelta) {
        float delta = Mth.wrapDegrees(target - current);
        delta = Mth.clamp(delta, -maxDelta, maxDelta);
        return current + delta;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (states.isEmpty()) return;
        int now = event.getServer().getTickCount();
        Iterator<Map.Entry<Integer, State>> it = states.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().lastTick > STALE_TICKS) it.remove();
        }
    }

    private static final class State {
        double moveDx, moveDz;
        boolean wantJump;
        float targetYaw, targetPitch;
        int attackTimer;
        int tick;
        int lastTick;

        State(RandomSource rnd) {
            this.attackTimer = 10 + rnd.nextInt(31);
        }
    }
}
