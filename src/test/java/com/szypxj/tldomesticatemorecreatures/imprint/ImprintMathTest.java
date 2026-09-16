package com.szypxj.tldomesticatemorecreatures.imprint;

public final class ImprintMathTest {
    public static void main(String[] args) {
        assertEquals(0, ImprintMath.percent(0, 3));
        assertEquals(33, ImprintMath.percent(1, 3));
        assertEquals(66, ImprintMath.percent(2, 3));
        assertEquals(100, ImprintMath.percent(3, 3));
        assertEquals(99, ImprintMath.levelCapBonus(150, 66));
        assertEquals(150, ImprintMath.levelCapBonus(150, 100));
        assertEquals(6000L, ImprintMath.effectiveWindowTicks(24000L, 6000L));
        assertEquals(24000L, ImprintMath.effectiveWindowTicks(24000L, -1L));
        assertEquals(24000L, ImprintMath.effectiveWindowTicks(24000L, 40000L));
        assertEquals(0L, ImprintMath.effectiveWindowTicks(24000L, 0L));
        long first = ImprintMath.scheduledOffset(24000L, 0, 3, 123L);
        long third = ImprintMath.scheduledOffset(24000L, 2, 3, 456L);
        assertRange(0L, 5999L, first);
        assertRange(16000L, 21999L, third);
        System.out.println("IMPRINT_MATH_PASS");
    }

    private static void assertRange(long min, long max, long actual) {
        if (actual < min || actual > max) {
            throw new AssertionError("expected range=" + min + ".." + max + " actual=" + actual);
        }
    }

    private static void assertEquals(long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
