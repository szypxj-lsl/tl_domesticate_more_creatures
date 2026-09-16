package com.szypxj.tldomesticatemorecreatures.config;

public record StatDefinition(
        String id,
        String nameKey,
        String icon,
        String target,
        double valuePerPoint,
        StatOperation operation,
        int maxPoints,
        boolean wildRandom,
        boolean showPlayer,
        boolean showMob,
        double randomWeight
) {
    public StatDefinition merge(StatOverride override) {
        if (override == null) {
            return this;
        }
        return new StatDefinition(
                id,
                override.nameKey() == null ? nameKey : override.nameKey(),
                override.icon() == null ? icon : override.icon(),
                override.target() == null ? target : override.target(),
                override.valuePerPoint() == null ? valuePerPoint : override.valuePerPoint(),
                override.operation() == null ? operation : override.operation(),
                override.maxPoints() == null ? maxPoints : override.maxPoints(),
                override.wildRandom() == null ? wildRandom : override.wildRandom(),
                override.showPlayer() == null ? showPlayer : override.showPlayer(),
                override.showMob() == null ? showMob : override.showMob(),
                override.randomWeight() == null ? randomWeight : override.randomWeight()
        );
    }
}
