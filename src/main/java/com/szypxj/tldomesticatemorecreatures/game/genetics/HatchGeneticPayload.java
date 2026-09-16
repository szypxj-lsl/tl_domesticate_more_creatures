package com.szypxj.tldomesticatemorecreatures.game.genetics;

import com.szypxj.tldomesticatemorecreatures.talent.TalentMigrationService;

public record HatchGeneticPayload(
        int version,
        long seed,
        String childEntityId,
        ParentGeneticsSnapshot parentA,
        ParentGeneticsSnapshot parentB,
        ChildGeneticsResult result
) {
    public static final int CURRENT_VERSION = 3;
    public static final int RESOLVED_LEGACY_VERSION = 2;
    public static final int LEGACY_VERSION = 1;

    public HatchGeneticPayload {
        if (parentA == null || parentB == null) {
            throw new IllegalArgumentException("parent snapshots are required");
        }
        childEntityId = childEntityId == null ? "" : childEntityId.trim();
        if (result != null && !childEntityId.isBlank()) {
            var migratedTalents = TalentMigrationService.migrateLegacyTalents(
                    TalentMigrationService.stableId(seed, childEntityId),
                    result.talents()
            );
            if (!migratedTalents.equals(result.talents())) {
                result = new ChildGeneticsResult(
                        result.initialLevel(),
                        result.paternalMutations(),
                        result.maternalMutations(),
                        result.mutated(),
                        result.ownerUuid(),
                        result.wildStats(),
                        migratedTalents,
                        result.specialTalents()
                );
            }
        }
    }

    public static HatchGeneticPayload create(
            long seed,
            String childEntityId,
            ParentGeneticsSnapshot parentA,
            ParentGeneticsSnapshot parentB,
            ChildGeneticsResult result
    ) {
        if (childEntityId == null || childEntityId.isBlank()) {
            throw new IllegalArgumentException("child entity id is required");
        }
        if (result == null) {
            throw new IllegalArgumentException("child genetics result is required");
        }
        return new HatchGeneticPayload(CURRENT_VERSION, seed, childEntityId, parentA, parentB, result);
    }

    public static HatchGeneticPayload legacy(long seed, ParentGeneticsSnapshot parentA, ParentGeneticsSnapshot parentB) {
        return new HatchGeneticPayload(LEGACY_VERSION, seed, "", parentA, parentB, null);
    }

    public boolean hasResolvedResult() {
        return version >= RESOLVED_LEGACY_VERSION && !childEntityId.isBlank() && result != null;
    }
}
