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
    public static final double WHIP_LENGTH = 2.5;
    public static final double CRACK_LENGTH = 4.0;
    public static final double GRAPPLE_LENGTH = 6.0;
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

    public static final int GRAPPLE_EXTEND_TICKS = 4;
    public static final int GRAPPLE_HOLD_TICKS = 8;
    public static final int GRAPPLE_COOLDOWN = 16;
    private static final double GRAPPLE_WRAP_TURNS = 1.0;
    private static final double GRAPPLE_MIN_WRAP_RADIUS = 0.28;
    private static final double GRAPPLE_WRAP_PAD = 0.12;
    private static final double GRAB_ASSIST = 1.0;

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
        public Vec3 grappleDir = Vec3.ZERO;
        public Vec3 grapplePoint = Vec3.ZERO;
        public int grappleTicks;
        public int grappleTargetId = -1;
        public boolean grappleWrap;
        public boolean grappleHit;
        public double grappleReach;
        public double wrapRadius = GRAPPLE_MIN_WRAP_RADIUS;
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
        if (s.grappleTicks > 0) return s.grappleReach + wrapLength(s);
        return s.crackTicks > 0 ? CRACK_LENGTH : WHIP_LENGTH;
    }

    private static double wrapLength(State s) {
        return s.grappleWrap ? 2.0 * Math.PI * s.wrapRadius * GRAPPLE_WRAP_TURNS : 0.0;
    }

    private static double spacingOf(State s) {
        return lengthOf(s) / (NODES - 1);
    }

    public static boolean canCrack(State s) {
        return s.cooldown <= 0 && s.crackTicks <= 0 && s.grappleTicks <= 0;
    }

    public static boolean canGrapple(State s) {
        return s.cooldown <= 0 && s.crackTicks <= 0 && s.grappleTicks <= 0;
    }

    private static Vec3 perpUp(Vec3 dir, Player player) {
        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 up = worldUp.subtract(dir.scale(dir.dot(worldUp)));
        if (up.lengthSqr() < 1.0e-4) {
            double yaw = Math.toRadians(player.yBodyRot);
            Vec3 fwd = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
            up = fwd.subtract(dir.scale(dir.dot(fwd)));
        }
        return up.normalize();
    }

    public static boolean grabbable(Entity e) {
        if (!e.isAlive() || e.isSpectator() || e instanceof WitchFactorEntity) return false;
        if (e instanceof Player p && HahUeuh.MATERIAL_PHASE.isActive(p.getUUID())) return false;
        return e.isPickable() || e instanceof net.minecraft.world.entity.item.ItemEntity;
    }

    public static Entity findGrabTarget(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 dir = player.getLookAngle().normalize();
        Vec3 end = eye.add(dir.scale(GRAPPLE_LENGTH));
        net.minecraft.world.phys.AABB search =
                player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(GRAB_ASSIST);
        Entity direct = null;
        double directDist = Double.MAX_VALUE;
        Entity near = null;
        double nearMiss = GRAB_ASSIST;
        for (Entity e : player.level().getEntities(player, search, GuiltywhipPhysics::grabbable)) {
            net.minecraft.world.phys.AABB box = e.getBoundingBox().inflate(0.3);
            java.util.Optional<Vec3> clip = box.clip(eye, end);
            Vec3 contact = clip.orElse(box.contains(eye) ? eye : null);
            if (contact != null) {
                double d = eye.distanceToSqr(contact);
                if (d < directDist) {
                    directDist = d;
                    direct = e;
                }
                continue;
            }
            double along = box.getCenter().subtract(eye).dot(dir);
            if (along < 0.0 || along > GRAPPLE_LENGTH) continue;
            Vec3 onRay = eye.add(dir.scale(along));
            double miss = distanceToBox(onRay, box);
            if (miss < nearMiss) {
                nearMiss = miss;
                near = e;
            }
        }
        Entity chosen = direct != null ? direct : near;
        if (chosen == null) return null;
        Vec3 grab = grabPointOf(chosen);
        BlockHitResult los = player.level().clip(new ClipContext(eye, grab,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (los.getType() == HitResult.Type.BLOCK
                && los.getLocation().distanceTo(eye) + 0.5 < grab.distanceTo(eye)) {
            return null;
        }
        return chosen;
    }

    private static double distanceToBox(Vec3 p, net.minecraft.world.phys.AABB box) {
        double dx = Math.max(Math.max(box.minX - p.x, 0.0), p.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - p.y, 0.0), p.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - p.z, 0.0), p.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static Vec3 findGrapplePoint(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().normalize().scale(GRAPPLE_LENGTH));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : null;
    }

    public static void grapple(State s, Player player, Vec3 anchor, Entity target, Vec3 blockPoint) {
        Vec3 point;
        if (target != null) {
            point = grabPointOf(target);
            s.grappleWrap = true;
            s.grappleTargetId = target.getId();
            s.wrapRadius = Math.max(GRAPPLE_MIN_WRAP_RADIUS,
                    target.getBbWidth() * 0.5 + GRAPPLE_WRAP_PAD);
        } else if (blockPoint != null) {
            point = blockPoint;
            s.grappleWrap = false;
            s.grappleTargetId = -1;
        } else {
            point = player.getEyePosition()
                    .add(player.getLookAngle().normalize().scale(GRAPPLE_LENGTH));
            s.grappleWrap = false;
            s.grappleTargetId = -1;
        }

        Vec3 to = point.subtract(anchor);
        s.grappleReach = Math.max(0.5, to.length());
        s.grappleDir = to.lengthSqr() < 1.0e-6
                ? player.getLookAngle().normalize()
                : to.normalize();
        s.grapplePoint = point;
        s.grappleHit = target != null || blockPoint != null;
        s.crackUp = perpUp(s.grappleDir, player);
        s.grappleTicks = GRAPPLE_EXTEND_TICKS + GRAPPLE_HOLD_TICKS;
        s.cooldown = GRAPPLE_COOLDOWN;
    }

    public static Vec3 grabPointOf(Entity target) {
        return target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
    }

    public static void crack(State s, Player player, boolean sweeping, Vec3 anchor) {
        Vec3 look = player.getLookAngle().normalize();
        s.crackDir = sweeping ? look : aimFromHand(player, anchor, look);
        s.crackUp = perpUp(s.crackDir, player);
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

    private static Vec3 wrapPathAt(State s, double arc) {
        Vec3 flat = new Vec3(s.grappleDir.x, 0.0, s.grappleDir.z);
        Vec3 h = flat.lengthSqr() < 1.0e-6 ? new Vec3(1.0, 0.0, 0.0) : flat.normalize();
        Vec3 side = new Vec3(-h.z, 0.0, h.x);
        double r = s.wrapRadius;
        double theta = arc / r;
        Vec3 centre = s.grapplePoint.add(h.scale(r));
        return centre.subtract(h.scale(r * Math.cos(theta))).add(side.scale(r * Math.sin(theta)));
    }

    private static Vec3 grapplePathAt(State s, Vec3 anchor, double reach, double arc) {
        if (arc <= reach) return anchor.add(s.grappleDir.scale(arc));
        return wrapPathAt(s, arc - reach);
    }

    private static void poseGrapple(State s, Vec3 anchor, Player owner, Level level) {
        if (s.grappleTargetId >= 0) {
            Entity target = level.getEntity(s.grappleTargetId);
            if (target != null && target.isAlive()) s.grapplePoint = grabPointOf(target);
        }
        Vec3 to = s.grapplePoint.subtract(anchor);
        double dist = Math.max(0.5, to.length());
        if (to.lengthSqr() > 1.0e-6) s.grappleDir = to.normalize();
        if (s.grappleHit && dist > s.grappleReach) s.grappleReach = dist;

        int total = GRAPPLE_EXTEND_TICKS + GRAPPLE_HOLD_TICKS;
        double elapsed = total - s.grappleTicks;
        double front = Math.min(1.0, (elapsed + 1.0) / GRAPPLE_EXTEND_TICKS);

        if (front >= 1.0 && s.grappleHit) {
            holdGrapple(s, anchor, owner, level);
        } else {
            extendGrapple(s, anchor, owner, level, dist, front, elapsed);
        }
    }

    private static void extendGrapple(State s, Vec3 anchor, Player owner, Level level,
                                      double dist, double front, double elapsed) {
        double len = dist + wrapLength(s);
        double radius = CRACK_LOOP_RADIUS * (1.0 - 0.5 * front);
        double settle = Mth.clamp((elapsed + 1.0 - GRAPPLE_EXTEND_TICKS) / SETTLE_TICKS, 0.0, 1.0);
        Vec3 dir = s.grappleDir;

        for (int i = 1; i < NODES; i++) {
            double t = i / (double) (NODES - 1);
            Vec3 target;
            if (t <= front) {
                target = grapplePathAt(s, anchor, dist, t * len);
            } else {
                Vec3 frontPos = grapplePathAt(s, anchor, dist, front * len);
                Vec3 centre = frontPos.add(s.crackUp.scale(radius));
                double theta = (t - front) * len / radius;
                target = centre
                        .subtract(s.crackUp.scale(radius * Math.cos(theta)))
                        .subtract(dir.scale(radius * Math.sin(theta)));
            }
            double follow = FOLLOW_BASE - FOLLOW_TIP_LOSS * t;
            follow += (1.0 - follow) * settle;
            Vec3 blended = s.pos[i].add(target.subtract(s.pos[i]).scale(follow));
            s.prev[i] = s.pos[i];
            s.pos[i] = resolve(level, owner, s.prev[i], blended);
        }
        s.prev[0] = s.pos[0];
        s.pos[0] = anchor;
    }

    private static void holdGrapple(State s, Vec3 anchor, Player owner, Level level) {
        double len = s.grappleReach + wrapLength(s);
        double spacing = len / (NODES - 1);
        Vec3[] pins = new Vec3[NODES];
        int wrapNodes = s.grappleWrap
                ? Math.min(NODES - 2, Math.max(2, (int) Math.round(wrapLength(s) / spacing) + 1))
                : 1;
        for (int i = NODES - wrapNodes; i < NODES; i++) {
            pins[i] = wrapPathAt(s, spacing * (i - (NODES - wrapNodes)));
        }

        for (int i = 1; i < NODES; i++) {
            if (pins[i] != null) continue;
            Vec3 current = s.pos[i];
            Vec3 velocity = current.subtract(s.prev[i]).scale(DAMPING);
            s.prev[i] = current;
            s.pos[i] = current.add(velocity).subtract(0.0, GRAVITY, 0.0);
        }
        s.prev[0] = s.pos[0];
        s.pos[0] = anchor;
        for (int i = 1; i < NODES; i++) {
            if (pins[i] != null) {
                s.prev[i] = s.pos[i];
                s.pos[i] = pins[i];
            }
        }

        relaxPinned(s, anchor, spacing, pins, ITERATIONS);
        for (int i = 1; i < NODES; i++) {
            if (pins[i] == null) s.pos[i] = resolve(level, owner, s.prev[i], s.pos[i]);
        }
        relaxPinned(s, anchor, spacing, pins, SETTLE_ITERATIONS);
    }

    private static void relaxPinned(State s, Vec3 anchor, double spacing, Vec3[] pins, int iterations) {
        for (int iter = 0; iter < iterations; iter++) {
            s.pos[0] = anchor;
            for (int i = 1; i < NODES; i++) {
                if (pins[i] != null) s.pos[i] = pins[i];
            }
            for (int i = 0; i < NODES - 1; i++) {
                Vec3 a = s.pos[i];
                Vec3 b = s.pos[i + 1];
                Vec3 delta = b.subtract(a);
                double dist = delta.length();
                if (dist < 1.0e-6) continue;
                double error = (dist - spacing) / dist;
                boolean aFixed = i == 0 || pins[i] != null;
                boolean bFixed = pins[i + 1] != null;
                if (aFixed && bFixed) continue;
                if (aFixed) {
                    s.pos[i + 1] = b.subtract(delta.scale(error));
                } else if (bFixed) {
                    s.pos[i] = a.add(delta.scale(error));
                } else {
                    Vec3 shift = delta.scale(error * 0.5);
                    s.pos[i] = a.add(shift);
                    s.pos[i + 1] = b.subtract(shift);
                }
            }
        }
        s.pos[0] = anchor;
        for (int i = 1; i < NODES; i++) {
            if (pins[i] != null) s.pos[i] = pins[i];
        }
    }

    public static void step(State s, Vec3 anchor, Player owner) {
        Level level = owner.level();
        if (s.cooldown > 0) s.cooldown--;
        if (s.grappleTicks > 0) {
            poseGrapple(s, anchor, owner, level);
            s.grappleTicks--;
            if (s.grappleTicks <= 0) s.grappleTargetId = -1;
            return;
        }
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
