package com.szypxj.tldomesticatemorecreatures.health;

public final class HealthRegenerationMathTest {
    public static void main(String[] args) {
        assertClose(17.0D, HealthRegenerationMath.healAmount(200.0D, 15, 0.01D, 1.0D));
        assertClose(10.0D, HealthRegenerationMath.healAmount(1000.0D, 0, 0.01D, 1.0D));
        assertClose(0.0D, HealthRegenerationMath.healAmount(100.0D, 10, -1.0D, -1.0D));
        if (HealthRegenerationMath.shouldHeal(199L, 200L)) throw new AssertionError();
        if (!HealthRegenerationMath.shouldHeal(200L, 200L)) throw new AssertionError();
    }

    private static void assertClose(double expected, double actual) {
        if (Math.abs(expected - actual) > 1.0E-9D) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
