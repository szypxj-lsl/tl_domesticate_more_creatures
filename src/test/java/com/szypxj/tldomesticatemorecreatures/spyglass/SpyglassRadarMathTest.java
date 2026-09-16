package com.szypxj.tldomesticatemorecreatures.spyglass;

public final class SpyglassRadarMathTest {
    public static void main(String[] args) {
        double[] unique = new double[]{1.0D, 3.0D, 5.0D};
        assertClose(50.5D, SpyglassRadarMath.percentile(3.0D, unique));
        assertClose(1.0D, SpyglassRadarMath.percentile(1.0D, unique));
        assertClose(100.0D, SpyglassRadarMath.percentile(5.0D, unique));
        assertClose(75.25D, SpyglassRadarMath.percentile(4.0D, unique));
        assertClose(0.0D, SpyglassRadarMath.percentile(0.0D, unique));
        System.out.println("SPYGLASS_RADAR_UNIQUE_VALUE_PASS");
    }

    private static void assertClose(double expected, double actual) {
        if (Math.abs(expected - actual) > 0.000001D) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
