package com.szypxj.tldomesticatemorecreatures.domestication;

import java.util.List;

public final class TamingRuleFileParserTest {
    public static void main(String[] args) {
        String content = """
                rules = [
                    { entity = "minecraft:pig", method = "FEED", native_foods = [{ item = "minecraft:carrot", amount = 5 }, { item = "minecraft:potato", amount = 4 }], extra_foods = [{ item = "minecraft:golden_carrot", amount = 2 }], removed_native_foods = ["minecraft:beetroot"] },
                    { entity = "minecraft:zombie", method = "KNOCKOUT", foods = [{ item = "minecraft:rotten_flesh", amount = 10 }] }
                ]
                """;

        List<TamingRuleFileParser.ParsedRule> rules = TamingRuleFileParser.parse(content);
        assertEquals(2, rules.size());

        TamingRuleFileParser.ParsedRule pig = rules.get(0);
        assertEquals("minecraft:pig", pig.entity());
        assertEquals("FEED", pig.method());
        assertEquals(2, pig.nativeFoods().size());
        assertEquals("minecraft:carrot", pig.nativeFoods().get(0).item());
        assertEquals(5, pig.nativeFoods().get(0).amount());
        assertEquals("minecraft:potato", pig.nativeFoods().get(1).item());
        assertEquals(4, pig.nativeFoods().get(1).amount());
        assertEquals(1, pig.extraFoods().size());
        assertEquals("minecraft:golden_carrot", pig.extraFoods().get(0).item());
        assertEquals(2, pig.extraFoods().get(0).amount());
        assertEquals(0, pig.legacyFoods().size());
        assertEquals(1, pig.removedNativeFoods().size());
        assertEquals("minecraft:beetroot", pig.removedNativeFoods().get(0));

        TamingRuleFileParser.ParsedRule zombie = rules.get(1);
        assertEquals(0, zombie.nativeFoods().size());
        assertEquals(0, zombie.extraFoods().size());
        assertEquals(1, zombie.legacyFoods().size());
        assertEquals(0, zombie.removedNativeFoods().size());
        assertEquals("minecraft:rotten_flesh", zombie.legacyFoods().get(0).item());
        assertEquals(10, zombie.legacyFoods().get(0).amount());

        assertEquals(false, TamingRuleFileParser.hasActiveLegacySections("# [[knockout]]\n# [[feeding]]\n"));
        assertEquals(true, TamingRuleFileParser.hasActiveLegacySections("[[knockout]]\nentity = \"minecraft:zombie\"\n"));
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }
}
