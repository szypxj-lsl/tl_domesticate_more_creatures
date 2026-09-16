package com.szypxj.tldomesticatemorecreatures.torpor;

public final class TorporMathTest {
    public static void main(String[] args) {
        assertClose(500.0D, TorporMath.maxMobTorpor(100.0D, 5.0D));
        assertClose(100.0D, TorporMath.maxMobTorpor(20.0D, 5.0D));
        assertClose(0.0D, TorporMath.maxMobTorpor(-20.0D, 5.0D));
        assertClose(0.0D, TorporMath.maxMobTorpor(20.0D, -5.0D));
        assertClose(25.0D, TorporMath.narcoticPerSecond(1, 25.0D));
        assertClose(75.0D, TorporMath.narcoticPerSecond(3, 25.0D));
        assertClose(25.0D, TorporMath.narcoticArrowImpactTorpor(1, 25.0D));
        assertClose(50.0D, TorporMath.narcoticArrowImpactTorpor(2, 25.0D));
        assertClose(75.0D, TorporMath.narcoticArrowImpactTorpor(3, 25.0D));
        assertClose(30.0D, TorporMath.naturalRecoveryPerSecond(1000.0D, 20.0D, 0.01D));
        assertClose(25.0D, TorporMath.naturalRecoveryPerSecond(500.0D, 20.0D, 0.01D));
        assertEquals(0L, TorporMath.recoveryTicks(100L, 100L, 299L, 200L));
        assertEquals(0L, TorporMath.recoveryTicks(100L, 100L, 300L, 200L));
        assertEquals(20L, TorporMath.recoveryTicks(100L, 100L, 320L, 200L));
        assertEquals(30L, TorporMath.recoveryTicks(310L, 100L, 340L, 200L));
        assertEquals(0L, TorporMath.recoveryTicks(320L, 330L, 400L, 200L));
        if (!TorporMath.shouldEnterUnconscious(1000.0D, 1000.0D)) throw new AssertionError();
        if (TorporMath.shouldWake(1.0D)) throw new AssertionError();
        if (!TorporMath.shouldWake(0.0D)) throw new AssertionError();
    }

    private static void assertEquals(long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertClose(double expected, double actual) {
        if (Math.abs(expected - actual) > 1.0E-9D) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
