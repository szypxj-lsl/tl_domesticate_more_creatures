package com.szypxj.tldomesticatemorecreatures.config;

public record StatOverride(
        String nameKey,
        String icon,
        String target,
        Double valuePerPoint,
        StatOperation operation,
        Integer maxPoints,
        Boolean wildRandom,
        Boolean showPlayer,
        Boolean showMob,
        Double randomWeight
) {
}
