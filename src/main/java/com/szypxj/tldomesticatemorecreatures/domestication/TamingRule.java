package com.szypxj.tldomesticatemorecreatures.domestication;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public record TamingRule(
        ResourceLocation entityId,
        TamingMethod method,
        int requiredPlayerLevel,
        List<TamingFood> nativeFoods,
        List<TamingFood> extraFoods,
        List<TamingFood> legacyFoods,
        Set<ResourceLocation> removedNativeFoods
) {
    public TamingRule {
        requiredPlayerLevel = Math.max(1, requiredPlayerLevel);
        nativeFoods = List.copyOf(nativeFoods);
        extraFoods = List.copyOf(extraFoods);
        legacyFoods = List.copyOf(legacyFoods);
        removedNativeFoods = Set.copyOf(removedNativeFoods);
    }
}
