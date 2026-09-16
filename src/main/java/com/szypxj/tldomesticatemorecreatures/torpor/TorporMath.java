package com.szypxj.tldomesticatemorecreatures.torpor;

public final class TorporMath {
    private TorporMath() {
    }

    public static double maxMobTorpor(double maxHealth, double healthMultiplier) {
        double safeHealth = Math.max(0.0D, maxHealth);
        double safeHealthMultiplier = Math.max(0.0D, healthMultiplier);
        double result = safeHealth * safeHealthMultiplier;
        if (!Double.isFinite(result)) {
            return Double.MAX_VALUE;
        }
        return Math.max(0.0D, result);
    }

    public static double narcoticPerSecond(int effectLevel, double perLevel) {
        return Math.max(0, effectLevel) * Math.max(0.0D, perLevel);
    }

    public static double narcoticArrowImpactTorpor(int effectLevel, double perLevel) {
        return narcoticPerSecond(effectLevel, perLevel);
    }

    public static double recoveryDurationSeconds(double maxTorpor, double baseDurationSeconds, double scaleDurationSeconds) {
        double safeMax = Math.max(0.0D, maxTorpor);
        double safeBase = Math.max(1.0D, baseDurationSeconds);
        double safeScale = Math.max(0.0D, scaleDurationSeconds);
        double result = safeBase + safeScale * Math.sqrt(safeMax / 500.0D);
        if (!Double.isFinite(result) || result <= 0.0D) {
            return safeBase;
        }
        return result;
    }

    public static double naturalRecoveryPerSecond(double maxTorpor, double baseDurationSeconds, double scaleDurationSeconds) {
        double safeMax = Math.max(0.0D, maxTorpor);
        if (safeMax <= 0.0D) {
            return 0.0D;
        }
        double duration = recoveryDurationSeconds(safeMax, baseDurationSeconds, scaleDurationSeconds);
        double result = safeMax / duration;
        if (!Double.isFinite(result)) {
            return Double.MAX_VALUE;
        }
        return Math.max(0.0D, result);
    }

    public static long recoveryTicks(long lastUpdateGameTime, long lastIncreaseGameTime, long nowGameTime, long delayTicks) {
        long now = Math.max(0L, nowGameTime);
        long lastUpdate = Math.max(0L, lastUpdateGameTime);
        long lastIncrease = Math.max(0L, lastIncreaseGameTime);
        long delay = Math.max(0L, delayTicks);
        if (now <= lastUpdate) {
            return 0L;
        }
        long recoveryStart = lastIncrease > Long.MAX_VALUE - delay ? Long.MAX_VALUE : lastIncrease + delay;
        long effectiveStart = Math.max(lastUpdate, recoveryStart);
        return now > effectiveStart ? now - effectiveStart : 0L;
    }

    public static boolean shouldEnterUnconscious(double current, double max) {
        return max > 0.0D && current >= max;
    }

    public static boolean shouldWake(double current) {
        return current <= 0.0D;
    }

    public static double damageTorporLoss(double damage, double multiplier) {
        double safeDamage = Math.max(0.0D, damage);
        double safeMultiplier = Math.max(0.0D, multiplier);
        double result = safeDamage * safeMultiplier;
        if (!Double.isFinite(result)) {
            return Double.MAX_VALUE;
        }
        return result;
    }

    public static double torporAfterDamage(double current, double damage, double multiplier) {
        return Math.max(0.0D, Math.max(0.0D, current) - damageTorporLoss(damage, multiplier));
    }

    public static double clampLethalDamage(double currentHealth, double incomingDamage) {
        double health = Math.max(0.0D, currentHealth);
        double damage = Math.max(0.0D, incomingDamage);
        if (damage < health) {
            return damage;
        }
        return Math.max(0.0D, health - 1.0D);
    }
}
