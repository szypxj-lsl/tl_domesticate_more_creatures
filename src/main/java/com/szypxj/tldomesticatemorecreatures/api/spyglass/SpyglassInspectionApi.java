package com.szypxj.tldomesticatemorecreatures.api.spyglass;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class SpyglassInspectionApi {
    private static final Map<String, Consumer<SpyglassScanCompleted>> LISTENERS = new ConcurrentHashMap<>();

    private SpyglassInspectionApi() {
    }

    public static void registerScanListener(String id, Consumer<SpyglassScanCompleted> listener) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        Objects.requireNonNull(listener, "listener");
        Consumer<SpyglassScanCompleted> previous = LISTENERS.putIfAbsent(id, listener);
        if (previous != null) {
            throw new IllegalArgumentException("Spyglass scan listener already registered: " + id);
        }
    }

    public static void fireCompleted(SpyglassScanCompleted event) {
        LISTENERS.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(entry -> entry.getValue().accept(event));
    }
}
