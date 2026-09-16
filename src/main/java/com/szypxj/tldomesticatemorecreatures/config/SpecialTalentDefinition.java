package com.szypxj.tldomesticatemorecreatures.config;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

public record SpecialTalentDefinition(
        String id,
        String nameKey,
        boolean enabled,
        Set<String> allowedEntities,
        double spawnChance,
        double inheritOneParentChance,
        double inheritBothParentsChance
) {
    public SpecialTalentDefinition {
        id = id == null ? "" : id.trim();
        nameKey = nameKey == null || nameKey.isBlank()
                ? "talent.tl_domesticate_more_creatures.special." + id
                : nameKey.trim();
        Set<String> normalized = new LinkedHashSet<>();
        if (allowedEntities != null) {
            for (String raw : allowedEntities) {
                ResourceLocation parsed = ResourceLocation.tryParse(raw == null ? "" : raw.trim());
                if (parsed != null) {
                    normalized.add(parsed.toString());
                }
            }
        }
        allowedEntities = Set.copyOf(normalized);
        spawnChance = clampChance(spawnChance);
        inheritOneParentChance = clampChance(inheritOneParentChance);
        inheritBothParentsChance = clampChance(inheritBothParentsChance);
    }

    public boolean allows(ResourceLocation entityId) {
        return enabled && entityId != null && allowedEntities.contains(entityId.toString());
    }

    private static double clampChance(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
