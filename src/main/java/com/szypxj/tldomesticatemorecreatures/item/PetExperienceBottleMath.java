package com.szypxj.tldomesticatemorecreatures.item;

public final class PetExperienceBottleMath {
    private static final long NORMAL_EXTRACT_AMOUNT = 1000L;

    private PetExperienceBottleMath() {
    }

    public static long extractAmount(long storedExperience, boolean extractAll) {
        long safeStored = Math.max(0L, storedExperience);
        return extractAll ? safeStored : Math.min(NORMAL_EXTRACT_AMOUNT, safeStored);
    }

    public static long remaining(long storedExperience, long consumed) {
        return Math.max(0L, Math.max(0L, storedExperience) - Math.max(0L, consumed));
    }
}
