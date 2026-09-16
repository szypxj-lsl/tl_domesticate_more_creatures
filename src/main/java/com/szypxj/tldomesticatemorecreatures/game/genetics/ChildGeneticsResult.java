package com.szypxj.tldomesticatemorecreatures.game.genetics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.UUID;

public record ChildGeneticsResult(
        int initialLevel,
        int paternalMutations,
        int maternalMutations,
        boolean mutated,
        UUID ownerUuid,
        Map<String, Integer> wildStats,
        Map<String, Integer> talents,
        Set<String> specialTalents
) {
    public ChildGeneticsResult {
        initialLevel = Math.max(1, initialLevel);
        paternalMutations = Math.max(0, paternalMutations);
        maternalMutations = Math.max(0, maternalMutations);
        wildStats = immutableNonNegativeMap(wildStats);
        talents = immutablePositiveMap(talents);
        specialTalents = Set.copyOf(new LinkedHashSet<>(specialTalents == null ? Set.of() : specialTalents));
    }

    public ChildGeneticsResult(
            int initialLevel,
            int paternalMutations,
            int maternalMutations,
            boolean mutated,
            UUID ownerUuid,
            Map<String, Integer> wildStats,
            Map<String, Integer> talents
    ) {
        this(initialLevel, paternalMutations, maternalMutations, mutated, ownerUuid, wildStats, talents, Set.of());
    }

    public int wildStat(String id) {
        return wildStats.getOrDefault(id, 0);
    }

    private static Map<String, Integer> immutableNonNegativeMap(Map<String, Integer> source) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (source != null) {
            for (Map.Entry<String, Integer> entry : source.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) {
                    continue;
                }
                result.put(entry.getKey(), Math.max(0, entry.getValue() == null ? 0 : entry.getValue()));
            }
        }
        return Map.copyOf(result);
    }

    private static Map<String, Integer> immutablePositiveMap(Map<String, Integer> source) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (source != null) {
            for (Map.Entry<String, Integer> entry : source.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                    continue;
                }
                result.put(entry.getKey(), Math.max(1, entry.getValue()));
            }
        }
        return Map.copyOf(result);
    }
}
