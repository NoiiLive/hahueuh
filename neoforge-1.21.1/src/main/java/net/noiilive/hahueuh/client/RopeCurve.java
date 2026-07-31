package net.noiilive.hahueuh.client;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class RopeCurve {
    private RopeCurve() {}

    public record Placement(Vec3 centre, Vec3 direction) {}

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
}
