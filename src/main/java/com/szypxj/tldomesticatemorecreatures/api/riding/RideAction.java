package com.szypxj.tldomesticatemorecreatures.api.riding;

/**
 * Unified rider action identifiers.
 *
 * <p>The legacy values are intentionally retained because existing TDMC
 * compatibility hooks and external integrations already use them as guard
 * categories. New ride-control providers should expose the unified action
 * slots below.</p>
 */
public enum RideAction {
    BOOST,
    FLIGHT,
    ATTACK,
    ABILITY,

    PRIMARY_ATTACK,
    SECONDARY_ATTACK,
    SKILL_1,
    SKILL_2,
    SKILL_3,
    MOVEMENT_SPECIAL,
    UTILITY,
    ROAR;

    public boolean unifiedControlAction() {
        return switch (this) {
            case PRIMARY_ATTACK, SECONDARY_ATTACK, SKILL_1, SKILL_2, SKILL_3, MOVEMENT_SPECIAL, UTILITY, ROAR -> true;
            default -> false;
        };
    }

    /**
     * Maps a unified action back to the legacy guard category so existing
     * RideActionGuard implementations continue to work unchanged.
     */
    public RideAction legacyGuardAction() {
        return switch (this) {
            case PRIMARY_ATTACK -> ATTACK;
            case SECONDARY_ATTACK, SKILL_1, SKILL_2, SKILL_3, UTILITY, ROAR -> ABILITY;
            case MOVEMENT_SPECIAL -> BOOST;
            default -> this;
        };
    }
}
