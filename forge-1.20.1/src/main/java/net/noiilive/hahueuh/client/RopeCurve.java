package net.noiilive.hahueuh.client;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class RopeCurve {
    private static final int SAMPLES = 40;
    private static final double CLEARANCE = 0.12;
    private static final double POOL_WAVELENGTH = 0.9;
    private static final double POOL_MAX_AMPLITUDE = 0.7;

    private RopeCurve() {}

    public record Placement(Vec3 centre, Vec3 direction) {}

    private record Sampled(List<Vec3> points, boolean[] grounded) {}

    public static List<Vec3> build(Level level, Entity viewer, Vec3 anchor, Vec3 tip, double target) {
        Sampled sampled = sample(level, viewer, anchor, tip, target);
        return pool(sampled, target);
    }

    public static double slackTarget(Vec3 anchor, Vec3 tip, double ropeLength, double tipSpeed,
                                     double tautSpeed) {
        double slack = 1.0 - Mth.clamp(tipSpeed / tautSpeed, 0.0, 1.0);
        double straight = tip.distanceTo(anchor);
        return straight + (ropeLength - straight) * slack;
    }

    private static Sampled sample(Level level, Entity viewer, Vec3 anchor, Vec3 tip, double target) {
        double dist = tip.distanceTo(anchor);
        Vec3 mid = anchor.add(tip).scale(0.5);
        double sag = 0.0;
        if (target - dist > 0.02) {
            double lo = 0.0;
            double hi = target;
            for (int i = 0; i < 16; i++) {
                sag = (lo + hi) * 0.5;
                if (curveLength(anchor, mid.add(0.0, -sag, 0.0), tip) < target) lo = sag; else hi = sag;
            }
        }
        Vec3 ctrl = mid.add(0.0, -sag, 0.0);
        List<Vec3> pts = new ArrayList<>(SAMPLES + 1);
        boolean[] grounded = new boolean[SAMPLES + 1];
        for (int i = 0; i <= SAMPLES; i++) {
            double t = i / (double) SAMPLES;
            Vec3 taut = anchor.add(tip.subtract(anchor).scale(t));
            Vec3 p = bezier(anchor, ctrl, tip, t);
            BlockHitResult hit = level.clip(new ClipContext(taut, p,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Direction face = hit.getDirection();
                Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
                p = hit.getLocation().add(normal.scale(CLEARANCE));
                grounded[i] = face == Direction.UP;
            }
            pts.add(p);
        }
        return new Sampled(pts, grounded);
    }

    private static List<Vec3> pool(Sampled data, double target) {
        List<Vec3> pts = data.points();
        int n = pts.size();
        double total = polyLength(pts);
        double deficit = target - total;
        if (deficit < 0.05) return pts;

        double[] groundArc = new double[n];
        double groundLen = 0.0;
        for (int i = 1; i < n; i++) {
            if (data.grounded()[i - 1] && data.grounded()[i]) groundLen += pts.get(i).distanceTo(pts.get(i - 1));
            groundArc[i] = groundLen;
        }
        if (groundLen < 0.05) return pts;

        List<Vec3> result = pts;
        double lo = 0.0;
        double hi = POOL_MAX_AMPLITUDE;
        for (int it = 0; it < 12; it++) {
            double amp = (lo + hi) * 0.5;
            List<Vec3> disp = serpentine(data, groundArc, groundLen, amp);
            if (polyLength(disp) < target) lo = amp; else hi = amp;
            result = disp;
        }
        return result;
    }

    private static List<Vec3> serpentine(Sampled data, double[] groundArc, double groundLen, double amp) {
        List<Vec3> pts = data.points();
        int n = pts.size();
        List<Vec3> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Vec3 p = pts.get(i);
            if (!data.grounded()[i]) {
                out.add(p);
                continue;
            }
            Vec3 prev = pts.get(Math.max(0, i - 1));
            Vec3 next = pts.get(Math.min(n - 1, i + 1));
            Vec3 dir = new Vec3(next.x - prev.x, 0.0, next.z - prev.z);
            if (dir.lengthSqr() < 1.0e-8) {
                out.add(p);
                continue;
            }
            Vec3 perp = new Vec3(-dir.z, 0.0, dir.x).normalize();
            double s = groundArc[i];
            double envelope = Math.sin(Math.PI * Mth.clamp(s / groundLen, 0.0, 1.0));
            double wave = Math.sin(2.0 * Math.PI * s / POOL_WAVELENGTH);
            out.add(p.add(perp.scale(amp * envelope * wave)));
        }
        return out;
    }

    public static List<Placement> layout(List<Vec3> curve, double segmentLength, Vec3 fallbackDir) {
        int n = curve.size();
        double[] cumulative = new double[n];
        double total = 0.0;
        for (int i = 1; i < n; i++) {
            total += curve.get(i).distanceTo(curve.get(i - 1));
            cumulative[i] = total;
        }
        List<Placement> out = new ArrayList<>();
        if (total < 0.05) return out;

        int count = Math.max(1, (int) Math.round(total / segmentLength));
        double per = total / count;
        int seg = 1;
        for (int i = 0; i < count; i++) {
            double s = (i + 0.5) * per;
            while (seg < n - 1 && cumulative[seg] < s) seg++;
            double segStart = cumulative[seg - 1];
            double segLen = cumulative[seg] - segStart;
            double f = segLen < 1.0e-6 ? 0.0 : (s - segStart) / segLen;
            Vec3 a = curve.get(seg - 1);
            Vec3 b = curve.get(seg);
            Vec3 centre = a.add(b.subtract(a).scale(f));
            Vec3 dir = b.subtract(a);
            dir = dir.lengthSqr() < 1.0e-8 ? fallbackDir : dir.normalize();
            out.add(new Placement(centre, dir));
        }
        return out;
    }

    public static Vec3 endDirection(List<Vec3> curve, Vec3 fallbackDir) {
        if (curve.size() < 2) return fallbackDir;
        Vec3 dir = curve.get(curve.size() - 1).subtract(curve.get(curve.size() - 2));
        return dir.lengthSqr() < 1.0e-8 ? fallbackDir : dir.normalize();
    }

    public static org.joml.Quaternionf rotationFromUp(Vec3 dir) {
        org.joml.Vector3f from = new org.joml.Vector3f(0.0f, 1.0f, 0.0f);
        org.joml.Vector3f to = new org.joml.Vector3f((float) dir.x, (float) dir.y, (float) dir.z).normalize();
        float dot = from.dot(to);
        if (dot > 0.9999f) return new org.joml.Quaternionf();
        if (dot < -0.9999f) return new org.joml.Quaternionf().rotationX((float) Math.PI);
        org.joml.Vector3f axis = new org.joml.Vector3f(from).cross(to).normalize();
        float angle = (float) Math.acos(Mth.clamp(dot, -1.0f, 1.0f));
        return new org.joml.Quaternionf().rotationAxis(angle, axis.x, axis.y, axis.z);
    }

    private static double polyLength(List<Vec3> pts) {
        double len = 0.0;
        for (int i = 1; i < pts.size(); i++) len += pts.get(i).distanceTo(pts.get(i - 1));
        return len;
    }

    private static double curveLength(Vec3 p0, Vec3 p1, Vec3 p2) {
        double len = 0.0;
        Vec3 prev = p0;
        for (int i = 1; i <= 16; i++) {
            Vec3 p = bezier(p0, p1, p2, i / 16.0);
            len += p.distanceTo(prev);
            prev = p;
        }
        return len;
    }

    private static Vec3 bezier(Vec3 p0, Vec3 p1, Vec3 p2, double t) {
        double u = 1.0 - t;
        return p0.scale(u * u).add(p1.scale(2.0 * u * t)).add(p2.scale(t * t));
    }
}
