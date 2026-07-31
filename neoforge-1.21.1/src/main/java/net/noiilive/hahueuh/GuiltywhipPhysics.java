package net.noiilive.hahueuh;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class GuiltywhipPhysics {
    public static final double WHIP_LENGTH = 3.0;
    public static final double CRACK_LENGTH = 4.0;
    public static final int NODES = 16;
    private static final double IDLE_SPACING = WHIP_LENGTH / (NODES - 1);
    private static final double GRAVITY = 0.04;
    private static final double DAMPING = 0.96;
    private static final int ITERATIONS = 8;
    private static final int SETTLE_ITERATIONS = 4;
    private static final double NODE_RADIUS = 0.08;
    private static final double GROUND_PROBE = NODE_RADIUS + 0.1;
    private static final double GROUND_FRICTION = 0.8;
    private static final double BASE_STIFFNESS = 0.55;
    private static final double SECOND_STIFFNESS = 0.2;

    public static final int CRACK_TICKS = 6;
    public static final int SWEEP_TICKS = 9;
    public static final int CRACK_COOLDOWN = 10;
    public static final double BASE_DAMAGE_FRACTION = 0.3;
    private static final double CRACK_UNROLL_TICKS = 4.0;
    private static final double SWEEP_UNROLL_TICKS = 2.0;
    private static final double CRACK_LOOP_RADIUS = 0.55;
    private static final double SWEEP_START_DEG = 70.0;
    private static final double SWEEP_TRAIL_DEG = 85.0;
    private static final double SWEEP_TRAIL_DECAY = 0.78;
    private static final double FOLLOW_BASE = 0.92;
    private static final double FOLLOW_TIP_LOSS = 0.32;
    private static final double SETTLE_TICKS = 2.0;

    private GuiltywhipPhysics() {}

    public static final class State {
        public final Vec3[] pos = new Vec3[NODES];
        public final Vec3[] prev = new Vec3[NODES];
        public Vec3 crackDir = Vec3.ZERO;
        public Vec3 crackUp = Vec3.ZERO;
        public boolean sweeping;
        public boolean sweepFlip;
        public int crackTicks;
        public int cooldown;
        public final java.util.Set<Integer> hitThisCrack = new java.util.HashSet<>();

        public State(Vec3 anchor) {
            for (int i = 0; i < NODES; i++) {
                Vec3 p = anchor.subtract(0.0, IDLE_SPACING * i, 0.0);
                pos[i] = p;
                prev[i] = p;
            }
        }
    }

    public static Vec3 handAnchor(Player player) {
        return MorningstarPhysics.handAnchor(player);
    }

    public static Vec3 handAnchor(Player player, float partialTick) {
        return MorningstarPhysics.handAnchor(player, partialTick);
    }

    public static double lengthOf(State s) {
        return s.crackTicks > 0 ? CRACK_LENGTH : WHIP_LENGTH;
    }

    private static double spacingOf(State s) {
        return lengthOf(s) / (NODES - 1);
    }

    public static boolean canCrack(State s) {
        return s.cooldown <= 0 && s.crackTicks <= 0;
    }

    public static void crack(State s, Player player, boolean sweeping, Vec3 anchor) {
        Vec3 look = player.getLookAngle().normalize();
        s.crackDir = sweeping ? look : aimFromHand(player, anchor, look);
        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 up = worldUp.subtract(s.crackDir.scale(s.crackDir.dot(worldUp)));
        if (up.lengthSqr() < 1.0e-4) {
            double yaw = Math.toRadians(player.yBodyRot);
            Vec3 fwd = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
            up = fwd.subtract(s.crackDir.scale(s.crackDir.dot(fwd)));
        }
        s.crackUp = up.normalize();
        s.sweeping = sweeping;
        if (sweeping) s.sweepFlip = !s.sweepFlip;
        s.crackTicks = sweeping ? SWEEP_TICKS : CRACK_TICKS;
        s.cooldown = Math.max(1, (int) Math.ceil(player.getCurrentItemAttackStrengthDelay()));
        s.hitThisCrack.clear();
    }

    private static Vec3 aimFromHand(Player player, Vec3 anchor, Vec3 look) {
        Vec3 eye = player.getEyePosition();
        Vec3 far = eye.add(look.scale(CRACK_LENGTH + 1.0));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, far,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 aim = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : far;
        Vec3 toAim = aim.subtract(anchor);
        return toAim.lengthSqr() < 1.0e-6 ? look : toAim.normalize();
    }

    public static double damageFraction(int node) {
        double t = Mth.clamp(node / (double) (NODES - 1), 0.0, 1.0);
        return BASE_DAMAGE_FRACTION + (1.0 - BASE_DAMAGE_FRACTION) * t;
    }

    private static void poseCrack(State s, Vec3 anchor, Player owner, Level level) {
        int total = s.sweeping ? SWEEP_TICKS : CRACK_TICKS;
        double elapsed = total - s.crackTicks;
        Vec3 dir = s.crackDir;
        double front;
        double unroll;
        double sweepEase = 0.0;
        if (s.sweeping) {
            unroll = SWEEP_UNROLL_TICKS;
            double progress = total <= 1 ? 1.0 : elapsed / (double) (total - 1);
            sweepEase = progress * progress * (3.0 - 2.0 * progress);
            double start = (s.sweepFlip ? -1.0 : 1.0) * SWEEP_START_DEG;
            double angle = Math.toRadians(start * (1.0 - 2.0 * sweepEase));
            Vec3 side = s.crackUp.cross(s.crackDir);
            dir = s.crackDir.scale(Math.cos(angle)).add(side.scale(Math.sin(angle)));
            front = Math.min(1.0, (elapsed + 1.0) / unroll);
        } else {
            unroll = CRACK_UNROLL_TICKS;
            front = Math.min(1.0, (elapsed + 1.0) / unroll);
        }
        double radius = CRACK_LOOP_RADIUS * (1.0 - 0.5 * front);
        double settle = Mth.clamp((elapsed + 1.0 - unroll) / SETTLE_TICKS, 0.0, 1.0);
        Vec3 sideAxis = s.crackUp.cross(s.crackDir);
        double trail = s.sweeping
                ? Math.toRadians(SWEEP_TRAIL_DEG) * (s.sweepFlip ? -1.0 : 1.0)
                        * (1.0 - SWEEP_TRAIL_DECAY * sweepEase)
                : 0.0;

        double len = lengthOf(s);
        double spacing = spacingOf(s);
        Vec3 walk = anchor;
        for (int i = 1; i < NODES; i++) {
            double t = i / (double) (NODES - 1);
            Vec3 target;
            if (s.sweeping) {
                double a = trail * t;
                double b = t <= front ? 0.0 : (t - front) * len / radius;
                Vec3 step = dir.scale(Math.cos(a) * Math.cos(b))
                        .add(sideAxis.scale(Math.sin(a) * Math.cos(b)))
                        .add(s.crackUp.scale(Math.sin(b)));
                walk = walk.add(step.scale(spacing));
                target = walk;
            } else if (t <= front) {
                target = anchor.add(dir.scale(t * len));
            } else {
                Vec3 frontPos = anchor.add(dir.scale(front * len));
                Vec3 centre = frontPos.add(s.crackUp.scale(radius));
                double theta = (t - front) * len / radius;
                target = centre
                        .subtract(s.crackUp.scale(radius * Math.cos(theta)))
                        .subtract(dir.scale(radius * Math.sin(theta)));
            }
            double follow = FOLLOW_BASE - FOLLOW_TIP_LOSS * t;
            if (!s.sweeping) follow += (1.0 - follow) * settle;
            Vec3 blended = s.pos[i].add(target.subtract(s.pos[i]).scale(follow));
            s.prev[i] = s.pos[i];
            s.pos[i] = resolve(level, owner, s.prev[i], blended);
        }
        s.prev[0] = s.pos[0];
        s.pos[0] = anchor;

        relax(s, anchor, SETTLE_ITERATIONS);
        clampReach(s, anchor);
    }

    public static void step(State s, Vec3 anchor, Player owner) {
        Level level = owner.level();
        if (s.cooldown > 0) s.cooldown--;
        if (s.crackTicks > 0) {
            poseCrack(s, anchor, owner, level);
            s.crackTicks--;
            return;
        }

        for (int i = 1; i < NODES; i++) {
            Vec3 current = s.pos[i];
            Vec3 velocity = current.subtract(s.prev[i]).scale(DAMPING);
            s.prev[i] = current;
            s.pos[i] = current.add(velocity).subtract(0.0, GRAVITY, 0.0);
        }
        s.prev[0] = s.pos[0];
        s.pos[0] = anchor;

        relax(s, anchor, ITERATIONS);
        stiffenBase(s, anchor, owner);

        for (int i = 1; i < NODES; i++) {
            s.pos[i] = resolve(level, owner, s.prev[i], s.pos[i]);
        }

        relax(s, anchor, SETTLE_ITERATIONS);
        clampReach(s, anchor);
    }

    private static void relax(State s, Vec3 anchor, int iterations) {
        double spacing = spacingOf(s);
        for (int iter = 0; iter < iterations; iter++) {
            s.pos[0] = anchor;
            for (int i = 0; i < NODES - 1; i++) {
                Vec3 a = s.pos[i];
                Vec3 b = s.pos[i + 1];
                Vec3 delta = b.subtract(a);
                double dist = delta.length();
                if (dist < 1.0e-6) continue;
                double error = (dist - spacing) / dist;
                if (i == 0) {
                    s.pos[i + 1] = b.subtract(delta.scale(error));
                } else {
                    Vec3 shift = delta.scale(error * 0.5);
                    s.pos[i] = a.add(shift);
                    s.pos[i + 1] = b.subtract(shift);
                }
            }
        }
    }

    private static void stiffenBase(State s, Vec3 anchor, Player owner) {
        double spacing = spacingOf(s);
        double yaw = Math.toRadians(owner.yBodyRot);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 handle = new Vec3(forward.x * 0.45, -0.9, forward.z * 0.45).normalize();
        Vec3 first = anchor.add(handle.scale(spacing));
        s.pos[1] = s.pos[1].add(first.subtract(s.pos[1]).scale(BASE_STIFFNESS));
        Vec3 second = first.add(handle.scale(spacing));
        s.pos[2] = s.pos[2].add(second.subtract(s.pos[2]).scale(SECOND_STIFFNESS));
    }

    private static void clampReach(State s, Vec3 anchor) {
        double spacing = spacingOf(s);
        for (int i = 1; i < NODES; i++) {
            Vec3 off = s.pos[i].subtract(anchor);
            double max = spacing * i;
            double dist = off.length();
            if (dist > max) s.pos[i] = anchor.add(off.scale(max / dist));
        }
    }

    private static Vec3 resolve(Level level, Entity owner, Vec3 from, Vec3 to) {
        if (from.distanceToSqr(to) > 1.0e-8) {
            BlockHitResult hit = level.clip(new ClipContext(from, to,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Direction face = hit.getDirection();
                Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
                to = hit.getLocation().add(normal.scale(NODE_RADIUS));
            }
        }

        BlockHitResult support = level.clip(new ClipContext(to,
                to.subtract(0.0, GROUND_PROBE, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        if (support.getType() == HitResult.Type.BLOCK && support.getDirection() == Direction.UP) {
            double rest = support.getLocation().y + NODE_RADIUS;
            if (to.y < rest) {
                Vec3 drift = to.subtract(from);
                to = new Vec3(from.x + drift.x * GROUND_FRICTION, rest,
                        from.z + drift.z * GROUND_FRICTION);
            }
        }
        return to;
    }
}
