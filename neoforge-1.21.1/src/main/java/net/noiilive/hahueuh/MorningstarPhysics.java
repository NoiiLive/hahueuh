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
            }
        }

        BlockHitResult support = level.clip(new ClipContext(next,
                next.subtract(0.0, GROUND_PROBE, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        if (support.getType() == HitResult.Type.BLOCK && support.getDirection() == Direction.UP) {
            double rest = support.getLocation().y + HEAD_RADIUS * 0.5;
            if (next.y < rest) {
                next = new Vec3(next.x, rest, next.z);
                s.onGround = true;
                if (s.vel.y < -0.3) s.groundImpact = -s.vel.y;
                boolean whipping = s.spinning && s.swingTicks > 0;
                double friction = whipping ? 1.0 : GROUND_FRICTION;
                s.vel = new Vec3(s.vel.x * friction, Math.max(s.vel.y, 0.0), s.vel.z * friction);
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
