package com.szypxj.tldomesticatemorecreatures.game.genetics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ChildGeneticsResultTest {
    public static void main(String[] args) {
        Map<String, Integer> wild = new LinkedHashMap<>();
        wild.put("health", 12);
        Map<String, Integer> talents = new LinkedHashMap<>();
        talents.put("wrath", 2);
        UUID owner = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        ChildGeneticsResult result = new ChildGeneticsResult(
                42,
                3,
                4,
                true,
                owner,
                wild,
                talents
        );

        wild.put("health", 99);
        talents.put("wrath", 3);

        if (result.initialLevel() != 42) throw new AssertionError();
        if (result.paternalMutations() != 3) throw new AssertionError();
        if (result.maternalMutations() != 4) throw new AssertionError();
        if (!result.mutated()) throw new AssertionError();
        if (!owner.equals(result.ownerUuid())) throw new AssertionError();
        if (result.wildStats().get("health") != 12) throw new AssertionError();
        if (result.talents().get("wrath") != 2) throw new AssertionError();

        boolean immutable = false;
        try {
            result.wildStats().put("damage", 1);
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        if (!immutable) throw new AssertionError("wild stats must be immutable");
        System.out.println("CHILD_GENETICS_RESULT_PASS");
    }
}
