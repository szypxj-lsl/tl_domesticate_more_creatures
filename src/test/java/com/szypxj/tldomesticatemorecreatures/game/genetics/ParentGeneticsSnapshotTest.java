package com.szypxj.tldomesticatemorecreatures.game.genetics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ParentGeneticsSnapshotTest {
    public static void main(String[] args) {
        Map<String, ParentGeneticsSnapshot.StatPoints> stats = new LinkedHashMap<>();
        stats.put("health", new ParentGeneticsSnapshot.StatPoints(7, 2, 1));
        Map<String, Integer> talents = new LinkedHashMap<>();
        talents.put("unyielding", 3);
        UUID owner = UUID.randomUUID();
        ParentGeneticsSnapshot snapshot = new ParentGeneticsSnapshot(45, 2, 4, stats, talents, owner);
        stats.clear();
        talents.clear();
        if (snapshot.level() != 45) throw new AssertionError();
        if (snapshot.stats().get("health").wild() != 7) throw new AssertionError();
        if (snapshot.talents().get("unyielding") != 3) throw new AssertionError();
        if (!owner.equals(snapshot.ownerUuid())) throw new AssertionError();
        try {
            snapshot.stats().put("damage", new ParentGeneticsSnapshot.StatPoints(1, 0, 0));
            throw new AssertionError("stats must be immutable");
        } catch (UnsupportedOperationException expected) {
        }
    }
}
