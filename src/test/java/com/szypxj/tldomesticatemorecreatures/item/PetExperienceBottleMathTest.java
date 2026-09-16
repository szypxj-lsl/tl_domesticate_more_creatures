package com.szypxj.tldomesticatemorecreatures.item;

public final class PetExperienceBottleMathTest {
    public static void main(String[] args) {
        assertEquals(750L, PetExperienceBottleMath.extractAmount(750L, false));
        assertEquals(1000L, PetExperienceBottleMath.extractAmount(3750L, false));
        assertEquals(3750L, PetExperienceBottleMath.extractAmount(3750L, true));
        assertEquals(0L, PetExperienceBottleMath.extractAmount(0L, true));
        assertEquals(400L, PetExperienceBottleMath.remaining(1000L, 600L));
        assertEquals(0L, PetExperienceBottleMath.remaining(400L, 500L));
    }

    private static void assertEquals(long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
