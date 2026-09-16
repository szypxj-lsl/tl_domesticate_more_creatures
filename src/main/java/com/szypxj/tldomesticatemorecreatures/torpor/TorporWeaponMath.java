package com.szypxj.tldomesticatemorecreatures.torpor;

public final class TorporWeaponMath {
    private TorporWeaponMath() {
    }

    public static double torporFromHit(double finalDamage, double fixedTorpor) {
        double damage = Math.max(0.0D, finalDamage);
        double fixed = Math.max(0.0D, fixedTorpor);
        double result = damage + fixed;
        if (!Double.isFinite(result)) {
            return Double.MAX_VALUE;
        }
        return Math.max(0.0D, result);
    }
}
