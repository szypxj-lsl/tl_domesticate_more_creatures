package com.szypxj.tldomesticatemorecreatures.game;

public final class ExperienceMath {
    private ExperienceMath() {
    }

    public static long saturatingAdd(long current, long amount) {
        long safeCurrent = Math.max(0L, current);
        long safeAmount = Math.max(0L, amount);
        if (safeAmount > Long.MAX_VALUE - safeCurrent) {
            return Long.MAX_VALUE;
        }
        return safeCurrent + safeAmount;
    }

    public static long clampConsumed(long requested, long available) {
        return Math.max(0L, Math.min(Math.max(0L, requested), Math.max(0L, available)));
    }

    public static boolean useMaxLevelPetGroup(int nonMaxEligibleCount, int maxLevelPetCount) {
        return nonMaxEligibleCount <= 0 && maxLevelPetCount > 0;
    }
}
