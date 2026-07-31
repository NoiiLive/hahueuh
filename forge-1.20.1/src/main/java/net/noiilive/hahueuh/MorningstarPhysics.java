package net.noiilive.hahueuh;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public final class MorningstarPhysics {
    public static final double CHAIN_LENGTH = 5.0;
    public static final double GRAVITY = 0.08;
    public static final double DRAG = 0.94;
    public static final double VERTICAL_DRAG = 0.98;
    public static final double HEAD_RADIUS = 0.45;
    public static final int SWING_TICKS = 14;
    public static final int SPIN_TICKS = 80;
    public static final int RESWING_WINDOW = 4;
    public static final double THROW_SPEED = 2.2;
    public static final double SPIN_START_SPEED = 0.35;
    public static final double SPIN_ACCEL = 0.18;
    public static final double SPIN_MIN_SPEED = 0.6;
    public static final double SPIN_MAX_SPEED = 2.8;
    public static final float SPIN_DAMAGE = 3.0f;
    public static final float SPIN_HEAD_MIN_DAMAGE = 10.0f;
    public static final float SPIN_HEAD_MAX_DAMAGE = 35.0f;
    public static final int SPIN_MOMENTUM_TICKS = 180;
    public static final int MAX_QUEUED_SPINS = 1;
    private static final double GROUND_FRICTION = 0.7;
    private static final double GROUND_PROBE = HEAD_RADIUS * 0.5 + 0.1;
    private static final double BLOCK_REEL_SPEED = 0.45;
    private static final double BLOCK_STOP_IMPACT = 0.35;

    private static final double SPIN_MIN_EXTENSION = 0.97;
    private static final double SPIN_TAUT_TICKS = 6.0;
    private static final double ENTITY_RESTITUTION = 0.06;
    private static final double ENTITY_FRICTION = 0.7;
    private static final double ENTITY_PAD = HEAD_RADIUS;
    private static final double TAUT_START = 0.82;
    private static final double TAUT_BLEND = 0.85;

    public static final int CHAIN_NODES = 16;
    private static final double CHAIN_SPACING = CHAIN_LENGTH / (CHAIN_NODES - 1);
    private static final double CHAIN_GRAVITY = 0.05;
    private static final double CHAIN_DAMPING = 0.97;
    private static final int CHAIN_ITERATIONS = 10;
    private static final int CHAIN_SETTLE = 3;
    private static final double CHAIN_NODE_RADIUS = 0.07;
    private static final double CHAIN_GROUND_PROBE = CHAIN_NODE_RADIUS + 0.1;
    private static final double CHAIN_GROUND_FRICTION = 0.7;

    private MorningstarPhysics() {}

    public static final class State {
        public Vec3 pos;
        public Vec3 prevPos;
        public Vec3 vel = Vec3.ZERO;
        public int swingTicks;
        public boolean spinning;
        public double spinAngle;
        public Vec3 pendingThrow;
        public int reelTicks;
        public double groundImpact;
        public boolean onGround;
        public int cooldown;
        public int queuedSpins;
        public int spinElapsed;
        public final Vec3[] chain = new Vec3[CHAIN_NODES];
        public final Vec3[] chainPrev = new Vec3[CHAIN_NODES];
        public boolean chainReady;
        public final Set<Integer> hitThisSwing = new HashSet<>();

        public State(Vec3 start) {
            this.pos = start;
            this.prevPos = start;
        }
    }

    public static Vec3 handAnchor(Player player) {
        return handAnchor(player, 1.0f);
    }

    public static Vec3 handAnchor(Player player, float partialTick) {
        Vec3 hold = player.getRopeHoldPosition(partialTick);
        double yaw = Math.toRadians(Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot));
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        double armSide = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT ? -0.14 : 0.14;
        return hold
                .add(right.scale(armSide))
                .add(fwd.scale(0.35))
                .add(0.0, 0.08, 0.0);
    }

    public static void step(State s, Vec3 anchor, Player owner) {
        Level level = owner.level();
        s.groundImpact = 0.0;
        s.onGround = false;
        if (s.cooldown > 0) s.cooldown--;
        if (s.pendingThrow != null) {
            s.prevPos = s.pos;
            Vec3 toAnchor = anchor.subtract(s.pos);
            s.reelTicks--;
            if (toAnchor.length() < 0.8 || s.reelTicks <= 0) {
                s.pos = anchor.add(s.pendingThrow.scale(0.4));
                s.vel = s.pendingThrow.scale(THROW_SPEED);
                s.swingTicks = SWING_TICKS;
                s.pendingThrow = null;
            } else {
                s.pos = s.pos.add(toAnchor.scale(0.5));
                s.vel = Vec3.ZERO;
            }
            stepChain(s, anchor, owner, level);
            return;
        }

        s.prevPos = s.pos;
        s.vel = s.vel.subtract(0.0, GRAVITY, 0.0);
        s.vel = new Vec3(s.vel.x * DRAG, s.vel.y * VERTICAL_DRAG, s.vel.z * DRAG);

        if (s.swingTicks > 0 && !s.spinning && s.vel.length() > 1.2) {
            s.vel = s.vel.add(0.0, GRAVITY * 0.75, 0.0);
        }

        if (s.swingTicks > 0 && s.spinning) {
            s.spinElapsed++;
            Vec3 radial = new Vec3(s.pos.x - anchor.x, 0.0, s.pos.z - anchor.z);
            if (radial.lengthSqr() > 1.0e-4) {
                Vec3 tangent = new Vec3(-radial.z, 0.0, radial.x).normalize();
                s.vel = s.vel
                        .add(tangent.scale(SPIN_ACCEL))
                        .add(radial.normalize().scale(0.05))
                        .add(0.0, GRAVITY, 0.0);
                double cap = SPIN_MIN_SPEED + (SPIN_MAX_SPEED - SPIN_MIN_SPEED) * momentum(s);
                double speed = s.vel.length();
                if (speed > cap) s.vel = s.vel.scale(cap / speed);
            }
        }

        Vec3 next = s.pos.add(s.vel);

        if (s.vel.lengthSqr() > 1.0e-8) {
            BlockHitResult hit = level.clip(new ClipContext(s.pos, next,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Direction face = hit.getDirection();
                Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
                next = hit.getLocation().add(normal.scale(HEAD_RADIUS * 0.5));
                double into = s.vel.dot(normal);
                if (into < 0.0) {
                    if (face == Direction.UP) {
                        s.onGround = true;
                        if (s.vel.y < -0.3) s.groundImpact = -s.vel.y;
                    }
                    Vec3 slide = s.vel.subtract(normal.scale(into));
                    boolean whipping = s.spinning && s.swingTicks > 0;
                    s.vel = whipping ? slide : slide.scale(GROUND_FRICTION);
                }
                cancelSwingOnBlock(s, anchor, next, -into);
            }
        }

        if (!s.spinning && (s.swingTicks > 0 || s.vel.lengthSqr() > 0.36)) {
            next = bounceOffEntities(s, owner, level, next);
        }

        BlockHitResult support = level.clip(new ClipContext(next,
                next.subtract(0.0, GROUND_PROBE, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        if (support.getType() == HitResult.Type.BLOCK && support.getDirection() == Direction.UP) {
            double rest = support.getLocation().y + HEAD_RADIUS * 0.5;
            if (next.y < rest) {
                next = new Vec3(next.x, rest, next.z);
                s.onGround = true;
                double drop = -s.vel.y;
                if (s.vel.y < -0.3) s.groundImpact = drop;
                boolean whipping = s.spinning && s.swingTicks > 0;
                double friction = whipping ? 1.0 : GROUND_FRICTION;
                s.vel = new Vec3(s.vel.x * friction, Math.max(s.vel.y, 0.0), s.vel.z * friction);
                cancelSwingOnBlock(s, anchor, next, drop);
            }
        }

        if (s.swingTicks > 0 && s.spinning) {
            Vec3 radialOff = next.subtract(anchor);
            double radialDist = radialOff.length();
            double spinUp = Math.min(1.0, s.spinElapsed / SPIN_TAUT_TICKS);
            double minRadius = CHAIN_LENGTH * (SPIN_MIN_EXTENSION
                    + (1.0 - SPIN_MIN_EXTENSION) * momentum(s)) * spinUp;
            if (radialDist > 1.0e-4 && radialDist < minRadius) {
                next = anchor.add(radialOff.scale(minRadius / radialDist));
            }
        }

        Vec3 off = next.subtract(anchor);
        double dist = off.length();
        if (dist > CHAIN_LENGTH) {
            Vec3 n = off.scale(1.0 / dist);
            next = anchor.add(n.scale(CHAIN_LENGTH));
            double radialVel = s.vel.dot(n);
            if (radialVel > 0.0) s.vel = s.vel.subtract(n.scale(radialVel));
        }

        if (s.swingTicks > 0 && s.spinning) {
            double a0 = Math.toDegrees(Math.atan2(s.prevPos.z - anchor.z, s.prevPos.x - anchor.x));
            double a1 = Math.toDegrees(Math.atan2(next.z - anchor.z, next.x - anchor.x));
            s.spinAngle += Math.abs(Mth.wrapDegrees(a1 - a0));
            if (s.spinAngle >= 360.0) {
                if (s.queuedSpins > 0) {
                    s.queuedSpins--;
                    s.spinAngle -= 360.0;
                    s.swingTicks = SPIN_TICKS;
                    s.hitThisSwing.clear();
                } else {
                    s.swingTicks = 0;
                    s.spinning = false;
                }
            }
        }

        s.pos = next;
        if (s.swingTicks > 0) s.swingTicks--;
        stepChain(s, anchor, owner, level);
    }

    private static void cancelSwingOnBlock(State s, Vec3 anchor, Vec3 at, double impact) {
        if (s.swingTicks <= 0 || impact < BLOCK_STOP_IMPACT) return;
        s.swingTicks = 0;
        s.spinning = false;
        s.spinAngle = 0.0;
        s.queuedSpins = 0;
        Vec3 back = anchor.subtract(at);
        s.vel = back.lengthSqr() < 1.0e-8
                ? Vec3.ZERO
                : back.normalize().scale(BLOCK_REEL_SPEED);
    }

    private static Vec3 bounceOffEntities(State s, Player owner, Level level, Vec3 next) {
        net.minecraft.world.phys.AABB search =
                new net.minecraft.world.phys.AABB(s.pos, next).inflate(HEAD_RADIUS + 0.5);
        net.minecraft.world.phys.AABB struck = null;
        Vec3 contact = null;
        double closest = Double.MAX_VALUE;

        for (net.minecraft.world.entity.Entity candidate : level.getEntities(owner, search,
                e -> e != owner && e.isAlive() && !e.isSpectator()
                        && e instanceof net.minecraft.world.entity.LivingEntity)) {
            net.minecraft.world.phys.AABB box =
                    candidate.getBoundingBox().inflate(ENTITY_PAD);
            Vec3 point;
            java.util.Optional<Vec3> entry = box.clip(s.pos, next);
            if (entry.isPresent()) {
                point = entry.get();
            } else if (box.contains(next)) {
                point = next;
            } else {
                continue;
            }
            double dist = s.pos.distanceToSqr(point);
            if (dist < closest) {
                closest = dist;
                struck = box;
                contact = point;
            }
        }
        if (struck == null) return next;

        Vec3 offset = contact.subtract(struck.getCenter());
        double halfX = (struck.maxX - struck.minX) * 0.5;
        double halfY = (struck.maxY - struck.minY) * 0.5;
        double halfZ = (struck.maxZ - struck.minZ) * 0.5;
        double ax = Math.abs(offset.x) / Math.max(halfX, 1.0e-4);
        double ay = Math.abs(offset.y) / Math.max(halfY, 1.0e-4);
        double az = Math.abs(offset.z) / Math.max(halfZ, 1.0e-4);
        Vec3 normal;
        if (ax >= ay && ax >= az) {
            normal = new Vec3(offset.x < 0.0 ? -1.0 : 1.0, 0.0, 0.0);
        } else if (az >= ax && az >= ay) {
            normal = new Vec3(0.0, 0.0, offset.z < 0.0 ? -1.0 : 1.0);
        } else {
            normal = new Vec3(0.0, offset.y < 0.0 ? -1.0 : 1.0, 0.0);
        }

        double into = s.vel.dot(normal);
        if (into < 0.0) {
            Vec3 tangential = s.vel.subtract(normal.scale(into));
            s.vel = tangential.scale(ENTITY_FRICTION)
                    .add(normal.scale(-into * ENTITY_RESTITUTION));
        }
        return contact.add(normal.scale(HEAD_RADIUS * 0.5));
    }

    private static void stepChain(State s, Vec3 anchor, Player owner, Level level) {
        if (!level.isClientSide) return;
        int last = CHAIN_NODES - 1;

        if (!s.chainReady) {
            for (int i = 0; i <= last; i++) {
                double t = i / (double) last;
                Vec3 p = anchor.add(s.pos.subtract(anchor).scale(t));
                s.chain[i] = p;
                s.chainPrev[i] = p;
            }
            s.chainReady = true;
        }

        for (int i = 1; i < last; i++) {
            Vec3 current = s.chain[i];
            Vec3 velocity = current.subtract(s.chainPrev[i]).scale(CHAIN_DAMPING);
            s.chainPrev[i] = current;
            s.chain[i] = current.add(velocity).subtract(0.0, CHAIN_GRAVITY, 0.0);
        }
        s.chainPrev[0] = s.chain[0];
        s.chain[0] = anchor;
        s.chainPrev[last] = s.chain[last];
        s.chain[last] = s.pos;

        relaxChain(s, anchor, CHAIN_ITERATIONS);
        for (int i = 1; i < last; i++) {
            s.chain[i] = resolveChain(level, owner, s.chainPrev[i], s.chain[i]);
        }
        relaxChain(s, anchor, CHAIN_SETTLE);
        straightenUnderTension(s, anchor);
    }

    private static void straightenUnderTension(State s, Vec3 anchor) {
        int last = CHAIN_NODES - 1;
        double extension = s.pos.distanceTo(anchor) / CHAIN_LENGTH;
        double tension = Mth.clamp((extension - TAUT_START) / (1.0 - TAUT_START), 0.0, 1.0);
        if (tension <= 0.0) return;

        double blend = tension * TAUT_BLEND;
        Vec3 span = s.pos.subtract(anchor);
        for (int i = 1; i < last; i++) {
            Vec3 straight = anchor.add(span.scale(i / (double) last));
            s.chain[i] = s.chain[i].add(straight.subtract(s.chain[i]).scale(blend));
        }
    }

    private static void relaxChain(State s, Vec3 anchor, int iterations) {
        int last = CHAIN_NODES - 1;
        for (int iter = 0; iter < iterations; iter++) {
            s.chain[0] = anchor;
            s.chain[last] = s.pos;
            for (int i = 0; i < last; i++) {
                Vec3 a = s.chain[i];
                Vec3 b = s.chain[i + 1];
                Vec3 delta = b.subtract(a);
                double dist = delta.length();
                if (dist < 1.0e-6) continue;
                double error = (dist - CHAIN_SPACING) / dist;
                boolean pinnedA = i == 0;
                boolean pinnedB = i + 1 == last;
                if (pinnedA && pinnedB) continue;
                if (pinnedA) {
                    s.chain[i + 1] = b.subtract(delta.scale(error));
                } else if (pinnedB) {
                    s.chain[i] = a.add(delta.scale(error));
                } else {
                    Vec3 shift = delta.scale(error * 0.5);
                    s.chain[i] = a.add(shift);
                    s.chain[i + 1] = b.subtract(shift);
                }
            }
        }
    }

    private static Vec3 resolveChain(Level level, Player owner, Vec3 from, Vec3 to) {
        if (from.distanceToSqr(to) > 1.0e-8) {
            BlockHitResult hit = level.clip(new ClipContext(from, to,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Direction face = hit.getDirection();
                Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
                to = hit.getLocation().add(normal.scale(CHAIN_NODE_RADIUS));
            }
        }

        BlockHitResult support = level.clip(new ClipContext(to,
                to.subtract(0.0, CHAIN_GROUND_PROBE, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        if (support.getType() == HitResult.Type.BLOCK && support.getDirection() == Direction.UP) {
            double rest = support.getLocation().y + CHAIN_NODE_RADIUS;
            if (to.y < rest) {
                Vec3 drift = to.subtract(from);
                to = new Vec3(from.x + drift.x * CHAIN_GROUND_FRICTION, rest,
                        from.z + drift.z * CHAIN_GROUND_FRICTION);
            }
        }
        return to;
    }

    public static boolean canSwing(State s) {
        return s.cooldown <= 0 && s.pendingThrow == null && s.swingTicks <= RESWING_WINDOW;
    }

    public static boolean tryQueueSpin(State s) {
        if (!s.spinning || s.swingTicks <= 0) return false;
        if (s.queuedSpins >= MAX_QUEUED_SPINS) return false;
        s.queuedSpins++;
        return true;
    }

    public static double momentum(State s) {
        return Mth.clamp(s.spinElapsed / (double) SPIN_MOMENTUM_TICKS, 0.0, 1.0);
    }

    public static float spinMomentumDamage(State s) {
        return (float) (SPIN_HEAD_MIN_DAMAGE
                + (SPIN_HEAD_MAX_DAMAGE - SPIN_HEAD_MIN_DAMAGE) * momentum(s));
    }

    public static void applySwing(State s, Player player, boolean spin) {
        s.hitThisSwing.clear();
        s.spinning = spin;
        s.spinAngle = 0.0;
        s.pendingThrow = null;
        s.queuedSpins = 0;
        s.spinElapsed = 0;
        s.cooldown = Math.max(1, (int) Math.ceil(player.getCurrentItemAttackStrengthDelay()));
        Vec3 look = player.getLookAngle().normalize();
        if (spin) {
            s.swingTicks = SPIN_TICKS;
            Vec3 flat = new Vec3(look.x, 0.0, look.z);
            Vec3 tangent = flat.lengthSqr() > 1.0e-6
                    ? new Vec3(-flat.z, 0.0, flat.x).normalize()
                    : new Vec3(1.0, 0.0, 0.0);
            s.vel = tangent.scale(SPIN_START_SPEED).add(0.0, 0.2, 0.0);
        } else {
            s.pendingThrow = look;
            s.reelTicks = 6;
        }
    }
}
