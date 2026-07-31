package net.noiilive.hahueuh;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class GuiltywhipPhysics {
    public static final double WHIP_LENGTH = 3.0;
    public static final int NODES = 16;
    public static final double SPACING = WHIP_LENGTH / (NODES - 1);
    private static final double GRAVITY = 0.04;
    private static final double DAMPING = 0.96;
    private static final int ITERATIONS = 8;
    private static final int SETTLE_ITERATIONS = 4;
    private static final double NODE_RADIUS = 0.08;
    private static final double GROUND_PROBE = NODE_RADIUS + 0.1;
    private static final double GROUND_FRICTION = 0.8;
    private static final double BASE_STIFFNESS = 0.55;
    private static final double SECOND_STIFFNESS = 0.2;

    private GuiltywhipPhysics() {}

    public static final class State {
        public final Vec3[] pos = new Vec3[NODES];
        public final Vec3[] prev = new Vec3[NODES];

        public State(Vec3 anchor) {
            for (int i = 0; i < NODES; i++) {
                Vec3 p = anchor.subtract(0.0, SPACING * i, 0.0);
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

    public static void step(State s, Vec3 anchor, Player owner) {
        Level level = owner.level();

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
        for (int iter = 0; iter < iterations; iter++) {
            s.pos[0] = anchor;
            for (int i = 0; i < NODES - 1; i++) {
                Vec3 a = s.pos[i];
                Vec3 b = s.pos[i + 1];
                Vec3 delta = b.subtract(a);
                double dist = delta.length();
                if (dist < 1.0e-6) continue;
                double error = (dist - SPACING) / dist;
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
        double yaw = Math.toRadians(owner.yBodyRot);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 handle = new Vec3(forward.x * 0.45, -0.9, forward.z * 0.45).normalize();
        Vec3 first = anchor.add(handle.scale(SPACING));
        s.pos[1] = s.pos[1].add(first.subtract(s.pos[1]).scale(BASE_STIFFNESS));
        Vec3 second = first.add(handle.scale(SPACING));
        s.pos[2] = s.pos[2].add(second.subtract(s.pos[2]).scale(SECOND_STIFFNESS));
    }

    private static void clampReach(State s, Vec3 anchor) {
        for (int i = 1; i < NODES; i++) {
            Vec3 off = s.pos[i].subtract(anchor);
            double max = SPACING * i;
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
