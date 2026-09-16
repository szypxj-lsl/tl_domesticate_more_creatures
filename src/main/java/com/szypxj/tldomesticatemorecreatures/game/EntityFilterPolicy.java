package com.szypxj.tldomesticatemorecreatures.game;

import java.util.Collection;
import java.util.Locale;

public final class EntityFilterPolicy {
    private EntityFilterPolicy() {
    }

    public static boolean isAffected(
            String entityId,
            String mode,
            Collection<? extends String> entityFilter,
            Collection<? extends String> modBlacklist,
            Collection<? extends String> modBlacklistExceptions,
            Boolean explicitEnabled
    ) {
        String normalizedEntityId = normalize(entityId);
        if (normalizedEntityId.isEmpty()) {
            return false;
        }

        int separator = normalizedEntityId.indexOf(':');
        if (separator <= 0) {
            return false;
        }
        String namespace = normalizedEntityId.substring(0, separator);
        boolean entityListed = containsNormalized(entityFilter, normalizedEntityId);
        boolean moduleBlocked = containsNormalized(modBlacklist, namespace);
        boolean moduleException = containsNormalized(modBlacklistExceptions, normalizedEntityId);
        String normalizedMode = normalize(mode).toUpperCase(Locale.ROOT);

        if ("BLACKLIST".equals(normalizedMode) && entityListed) {
            return false;
        }
        if (moduleBlocked && !moduleException) {
            return false;
        }
        if (explicitEnabled != null) {
            return explicitEnabled;
        }
        if ("WHITELIST".equals(normalizedMode)) {
            return entityListed;
        }
        return true;
    }

    private static boolean containsNormalized(Collection<? extends String> values, String expected) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        for (String value : values) {
            if (normalize(value).equals(expected)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
