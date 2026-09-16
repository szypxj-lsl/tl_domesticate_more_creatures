package com.szypxj.tldomesticatemorecreatures.spyglass;

public final class SpyglassRadarMath {
    private static final double MAX_RESISTANCE_FOR_EHP = 0.99D;
    private static final double MIN_POSITIVE_SCORE = 1.0D;

    private SpyglassRadarMath() {
    }

    public static double effectiveHealth(double maxHealth, double resistance) {
        if (!Double.isFinite(maxHealth) || maxHealth <= 0.0D) {
            return 0.0D;
        }
        double safeResistance = Double.isFinite(resistance)
                ? Math.max(0.0D, Math.min(MAX_RESISTANCE_FOR_EHP, resistance))
                : 0.0D;
        return maxHealth / (1.0D - safeResistance);
    }

    public static double percentile(double value, double[] sortedBaseline) {
        if (!Double.isFinite(value) || value <= 0.0D || sortedBaseline == null || sortedBaseline.length == 0) {
            return 0.0D;
        }
        if (sortedBaseline.length == 1) {
            return value > 0.0D ? 100.0D : 0.0D;
        }

        double first = sortedBaseline[0];
        double last = sortedBaseline[sortedBaseline.length - 1];
        if (value <= first) {
            return MIN_POSITIVE_SCORE;
        }
        if (value >= last) {
            return 100.0D;
        }

        int high = lowerBound(sortedBaseline, value);
        if (high <= 0) {
            return MIN_POSITIVE_SCORE;
        }
        if (high >= sortedBaseline.length) {
            return 100.0D;
        }

        double highValue = sortedBaseline[high];
        double highScore = scoreAtIndex(high, sortedBaseline.length);
        if (Double.compare(value, highValue) == 0) {
            return highScore;
        }

        int low = high - 1;
        double lowValue = sortedBaseline[low];
        double lowScore = scoreAtIndex(low, sortedBaseline.length);
        if (highValue <= lowValue) {
            return highScore;
        }
        double progress = (value - lowValue) / (highValue - lowValue);
        return clamp100(lowScore + (highScore - lowScore) * progress);
    }

    private static double scoreAtIndex(int index, int size) {
        if (size <= 1) {
            return 100.0D;
        }
        return MIN_POSITIVE_SCORE + (99.0D * index / (size - 1.0D));
    }

    private static int lowerBound(double[] values, double target) {
        int low = 0;
        int high = values.length;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (values[mid] < target) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    public static int displayScore(double value) {
        return (int) Math.round(clamp100(value));
    }

    private static double clamp100(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(100.0D, value));
    }
}
