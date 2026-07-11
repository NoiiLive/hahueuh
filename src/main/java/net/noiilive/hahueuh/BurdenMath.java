package net.noiilive.hahueuh;

public final class BurdenMath {
    private BurdenMath() {}

    private static final double EPSILON = 1.0e-6;

    public static double[] redistribute(double[] weights, int changedIndex, double newValue) {
        int n = weights.length;
        double[] out = new double[n];
        if (n == 0) return out;
        if (changedIndex < 0 || changedIndex >= n) {
            System.arraycopy(weights, 0, out, 0, n);
            return out;
        }

        double clamped = Math.clamp(newValue, 0.0, 100.0);
        out[changedIndex] = clamped;
        double remaining = 100.0 - clamped;

        if (n == 1) {
            out[changedIndex] = 100.0;
            return out;
        }

        double othersSum = 0.0;
        for (int i = 0; i < n; i++) {
            if (i != changedIndex) othersSum += weights[i];
        }

        if (othersSum <= EPSILON) {
            double each = remaining / (n - 1);
            for (int i = 0; i < n; i++) {
                if (i != changedIndex) out[i] = each;
            }
        } else {
            for (int i = 0; i < n; i++) {
                if (i != changedIndex) out[i] = weights[i] / othersSum * remaining;
            }
        }
        return out;
    }

    public static double[] evenOut(int n) {
        double[] out = new double[n];
        if (n == 0) return out;
        double each = 100.0 / n;
        for (int i = 0; i < n; i++) out[i] = each;
        return out;
    }

    public static double[] normalize(double[] weights) {
        int n = weights.length;
        double[] out = new double[n];
        if (n == 0) return out;
        double sum = 0.0;
        for (double w : weights) sum += Math.max(0.0, w);
        if (sum <= EPSILON) return evenOut(n);
        for (int i = 0; i < n; i++) out[i] = Math.max(0.0, weights[i]) / sum * 100.0;
        return out;
    }
}
