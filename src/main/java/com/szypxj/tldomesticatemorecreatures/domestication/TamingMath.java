package com.szypxj.tldomesticatemorecreatures.domestication;

public final class TamingMath {
    private TamingMath() {
    }

    public static double efficiency(double damageDuringKnockout, double maxHealthAtKnockout) {
        if (maxHealthAtKnockout <= 0.0D) {
            return 0.0D;
        }
        double value = 1.0D - Math.max(0.0D, damageDuringKnockout) / maxHealthAtKnockout;
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    public static double actualBonusRate(double defaultRate, double damageDuringKnockout, double maxHealthAtKnockout) {
        return Math.max(0.0D, defaultRate) * efficiency(damageDuringKnockout, maxHealthAtKnockout);
    }

    public static double foodProgress(int consumed, int required) {
        if (consumed <= 0 || required <= 0) {
            return 0.0D;
        }
        return Math.min(1.0D, consumed / (double) required);
    }
}
