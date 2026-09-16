package com.szypxj.tldomesticatemorecreatures.api.stat;

import com.szypxj.tldomesticatemorecreatures.config.StatDefinition;
import com.szypxj.tldomesticatemorecreatures.game.AttributeService;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

public final class ExternalStatRegistry {
    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    private ExternalStatRegistry() {
    }

    public static synchronized void register(
            String ownerModId,
            StatDefinition definition,
            Predicate<LivingEntity> availability,
            ToDoubleFunction<LivingEntity> displayedValue,
            String displayFormat
    ) {
        register(ownerModId, definition, availability, displayedValue, entity -> definition.maxPoints(), displayFormat);
    }

    public static synchronized void register(
            String ownerModId,
            StatDefinition definition,
            Predicate<LivingEntity> availability,
            ToDoubleFunction<LivingEntity> displayedValue,
            ToIntFunction<LivingEntity> pointLimit,
            String displayFormat
    ) {
        Objects.requireNonNull(ownerModId, "ownerModId");
        Objects.requireNonNull(definition, "definition");
        if (ownerModId.isBlank()) {
            throw new IllegalArgumentException("ownerModId cannot be blank");
        }
        if (definition.id() == null || definition.id().isBlank()) {
            throw new IllegalArgumentException("stat id cannot be blank");
        }
        if (ENTRIES.containsKey(definition.id())) {
            throw new IllegalArgumentException("External stat already registered: " + definition.id());
        }
        ENTRIES.put(definition.id(), new Entry(
                ownerModId,
                definition,
                availability == null ? entity -> true : availability,
                displayedValue == null ? entity -> 0.0D : displayedValue,
                pointLimit == null ? entity -> definition.maxPoints() : pointLimit,
                normalizeDisplayFormat(displayFormat)
        ));
        AttributeService.clearDefinitionCache();
    }

    public static synchronized List<StatDefinition> appendDefinitions(List<StatDefinition> baseDefinitions) {
        List<StatDefinition> result = new ArrayList<>(baseDefinitions == null ? List.of() : baseDefinitions);
        for (Entry entry : ENTRIES.values()) {
            boolean duplicate = result.stream().anyMatch(definition -> definition.id().equals(entry.definition().id()));
            if (!duplicate) {
                result.add(entry.definition());
            }
        }
        return List.copyOf(result);
    }

    public static synchronized Optional<Entry> entry(String statId) {
        return Optional.ofNullable(ENTRIES.get(statId));
    }

    public static boolean isExternal(String statId) {
        return entry(statId).isPresent();
    }

    public static boolean isAvailable(LivingEntity entity, StatDefinition definition) {
        return entry(definition.id())
                .map(value -> value.availability().test(entity))
                .orElse(false);
    }

    public static double displayedValue(LivingEntity entity, StatDefinition definition) {
        return entry(definition.id())
                .map(value -> value.displayedValue().applyAsDouble(entity))
                .orElse(0.0D);
    }

    public static int pointLimit(LivingEntity entity, StatDefinition definition) {
        return entry(definition.id())
                .map(value -> value.pointLimit().applyAsInt(entity))
                .orElse(definition.maxPoints());
    }

    public static String displayFormat(StatDefinition definition) {
        return entry(definition.id()).map(Entry::displayFormat).orElse("NUMBER");
    }

    private static String normalizeDisplayFormat(String displayFormat) {
        if ("MULTIPLIER".equals(displayFormat) || "PERCENT".equals(displayFormat)) {
            return displayFormat;
        }
        return "NUMBER";
    }

    public record Entry(
            String ownerModId,
            StatDefinition definition,
            Predicate<LivingEntity> availability,
            ToDoubleFunction<LivingEntity> displayedValue,
            ToIntFunction<LivingEntity> pointLimit,
            String displayFormat
    ) {
    }
}
