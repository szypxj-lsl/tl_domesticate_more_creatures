package com.szypxj.tldomesticatemorecreatures.elite;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class EliteMathTest {
    public static void main(String[] args) {
        assertEquals(0, EliteMath.wildPointBudget(1, false));
        assertEquals(0, EliteMath.wildPointBudget(1, true));
        assertEquals(9, EliteMath.wildPointBudget(10, false));
        assertEquals(18, EliteMath.wildPointBudget(10, true));
        assertEquals(9, EliteMath.eliteBonusPoints(10));
        assertEquals(0, EliteMath.eliteBonusPoints(1));
        assertEquals(Integer.MAX_VALUE, EliteMath.wildPointBudget(Integer.MAX_VALUE, true));
        assertEquals(0, EliteMath.wildPointBudget(0, true));

        assertDoubleEquals(2.0D, EliteMath.statWeight(1.0D, true, 2.0D));
        assertDoubleEquals(1.0D, EliteMath.statWeight(1.0D, false, 2.0D));
        assertDoubleEquals(0.0D, EliteMath.statWeight(-1.0D, true, 2.0D));

        AtomicInteger index = new AtomicInteger();
        int[] rolls = {0, 2};
        int level = EliteMath.biasedTalentLevel(1, 3, 2, bound -> rolls[index.getAndIncrement()] % bound);
        assertEquals(3, level);
        assertEquals(1, EliteMath.biasedTalentLevel(1, 1, 5, bound -> 0));

        Map<String, Integer> wild = new LinkedHashMap<>();
        wild.put("health", 40);
        wild.put("damage", 30);
        wild.put("armor", 18);
        wild.put("speed", 10);
        Map<String, Integer> normalized = EliteMath.normalizeWildPoints(wild, 49);
        assertEquals(49, normalized.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(20, normalized.get("health"));
        assertEquals(15, normalized.get("damage"));
        assertEquals(9, normalized.get("armor"));
        assertEquals(5, normalized.get("speed"));
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertDoubleEquals(double expected, double actual) {
        if (Math.abs(expected - actual) > 0.0000001D) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
