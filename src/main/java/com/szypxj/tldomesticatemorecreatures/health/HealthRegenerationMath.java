package com.szypxj.tldomesticatemorecreatures.health;

public final class HealthRegenerationMath {
    private HealthRegenerationMath() {
    }

    public static double healAmount(double maxHealth, int healthPoints, double maxHealthRate, double healthPointValue) {
        double safeMaxHealth = Math.max(0.0D, maxHealth);
        long safePoints = Math.max(0L, (long) healthPoints);
        double safeRate = Math.max(0.0D, maxHealthRate);
        double safePointValue = Math.max(0.0D, healthPointValue);
        double result = safeMaxHealth * safeRate + safePoints * safePointValue;
        if (!Double.isFinite(result)) {
            return Double.MAX_VALUE;
        }
        return Math.max(0.0D, result);
    }

    public static boolean shouldHeal(long now, long nextHealGameTime) {
        return nextHealGameTime > 0L && now >= nextHealGameTime;
    }
}
