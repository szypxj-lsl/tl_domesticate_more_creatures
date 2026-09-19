package com.szypxj.tldomesticatemorecreatures.api.creature.threat;

import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStats;
import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStatsSource;

import java.util.List;

/**
 * Generic threat description shared by the live spyglass and species-level consumers such as TCB.
 * A provider may describe a growth form through formKey while keeping the public radar at three axes.
 */
public record CreatureThreatProfile(
        double maxHealth,
        double movementSpeed,
        double meleeDamage,
        List<CombatThreatChannel> combatChannels,
        String formKey
) {
    public static final CreatureThreatProfile NONE = new CreatureThreatProfile(
            0.0D, 0.0D, 0.0D, List.of(), ""
    );

    public CreatureThreatProfile {
        maxHealth = sanitize(maxHealth);
        movementSpeed = sanitize(movementSpeed);
        meleeDamage = sanitize(meleeDamage);
        combatChannels = combatChannels == null
                ? List.of()
                : List.copyOf(combatChannels.stream().filter(java.util.Objects::nonNull).toList());
        formKey = formKey == null ? "" : formKey;
    }

    public static CreatureThreatProfile basic(double health, double meleeDamage, double movementSpeed) {
        return new CreatureThreatProfile(health, movementSpeed, meleeDamage, List.of(), "");
    }

    public double effectivePower() {
        double result = meleeDamage;
        for (CombatThreatChannel channel : combatChannels) {
            result = Math.max(result, channel.effectivePower());
        }
        return sanitize(result);
    }

    /**
     * Compatibility projection for existing callers. In danger-rating APIs the BaseStats attack slot carries
     * composite power, not merely the vanilla ATTACK_DAMAGE attribute.
     */
    public BaseStats toDangerStats(BaseStatsSource source) {
        return new BaseStats(maxHealth, effectivePower(), movementSpeed, source);
    }

    private static double sanitize(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }
}
