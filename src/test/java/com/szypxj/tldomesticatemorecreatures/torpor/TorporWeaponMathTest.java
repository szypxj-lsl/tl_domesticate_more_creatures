package com.szypxj.tldomesticatemorecreatures.torpor;

public final class TorporWeaponMathTest {
    public static void main(String[] args) {
        assertClose(90.0D, TorporWeaponMath.torporFromHit(15.0D, 75.0D));
        assertClose(83.0D, TorporWeaponMath.torporFromHit(8.0D, 75.0D));
        assertClose(32.0D, TorporWeaponMath.torporFromHit(7.0D, 25.0D));
        assertClose(75.0D, TorporWeaponMath.torporFromHit(-4.0D, 75.0D));
        assertClose(0.0D, TorporWeaponMath.torporFromHit(-4.0D, -2.0D));
    }

    private static void assertClose(double expected, double actual) {
        if (Math.abs(expected - actual) > 1.0E-9D) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
