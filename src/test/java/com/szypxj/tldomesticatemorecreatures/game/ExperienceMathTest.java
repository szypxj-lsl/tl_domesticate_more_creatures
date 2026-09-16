package com.szypxj.tldomesticatemorecreatures.game;

public final class ExperienceMathTest {
    public static void main(String[] args) {
        assertEquals(15L, ExperienceMath.saturatingAdd(10L, 5L));
        assertEquals(Long.MAX_VALUE, ExperienceMath.saturatingAdd(Long.MAX_VALUE - 2L, 10L));
        assertEquals(0L, ExperienceMath.clampConsumed(0L, 50L));
        assertEquals(40L, ExperienceMath.clampConsumed(40L, 50L));
        assertEquals(50L, ExperienceMath.clampConsumed(80L, 50L));
        assertEquals(true, ExperienceMath.useMaxLevelPetGroup(0, 2));
        assertEquals(false, ExperienceMath.useMaxLevelPetGroup(1, 2));
        assertEquals(false, ExperienceMath.useMaxLevelPetGroup(0, 0));
    }

    private static void assertEquals(long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertEquals(boolean expected, boolean actual) {
        if (expected != actual) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
