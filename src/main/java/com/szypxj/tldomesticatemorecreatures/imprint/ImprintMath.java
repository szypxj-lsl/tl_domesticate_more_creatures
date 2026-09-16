package com.szypxj.tldomesticatemorecreatures.imprint;

public final class ImprintMath {
    private ImprintMath() {
    }

    public static int percent(int completed, int total) {
        if (total <= 0) {
            return 0;
        }
        int clamped = Math.max(0, Math.min(total, completed));
        if (clamped >= total) {
            return 100;
        }
        return (clamped * 100) / total;
    }

    public static int levelCapBonus(int initialLevel, int percent) {
        if (initialLevel <= 0 || percent <= 0) {
            return 0;
        }
        int clampedPercent = Math.max(0, Math.min(100, percent));
        long rounded = Math.round((double) initialLevel * clampedPercent / 100.0D);
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, rounded));
    }

    public static long effectiveWindowTicks(long configuredTicks, long remainingGrowthTicks) {
        long configured = Math.max(0L, configuredTicks);
        if (remainingGrowthTicks < 0L) {
            return configured;
        }
        return Math.min(configured, Math.max(0L, remainingGrowthTicks));
    }

    public static long scheduledOffset(long windowTicks, int index, int total, long randomBits) {
        if (windowTicks <= 0L || total <= 0 || index < 0 || index >= total) {
            return 0L;
        }
        long segmentStart = windowTicks * index / total;
        long segmentEnd = windowTicks * (index + 1L) / total;
        long segmentLength = Math.max(1L, segmentEnd - segmentStart);
        long randomSpan = Math.max(1L, segmentLength * 3L / 4L);
        long offset = Long.remainderUnsigned(randomBits, randomSpan);
        return Math.min(Math.max(0L, windowTicks - 1L), segmentStart + offset);
    }
}
