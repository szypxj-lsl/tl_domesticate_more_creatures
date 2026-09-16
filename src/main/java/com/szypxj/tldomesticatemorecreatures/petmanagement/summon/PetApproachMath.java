package com.szypxj.tldomesticatemorecreatures.petmanagement.summon;

public final class PetApproachMath {
    public static final int MAX_AGE_TICKS = 140;

    private PetApproachMath() {
    }

    public static double completionRadius(double bodyWidth) {
        return Math.max(1.75D, Math.max(0.0D, bodyWidth) * 0.34D + 0.85D);
    }

    public static double cruiseSpeed(double bodyWidth, double bodyHeight, boolean flying) {
        double visualSize = Math.max(Math.max(0.0D, bodyWidth), Math.max(0.0D, bodyHeight) * 0.55D);
        double sizeBonus = clamp((visualSize - 1.0D) * 0.018D, 0.0D, 0.20D);
        return flying ? 0.62D + sizeBonus : 0.38D + sizeBonus * 0.55D;
    }

    public static double stepDistance(double distance, double completionRadius, double cruiseSpeed, int moveAge) {
        if (distance <= 0.0D || cruiseSpeed <= 0.0D) return 0.0D;
        double startup = clamp((moveAge + 1.0D) / 7.0D, 0.42D, 1.0D);
        double slowWindow = Math.max(4.0D, completionRadius * 2.25D);
        double arrival = clamp((distance - completionRadius) / slowWindow, 0.32D, 1.0D);
        return Math.min(distance, cruiseSpeed * startup * arrival);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
