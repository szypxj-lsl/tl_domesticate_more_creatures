package com.szypxj.tldomesticatemorecreatures.torpor;

public final class TorporDamageMathTest {
    public static void main(String[] args) {
        assertClose(20.0D, TorporMath.damageTorporLoss(10.0D, 2.0D));
        assertClose(80.0D, TorporMath.torporAfterDamage(100.0D, 10.0D, 2.0D));
        assertClose(0.0D, TorporMath.torporAfterDamage(15.0D, 10.0D, 2.0D));
        assertClose(9.0D, TorporMath.clampLethalDamage(10.0D, 15.0D));
        assertClose(0.0D, TorporMath.clampLethalDamage(1.0D, 15.0D));
        assertClose(9.0D, TorporMath.clampLethalDamage(10.0D, 9.0D));
    }

    private static void assertClose(double expected, double actual) {
        if (Math.abs(expected - actual) > 1.0E-9D) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
