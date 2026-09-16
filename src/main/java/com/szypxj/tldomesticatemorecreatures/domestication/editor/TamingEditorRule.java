package com.szypxj.tldomesticatemorecreatures.domestication.editor;

import com.szypxj.tldomesticatemorecreatures.domestication.TamingFood;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingMethod;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingRule;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public record TamingEditorRule(
        ResourceLocation entityId,
        TamingMethod method,
        int requiredPlayerLevel,
        Map<ResourceLocation, Integer> nativeFoods,
        Map<ResourceLocation, Integer> extraFoods,
        Map<ResourceLocation, Integer> legacyFoods,
        Set<ResourceLocation> removedNativeFoods
) {
    public TamingEditorRule {
        nativeFoods = Map.copyOf(nativeFoods == null ? Map.of() : nativeFoods);
        extraFoods = Map.copyOf(extraFoods == null ? Map.of() : extraFoods);
        legacyFoods = Map.copyOf(legacyFoods == null ? Map.of() : legacyFoods);
        removedNativeFoods = Set.copyOf(removedNativeFoods == null ? Set.of() : removedNativeFoods);
    }

    public static TamingEditorRule from(TamingRule rule) {
        return new TamingEditorRule(
                rule.entityId(),
                rule.method(),
                rule.requiredPlayerLevel(),
                amounts(rule.nativeFoods()),
                amounts(rule.extraFoods()),
                amounts(rule.legacyFoods()),
                rule.removedNativeFoods()
        );
    }

    public static TamingEditorRule defaults(ResourceLocation entityId) {
        return new TamingEditorRule(entityId, TamingMethod.FEED, 1, Map.of(), Map.of(), Map.of(), Set.of());
    }

    public TamingEditorRule withMethod(TamingMethod value) {
        return new TamingEditorRule(entityId, value, requiredPlayerLevel, nativeFoods, extraFoods, legacyFoods, removedNativeFoods);
    }

    public TamingEditorRule withRequiredPlayerLevel(int value) {
        return new TamingEditorRule(entityId, method, value, nativeFoods, extraFoods, legacyFoods, removedNativeFoods);
    }

    public TamingEditorRule withNativeFoods(Map<ResourceLocation, Integer> value) {
        return new TamingEditorRule(entityId, method, requiredPlayerLevel, value, extraFoods, legacyFoods, removedNativeFoods);
    }

    public TamingEditorRule withExtraFoods(Map<ResourceLocation, Integer> value) {
        return new TamingEditorRule(entityId, method, requiredPlayerLevel, nativeFoods, value, legacyFoods, removedNativeFoods);
    }

    public TamingEditorRule withLegacyFoods(Map<ResourceLocation, Integer> value) {
        return new TamingEditorRule(entityId, method, requiredPlayerLevel, nativeFoods, extraFoods, value, removedNativeFoods);
    }

    public TamingEditorRule withRemovedNativeFoods(Set<ResourceLocation> value) {
        return new TamingEditorRule(entityId, method, requiredPlayerLevel, nativeFoods, extraFoods, legacyFoods, value);
    }

    private static Map<ResourceLocation, Integer> amounts(Iterable<TamingFood> foods) {
        Map<ResourceLocation, Integer> result = new LinkedHashMap<>();
        for (TamingFood food : foods) {
            result.put(food.itemId(), food.amount());
        }
        return Map.copyOf(result);
    }

    public TamingEditorRule normalizeLegacy(Set<ResourceLocation> nativeIds) {
        if (legacyFoods.isEmpty()) {
            return this;
        }
        Map<ResourceLocation, Integer> nativeCopy = new LinkedHashMap<>(nativeFoods);
        Map<ResourceLocation, Integer> extraCopy = new LinkedHashMap<>(extraFoods);
        for (Map.Entry<ResourceLocation, Integer> entry : legacyFoods.entrySet()) {
            if (nativeIds.contains(entry.getKey())) {
                nativeCopy.putIfAbsent(entry.getKey(), entry.getValue());
            } else {
                extraCopy.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        return new TamingEditorRule(entityId, method, requiredPlayerLevel, nativeCopy, extraCopy, Map.of(), new LinkedHashSet<>(removedNativeFoods));
    }
}
