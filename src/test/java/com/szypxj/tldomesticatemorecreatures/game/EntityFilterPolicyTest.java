package com.szypxj.tldomesticatemorecreatures.game;

import java.util.List;

public final class EntityFilterPolicyTest {
    public static void main(String[] args) {
        assertTrue(EntityFilterPolicy.isAffected(
                "examplemod:dragon", "BLACKLIST", List.of(), List.of(), List.of(), null
        ));

        assertFalse(EntityFilterPolicy.isAffected(
                "examplemod:butterfly", "BLACKLIST", List.of(), List.of("examplemod"), List.of(), null
        ));

        assertTrue(EntityFilterPolicy.isAffected(
                "examplemod:dragon", "BLACKLIST", List.of(), List.of("examplemod"), List.of("examplemod:dragon"), null
        ));

        assertFalse(EntityFilterPolicy.isAffected(
                "examplemod:dragon", "BLACKLIST", List.of("examplemod:dragon"), List.of("examplemod"), List.of("examplemod:dragon"), null
        ));

        assertFalse(EntityFilterPolicy.isAffected(
                "examplemod:butterfly", "BLACKLIST", List.of(), List.of("examplemod"), List.of(), true
        ));

        assertFalse(EntityFilterPolicy.isAffected(
                "examplemod:dragon", "BLACKLIST", List.of(), List.of("examplemod"), List.of("examplemod:dragon"), false
        ));

        assertTrue(EntityFilterPolicy.isAffected(
                "examplemod:dragon", "WHITELIST", List.of("examplemod:dragon"), List.of("examplemod"), List.of("examplemod:dragon"), null
        ));

        assertFalse(EntityFilterPolicy.isAffected(
                "examplemod:wolf", "WHITELIST", List.of("examplemod:dragon"), List.of("examplemod"), List.of("examplemod:wolf"), null
        ));

        assertTrue(EntityFilterPolicy.isAffected(
                "EXAMPLEMOD:DRAGON", "blacklist", List.of(), List.of(" ExampleMod "), List.of(" examplemod:dragon "), null
        ));
    }

    private static void assertTrue(boolean value) {
        if (!value) {
            throw new AssertionError("expected true");
        }
    }

    private static void assertFalse(boolean value) {
        if (value) {
            throw new AssertionError("expected false");
        }
    }
}
