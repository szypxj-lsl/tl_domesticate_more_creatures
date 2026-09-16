package com.szypxj.tldomesticatemorecreatures.api.client;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class SpyglassTitleExtensionRegistry {
    private static final Map<String, SpyglassTitleExtension> EXTENSIONS = new ConcurrentHashMap<>();

    private SpyglassTitleExtensionRegistry() {
    }

    public static void register(String id, SpyglassTitleExtension extension) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        Objects.requireNonNull(extension, "extension");
        SpyglassTitleExtension previous = EXTENSIONS.putIfAbsent(id, extension);
        if (previous != null) {
            throw new IllegalArgumentException("Spyglass title extension already registered: " + id);
        }
    }

    public static List<Entry> entries() {
        return EXTENSIONS.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new Entry(entry.getKey(), entry.getValue()))
                .toList();
    }

    public static void clearForTests() {
        EXTENSIONS.clear();
    }

    public record Entry(String id, SpyglassTitleExtension extension) {
    }
}
