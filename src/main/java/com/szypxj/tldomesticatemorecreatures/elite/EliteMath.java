package com.szypxj.tldomesticatemorecreatures.elite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

public final class EliteMath {
    private EliteMath() {
    }

    public static int wildPointBudget(int level, boolean elite) {
        long base = Math.max(0L, (long) level - 1L);
        long total = elite ? base * 2L : base;
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public static int eliteBonusPoints(int level) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) level - 1L));
    }

    public static double statWeight(double baseWeight, boolean combatStat, double combatMultiplier) {
        double safeBase = Double.isFinite(baseWeight) ? Math.max(0.0D, baseWeight) : 0.0D;
        if (!combatStat) {
            return safeBase;
        }
        double safeMultiplier = Double.isFinite(combatMultiplier) ? Math.max(0.0D, combatMultiplier) : 0.0D;
        return safeBase * safeMultiplier;
    }

    public static int biasedTalentLevel(int minLevel, int maxLevel, int rolls, IntUnaryOperator nextInt) {
        int min = Math.max(1, minLevel);
        int max = Math.max(min, maxLevel);
        if (max <= min) {
            return min;
        }
        int attempts = Math.max(1, rolls);
        int bound = max - min + 1;
        int best = min;
        for (int i = 0; i < attempts; i++) {
            int rolled = min + Math.floorMod(nextInt.applyAsInt(bound), bound);
            if (rolled > best) {
                best = rolled;
            }
        }
        return best;
    }

    public static Map<String, Integer> normalizeWildPoints(Map<String, Integer> source, int targetTotal) {
        LinkedHashMap<String, Integer> sanitized = new LinkedHashMap<>();
        long total = 0L;
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            int value = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
            sanitized.put(entry.getKey(), value);
            total += value;
        }
        int target = Math.max(0, targetTotal);
        if (total <= target) {
            return Map.copyOf(sanitized);
        }
        if (target == 0 || total <= 0L) {
            sanitized.replaceAll((key, value) -> 0);
            return Map.copyOf(sanitized);
        }

        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        List<Fraction> fractions = new ArrayList<>();
        int assigned = 0;
        int order = 0;
        for (Map.Entry<String, Integer> entry : sanitized.entrySet()) {
            double scaled = (double) entry.getValue() * target / total;
            int floor = (int) Math.floor(scaled);
            result.put(entry.getKey(), floor);
            assigned += floor;
            fractions.add(new Fraction(entry.getKey(), scaled - floor, order++));
        }

        fractions.sort((left, right) -> {
            int fractionCompare = Double.compare(right.fraction(), left.fraction());
            return fractionCompare != 0 ? fractionCompare : Integer.compare(left.order(), right.order());
        });
        int remaining = target - assigned;
        for (int i = 0; i < remaining && i < fractions.size(); i++) {
            String key = fractions.get(i).key();
            result.put(key, result.get(key) + 1);
        }
        return Map.copyOf(result);
    }

    private record Fraction(String key, double fraction, int order) {
    }
}
