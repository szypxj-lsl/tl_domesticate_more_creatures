package com.szypxj.tldomesticatemorecreatures.config;

import java.util.Map;

public record TalentDefinition(
        String id,
        String nameKey,
        double weight,
        int minLevel,
        int maxLevel,
        Map<Integer, Map<String, Integer>> levels
) {
    public int clampLevel(int level) {
        return Math.max(minLevel, Math.min(maxLevel, level));
    }

    public Map<String, Integer> effectsFor(int level) {
        int clamped = clampLevel(level);
        Map<String, Integer> exact = levels.get(clamped);
        if (exact != null) {
            return exact;
        }
        for (int candidate = clamped - 1; candidate >= minLevel; candidate--) {
            Map<String, Integer> effects = levels.get(candidate);
            if (effects != null) {
                return effects;
            }
        }
        for (int candidate = clamped + 1; candidate <= maxLevel; candidate++) {
            Map<String, Integer> effects = levels.get(candidate);
            if (effects != null) {
                return effects;
            }
        }
        return Map.of();
    }
}
