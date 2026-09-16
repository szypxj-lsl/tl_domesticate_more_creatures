package com.szypxj.tldomesticatemorecreatures.api.creature;

public record BaseStats(double maxHealth, double attackDamage, double movementSpeed, BaseStatsSource source) {
    public static final BaseStats NONE = new BaseStats(0.0D, 0.0D, 0.0D, BaseStatsSource.NONE);

    public BaseStats {
        maxHealth = sanitize(maxHealth);
        attackDamage = sanitize(attackDamage);
        movementSpeed = sanitize(movementSpeed);
        source = source == null ? BaseStatsSource.NONE : source;
    }

    private static double sanitize(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }
}
