package com.szypxj.tldomesticatemorecreatures.spyglass;

import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStats;
import com.szypxj.tldomesticatemorecreatures.game.DangerRatingStatsService;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;

public final class SpyglassRadarBaseline {
    private static final Object LOCK = new Object();
    private static volatile double[] attackValues = new double[0];
    private static volatile double[] lifeValues = new double[0];
    private static volatile double[] speedValues = new double[0];

    private SpyglassRadarBaseline() {
    }

    /**
     * Builds all three percentile distributions from the same representative threat resolver used by TCB.
     * This keeps skill-aware providers in the comparison population instead of comparing composite power
     * against a vanilla-ATTACK_DAMAGE-only baseline.
     */
    public static void refresh() {
        synchronized (LOCK) {
            int capacity = ForgeRegistries.ENTITY_TYPES.getValues().size();
            double[] attacks = new double[capacity];
            double[] lives = new double[capacity];
            double[] speeds = new double[capacity];
            int attackCount = 0;
            int lifeCount = 0;
            int speedCount = 0;

            for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues()) {
                if (type == EntityType.PLAYER) {
                    continue;
                }
                BaseStats stats = DangerRatingStatsService.get(type);
                double health = nonNegative(stats.maxHealth());
                double attack = nonNegative(stats.attackDamage());
                double speed = nonNegative(stats.movementSpeed());

                if (health > 0.0D) {
                    lives[lifeCount++] = health;
                }
                if (attack > 0.0D) {
                    attacks[attackCount++] = attack;
                }
                if (speed > 0.0D) {
                    speeds[speedCount++] = speed;
                }
            }

            attackValues = sortedUniqueCopy(attacks, attackCount);
            lifeValues = sortedUniqueCopy(lives, lifeCount);
            speedValues = sortedUniqueCopy(speeds, speedCount);
        }
    }

    public static int powerPercentile(double value) {
        ensureReady();
        return SpyglassRadarMath.displayScore(SpyglassRadarMath.percentile(value, attackValues));
    }

    public static int lifePercentile(double value) {
        ensureReady();
        return SpyglassRadarMath.displayScore(SpyglassRadarMath.percentile(value, lifeValues));
    }

    public static int speedPercentile(double value) {
        ensureReady();
        return SpyglassRadarMath.displayScore(SpyglassRadarMath.percentile(value, speedValues));
    }

    private static void ensureReady() {
        if (lifeValues.length == 0) {
            refresh();
        }
    }

    private static double[] sortedUniqueCopy(double[] values, int count) {
        double[] sorted = Arrays.copyOf(values, count);
        Arrays.sort(sorted);
        if (sorted.length < 2) {
            return sorted;
        }
        int uniqueCount = 1;
        for (int i = 1; i < sorted.length; i++) {
            if (Double.compare(sorted[i], sorted[uniqueCount - 1]) != 0) {
                sorted[uniqueCount++] = sorted[i];
            }
        }
        return Arrays.copyOf(sorted, uniqueCount);
    }

    private static double nonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }
}
