package com.szypxj.tldomesticatemorecreatures.game.genetics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.UUID;

public record ParentGeneticsSnapshot(
        int level,
        int paternalMutations,
        int maternalMutations,
        Map<String, StatPoints> stats,
        Map<String, Integer> talents,
        Set<String> specialTalents,
        UUID ownerUuid
) {
    public ParentGeneticsSnapshot {
        level = Math.max(1, level);
        paternalMutations = Math.max(0, paternalMutations);
        maternalMutations = Math.max(0, maternalMutations);
        stats = Map.copyOf(new LinkedHashMap<>(stats == null ? Map.of() : stats));
        talents = Map.copyOf(new LinkedHashMap<>(talents == null ? Map.of() : talents));
        specialTalents = Set.copyOf(new LinkedHashSet<>(specialTalents == null ? Set.of() : specialTalents));
    }

    public ParentGeneticsSnapshot(
            int level,
            int paternalMutations,
            int maternalMutations,
            Map<String, StatPoints> stats,
            Map<String, Integer> talents,
            UUID ownerUuid
    ) {
        this(level, paternalMutations, maternalMutations, stats, talents, Set.of(), ownerUuid);
    }

    public StatPoints stat(String id) {
        return stats.getOrDefault(id, new StatPoints(0, 0, 0));
    }

    public record StatPoints(int wild, int trained, int talent) {
        public StatPoints {
            wild = Math.max(0, wild);
            trained = Math.max(0, trained);
            talent = Math.max(0, talent);
        }
    }
}
