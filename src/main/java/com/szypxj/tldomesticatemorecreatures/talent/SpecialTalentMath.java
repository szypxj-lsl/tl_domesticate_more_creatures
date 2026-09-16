package com.szypxj.tldomesticatemorecreatures.talent;

public final class SpecialTalentMath {
    private SpecialTalentMath() {
    }

    public static double maxAnger(double maxHealth, double ratio) {
        return Math.max(0.0D, maxHealth) * Math.max(0.0D, ratio);
    }

    public static double outgoingDamage(double damage, boolean berserk, double normalMultiplier, double berserkMultiplier) {
        return Math.max(0.0D, damage) * Math.max(0.0D, berserk ? berserkMultiplier : normalMultiplier);
    }

    public static double incomingDamage(double damage, double multiplier) {
        return Math.max(0.0D, damage) * Math.max(0.0D, multiplier);
    }

    public static double rageGain(double finalDamage, boolean berserk, double berserkGainMultiplier) {
        double base = Math.max(0.0D, finalDamage);
        return berserk ? base * Math.max(0.0D, berserkGainMultiplier) : base;
    }

    public static double decayPerSecond(double maxAnger, double percentPerSecond) {
        return Math.max(0.0D, maxAnger) * Math.max(0.0D, percentPerSecond);
    }

    public static double decayForTicks(double current, double maxAnger, double percentPerSecond, long ticks) {
        double decay = decayPerSecond(maxAnger, percentPerSecond) * (Math.max(0L, ticks) / 20.0D);
        return Math.max(0.0D, current - decay);
    }

    public static double bleedingDamage(double currentHealth, double percent) {
        return Math.max(0.0D, currentHealth) * Math.max(0.0D, percent);
    }

    public static double clampAnger(double value, double maxAnger) {
        double max = Math.max(0.0D, maxAnger);
        return Math.max(0.0D, Math.min(max, value));
    }
}
