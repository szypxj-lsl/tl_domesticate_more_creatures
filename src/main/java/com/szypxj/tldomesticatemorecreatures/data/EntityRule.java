package com.szypxj.tldomesticatemorecreatures.data;

import com.szypxj.tldomesticatemorecreatures.config.StatOverride;

import java.util.Map;

public record EntityRule(
        String entity,
        Boolean enabled,
        Integer minLevel,
        Integer maxLevel,
        Integer fixedLevel,
        Double maxLevelMultiplier,
        Double killExperienceMultiplier,
        Boolean canGainExperience,
        Boolean canLevelUp,
        Double resistanceCap,
        Map<String, Double> randomWeights,
        Map<String, Integer> fixedStats,
        Map<String, StatOverride> statOverrides
) {
    public static EntityRule empty(String entity) {
        return new EntityRule(entity, null, null, null, null, null, null, null, null, null, Map.of(), Map.of(), Map.of());
    }
}
