package com.szypxj.tldomesticatemorecreatures.domestication;

import java.util.List;

public final class TamingFoodPlanTest {
    public static void main(String[] args) {
        List<String> nativeItems = List.of("minecraft:carrot", "minecraft:potato", "minecraft:beetroot");
        List<TamingFoodPlan.Spec> nativeFoods = List.of(
                new TamingFoodPlan.Spec("minecraft:carrot", 5),
                new TamingFoodPlan.Spec("minecraft:potato", 4)
        );
        List<TamingFoodPlan.Spec> extras = List.of(
                new TamingFoodPlan.Spec("minecraft:golden_carrot", 2)
        );

        TamingFoodPlan.Result result = TamingFoodPlan.resolve(nativeItems, nativeFoods, extras, List.of(), List.of("minecraft:carrot"));
        assertEquals(3, result.display().size());
        assertEntry(result.display().get(0), "minecraft:potato", 4, true);
        assertEntry(result.display().get(1), "minecraft:beetroot", 0, false);
        assertEntry(result.display().get(2), "minecraft:golden_carrot", 2, true);
        assertEquals(2, result.usable().size());

        TamingFoodPlan.Result legacy = TamingFoodPlan.resolve(
                List.of("minecraft:wheat"),
                List.of(),
                List.of(),
                List.of(
                        new TamingFoodPlan.Spec("minecraft:wheat", 3),
                        new TamingFoodPlan.Spec("minecraft:golden_carrot", 2)
                ),
                List.of()
        );
        assertEquals(2, legacy.display().size());
        assertEntry(legacy.display().get(0), "minecraft:wheat", 3, true);
        assertEntry(legacy.display().get(1), "minecraft:golden_carrot", 2, true);
        assertEquals(2, legacy.usable().size());

        TamingFoodPlan.Result excludedCannotReturnAsExtra = TamingFoodPlan.resolve(
                List.of("minecraft:carrot"),
                List.of(new TamingFoodPlan.Spec("minecraft:carrot", 5)),
                List.of(new TamingFoodPlan.Spec("minecraft:carrot", 99)),
                List.of(),
                List.of("minecraft:carrot")
        );
        assertEquals(0, excludedCannotReturnAsExtra.display().size());
        assertEquals(0, excludedCannotReturnAsExtra.usable().size());

        TamingFoodPlan.Result restored = TamingFoodPlan.resolve(
                List.of("minecraft:carrot"),
                List.of(new TamingFoodPlan.Spec("minecraft:carrot", 5)),
                List.of(),
                List.of(),
                List.of()
        );
        assertEquals(1, restored.display().size());
        assertEntry(restored.display().get(0), "minecraft:carrot", 5, true);

    }

    private static void assertEntry(TamingFoodPlan.Entry entry, String item, int amount, boolean configured) {
        assertEquals(item, entry.item());
        assertEquals(amount, entry.amount());
        assertEquals(configured, entry.configured());
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
