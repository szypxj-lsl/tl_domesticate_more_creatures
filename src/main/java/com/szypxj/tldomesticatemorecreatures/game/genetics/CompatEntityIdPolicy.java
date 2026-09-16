package com.szypxj.tldomesticatemorecreatures.game.genetics;

import java.util.Locale;

public final class CompatEntityIdPolicy {
    private CompatEntityIdPolicy() {
    }

    public static boolean matchesNamespace(String entityId, String namespace) {
        String normalizedId = normalize(entityId);
        String normalizedNamespace = normalize(namespace);
        if (normalizedId.isEmpty() || normalizedNamespace.isEmpty()) {
            return false;
        }
        int separator = normalizedId.indexOf(':');
        return separator > 0 && normalizedId.substring(0, separator).equals(normalizedNamespace);
    }

    public static boolean matchesAnyNamespace(String entityId, String... namespaces) {
        if (namespaces == null) {
            return false;
        }
        for (String namespace : namespaces) {
            if (matchesNamespace(entityId, namespace)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesExact(String entityId, String expectedId) {
        String normalizedId = normalize(entityId);
        String normalizedExpected = normalize(expectedId);
        return !normalizedId.isEmpty() && normalizedId.equals(normalizedExpected);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
