package com.szypxj.tldomesticatemorecreatures.game.genetics;

import java.util.Map;

public final class HatchGeneticPayloadTest {
    public static void main(String[] args) {
        ParentGeneticsSnapshot a = new ParentGeneticsSnapshot(10, 1, 2, Map.of(), Map.of(), null);
        ParentGeneticsSnapshot b = new ParentGeneticsSnapshot(20, 3, 4, Map.of(), Map.of(), null);
        ChildGeneticsResult result = new ChildGeneticsResult(15, 3, 7, false, null, Map.of("health", 5), Map.of());
        HatchGeneticPayload payload = HatchGeneticPayload.create(123456789L, "iceandfire:fire_dragon", a, b, result);
        if (payload.version() != HatchGeneticPayload.CURRENT_VERSION) throw new AssertionError();
        if (payload.seed() != 123456789L) throw new AssertionError();
        if (!"iceandfire:fire_dragon".equals(payload.childEntityId())) throw new AssertionError();
        if (payload.parentA().level() != 10 || payload.parentB().level() != 20) throw new AssertionError();
        if (payload.result() == null || payload.result().initialLevel() != 15) throw new AssertionError();
        if (!payload.hasResolvedResult()) throw new AssertionError();
        HatchGeneticPayload legacy = HatchGeneticPayload.legacy(9L, a, b);
        if (legacy.version() != HatchGeneticPayload.LEGACY_VERSION) throw new AssertionError();
        if (legacy.hasResolvedResult()) throw new AssertionError();
        System.out.println("HATCH_GENETIC_PAYLOAD_PASS");
    }
}
