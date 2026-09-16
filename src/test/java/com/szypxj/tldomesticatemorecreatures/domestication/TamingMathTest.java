package com.szypxj.tldomesticatemorecreatures.domestication;

public final class TamingMathTest {
    public static void main(String[] args) {
        assertClose(1.0D, TamingMath.efficiency(0.0D, 100.0D));
        assertClose(0.8D, TamingMath.efficiency(20.0D, 100.0D));
        assertClose(0.0D, TamingMath.efficiency(100.0D, 100.0D));
        assertClose(0.0D, TamingMath.efficiency(150.0D, 100.0D));
        assertClose(0.4D, TamingMath.actualBonusRate(0.5D, 20.0D, 100.0D));
        assertClose(0.5D, TamingMath.foodProgress(1, 2));
        assertClose(0.1D, TamingMath.foodProgress(1, 10));
    }

    private static void assertClose(double expected, double actual) {
        if (Math.abs(expected - actual) > 1.0E-9D) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
