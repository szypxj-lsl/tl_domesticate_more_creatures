package com.szypxj.tldomesticatemorecreatures.game;

public final class HatchSeedMathTest {
    public static void main(String[] args) {
        long base = 0x1234ABCD55AA7711L;
        long first = HatchSeedMath.childSeed(base, 0);
        long firstAgain = HatchSeedMath.childSeed(base, 0);
        long second = HatchSeedMath.childSeed(base, 1);
        if (first != firstAgain) {
            throw new AssertionError("same base/index must be deterministic");
        }
        if (first == second) {
            throw new AssertionError("different child indices need distinct seeds");
        }
        if (HatchSeedMath.childSeed(base, -1) != first) {
            throw new AssertionError("negative child index must clamp to zero");
        }
    }
}
