package com.szypxj.tldomesticatemorecreatures.talent.active;

public final class ActiveTalentConfig {
    public static final ShadowstepConfig DEFAULT_SHADOWSTEP = new ShadowstepConfig(100, 16.0D, 0.25D, 32.0D, 5, 4.0D, 800, 0.10D);
    public static final CamouflageConfig DEFAULT_CAMOUFLAGE = new CamouflageConfig(6000, 0.20D, 3.0D, 300, 0.30F);
    public static final Snapshot DEFAULT = new Snapshot(DEFAULT_SHADOWSTEP, DEFAULT_CAMOUFLAGE);

    private ActiveTalentConfig() {
    }

    public record Snapshot(ShadowstepConfig shadowstep, CamouflageConfig camouflage) {
    }

    public record ShadowstepConfig(
            int markingDurationTicks,
            double slowRadius,
            double enemyActionMultiplier,
            double markRange,
            int maxMarks,
            double dashBlocksPerTick,
            int cooldownTicks,
            double refundPerUnusedMark
    ) {
        public int cooldownSeconds() {
            return Math.max(0, cooldownTicks / 20);
        }
    }

    public record CamouflageConfig(
            int maxDurationTicks,
            double enemyDetectionMultiplier,
            double ambushDamageMultiplier,
            int cooldownTicks,
            float renderAlpha
    ) {
        public int cooldownSeconds() {
            return Math.max(0, cooldownTicks / 20);
        }
    }
}
