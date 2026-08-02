package com.greyhat.dark_grey.combat;

import net.minecraft.util.AxisAlignedBB;

/** Pure, deterministic math shared by live polarity physics and diagnostics. */
public final class PolarityPhysicsMath {

    private static final double EPSILON = 1.0E-9D;

    private PolarityPhysicsMath() {}

    public static double pairAcceleration(double distance, double range, double maxPairAcceleration) {
        if (!isFinite(distance) || !isFinite(range)
            || !isFinite(maxPairAcceleration)
            || range <= 0.0D
            || maxPairAcceleration <= 0.0D
            || distance >= range) {
            return 0.0D;
        }
        double normalizedDistance = Math.max(0.0D, distance) / range;
        double falloff = 1.0D - normalizedDistance;
        return maxPairAcceleration * falloff * falloff;
    }

    /**
     * Writes a stable unit vector from A to B. Completely overlapping entities
     * use a deterministic axis derived from their sorted entity IDs.
     */
    public static void directionAtoB(double dx, double dy, double dz, int entityIdA, int entityIdB, double[] out) {
        double lengthSq = dx * dx + dy * dy + dz * dz;
        if (isFinite(lengthSq) && lengthSq > EPSILON * EPSILON) {
            double inverseLength = 1.0D / Math.sqrt(lengthSq);
            out[0] = dx * inverseLength;
            out[1] = dy * inverseLength;
            out[2] = dz * inverseLength;
            return;
        }

        int low = Math.min(entityIdA, entityIdB);
        int high = Math.max(entityIdA, entityIdB);
        int selector = (low * 31 ^ high * 17) & 3;
        double sign = entityIdA <= entityIdB ? 1.0D : -1.0D;
        if (selector == 0) {
            out[0] = sign;
            out[1] = 0.0D;
            out[2] = 0.0D;
        } else if (selector == 1) {
            out[0] = 0.0D;
            out[1] = 0.0D;
            out[2] = sign;
        } else {
            double diagonal = sign / Math.sqrt(2.0D);
            out[0] = diagonal;
            out[1] = 0.0D;
            out[2] = selector == 2 ? diagonal : -diagonal;
        }
    }

    /** Limits only the supplied delta vector. Existing entity motion is never inspected. */
    public static double limitMagnitude(double[] vector, double maximum) {
        double magnitudeSq = vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2];
        if (!isFinite(magnitudeSq) || magnitudeSq <= 0.0D) {
            vector[0] = 0.0D;
            vector[1] = 0.0D;
            vector[2] = 0.0D;
            return 0.0D;
        }
        double magnitude = Math.sqrt(magnitudeSq);
        if (maximum <= 0.0D) {
            vector[0] = 0.0D;
            vector[1] = 0.0D;
            vector[2] = 0.0D;
            return 0.0D;
        }
        if (magnitude > maximum) {
            double scale = maximum / magnitude;
            vector[0] *= scale;
            vector[1] *= scale;
            vector[2] *= scale;
            return maximum;
        }
        return magnitude;
    }

    public static double closingSpeed(double velocityAX, double velocityAY, double velocityAZ, double velocityBX,
        double velocityBY, double velocityBZ, double directionX, double directionY, double directionZ) {
        return -((velocityBX - velocityAX) * directionX + (velocityBY - velocityAY) * directionY
            + (velocityBZ - velocityAZ) * directionZ);
    }

    public static boolean intersectsWithTolerance(AxisAlignedBB a, AxisAlignedBB b, double tolerance) {
        double expansion = Math.max(0.0D, tolerance) * 0.5D;
        return a.expand(expansion, expansion, expansion)
            .intersectsWith(b.expand(expansion, expansion, expansion));
    }

    /**
     * Returns the first contact time in [0,1] for two moving AABBs, or NaN when
     * they do not touch. Motion arguments are actual per-tick displacements.
     */
    public static double sweptContactTime(AxisAlignedBB startA, AxisAlignedBB startB, double moveAX, double moveAY,
        double moveAZ, double moveBX, double moveBY, double moveBZ, double tolerance) {
        double[] interval = new double[] { Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY };
        double expansion = Math.max(0.0D, tolerance) * 0.5D;
        if (!clipAxis(
            startA.minX - expansion,
            startA.maxX + expansion,
            startB.minX - expansion,
            startB.maxX + expansion,
            moveAX - moveBX,
            interval)
            || !clipAxis(
                startA.minY - expansion,
                startA.maxY + expansion,
                startB.minY - expansion,
                startB.maxY + expansion,
                moveAY - moveBY,
                interval)
            || !clipAxis(
                startA.minZ - expansion,
                startA.maxZ + expansion,
                startB.minZ - expansion,
                startB.maxZ + expansion,
                moveAZ - moveBZ,
                interval)) {
            return Double.NaN;
        }
        double entry = Math.max(0.0D, interval[0]);
        double exit = Math.min(1.0D, interval[1]);
        return entry <= exit ? entry : Double.NaN;
    }

    public static double aabbGap(AxisAlignedBB a, AxisAlignedBB b) {
        double dx = axisGap(a.minX, a.maxX, b.minX, b.maxX);
        double dy = axisGap(a.minY, a.maxY, b.minY, b.maxY);
        double dz = axisGap(a.minZ, a.maxZ, b.minZ, b.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static boolean clipAxis(double minA, double maxA, double minB, double maxB, double relativeMotion,
        double[] interval) {
        if (Math.abs(relativeMotion) <= EPSILON) {
            return maxA >= minB && maxB >= minA;
        }

        double entry;
        double exit;
        if (relativeMotion > 0.0D) {
            entry = (minB - maxA) / relativeMotion;
            exit = (maxB - minA) / relativeMotion;
        } else {
            entry = (maxB - minA) / relativeMotion;
            exit = (minB - maxA) / relativeMotion;
        }
        if (entry > interval[0]) interval[0] = entry;
        if (exit < interval[1]) interval[1] = exit;
        return interval[0] <= interval[1];
    }

    private static double axisGap(double minA, double maxA, double minB, double maxB) {
        if (maxA < minB) return minB - maxA;
        if (maxB < minA) return minA - maxB;
        return 0.0D;
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
