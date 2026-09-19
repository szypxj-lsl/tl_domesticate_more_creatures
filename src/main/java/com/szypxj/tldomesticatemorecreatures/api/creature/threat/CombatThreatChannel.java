package com.szypxj.tldomesticatemorecreatures.api.creature.threat;

/**
 * Describes one damage-producing combat channel without tying TDMC to a specific mod's ability system.
 * Values are raw/pre-mitigation estimates. The conversion intentionally favors the strongest usable channel
 * instead of summing every mutually-exclusive skill together.
 */
public record CombatThreatChannel(
        String id,
        double directDamage,
        double burstDamage,
        double sustainedDamagePerSecond,
        double affectedTargets,
        double controlSeverity
) {
    public CombatThreatChannel {
        id = id == null ? "" : id;
        directDamage = sanitize(directDamage);
        burstDamage = sanitize(burstDamage);
        sustainedDamagePerSecond = sanitize(sustainedDamagePerSecond);
        affectedTargets = Math.max(1.0D, sanitize(affectedTargets));
        controlSeverity = clamp01(controlSeverity);
    }

    public static CombatThreatChannel direct(String id, double damage) {
        return new CombatThreatChannel(id, damage, damage, 0.0D, 1.0D, 0.0D);
    }

    public static CombatThreatChannel area(String id, double damage, double affectedTargets, double controlSeverity) {
        return new CombatThreatChannel(id, damage, damage, 0.0D, affectedTargets, controlSeverity);
    }

    /**
     * Converts this channel to an attack-damage-like equivalent used by the existing percentile radar.
     * Multi-hit/AOE/control effects receive bounded bonuses so a skill with many mutually-exclusive effects
     * cannot inflate the radar simply by summing all of its theoretical damage.
     */
    public double effectivePower() {
        double direct = directDamage;
        double burstEquivalent = direct + Math.max(0.0D, burstDamage - direct) * 0.35D;
        double sustainedEquivalent = sustainedDamagePerSecond * 2.0D;
        double base = Math.max(direct, Math.max(burstEquivalent, sustainedEquivalent));
        if (base <= 0.0D) {
            return 0.0D;
        }

        double areaSteps = Math.log(affectedTargets) / Math.log(2.0D);
        double areaMultiplier = 1.0D + Math.min(0.20D, Math.max(0.0D, areaSteps) * 0.05D);
        double controlMultiplier = 1.0D + Math.min(0.10D, controlSeverity * 0.10D);
        return base * areaMultiplier * controlMultiplier;
    }

    private static double sanitize(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
