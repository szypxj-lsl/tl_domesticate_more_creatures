package com.szypxj.tldomesticatemorecreatures.talent.active;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class ActiveTalentRegistry {
    private static final Set<String> ACTIVE_IDS = Set.of(ActiveTalentIds.SHADOWSTEP, ActiveTalentIds.CAMOUFLAGE);

    private ActiveTalentRegistry() {
    }

    public static boolean isActive(String id) {
        return ACTIVE_IDS.contains(id);
    }

    public static Optional<String> activeTalent(Set<String> talents) {
        if (talents == null || talents.isEmpty()) return Optional.empty();
        return talents.stream().filter(ACTIVE_IDS::contains).sorted().findFirst();
    }

    public static Set<String> normalizeToSingleActive(UUID stableId, Set<String> talents) {
        Set<String> source = talents == null ? Set.of() : talents;
        List<String> active = source.stream().filter(ACTIVE_IDS::contains).sorted().toList();
        if (active.size() <= 1) return Set.copyOf(source);
        UUID id = stableId == null ? new UUID(0L, 0L) : stableId;
        Random random = new Random(id.getMostSignificantBits() ^ Long.rotateLeft(id.getLeastSignificantBits(), 13));
        return keepOnly(source, active.get(random.nextInt(active.size())));
    }

    public static Set<String> normalizeToSingleActive(RandomSource random, Set<String> talents) {
        Set<String> source = talents == null ? Set.of() : talents;
        List<String> active = new ArrayList<>(source.stream().filter(ACTIVE_IDS::contains).sorted().toList());
        if (active.size() <= 1) return Set.copyOf(source);
        RandomSource actual = random == null ? RandomSource.create() : random;
        return keepOnly(source, active.get(actual.nextInt(active.size())));
    }

    private static Set<String> keepOnly(Set<String> source, String keep) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String id : source) {
            if (!ACTIVE_IDS.contains(id) || id.equals(keep)) result.add(id);
        }
        return Set.copyOf(result);
    }
}
