package com.szypxj.tldomesticatemorecreatures.game;

public final class HatchSeedMath {
    private HatchSeedMath() {
    }

    public static long childSeed(long baseSeed, int childIndex) {
        long index = Math.max(0, childIndex);
        long z = baseSeed + 0x9E3779B97F4A7C15L * (index + 1L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
