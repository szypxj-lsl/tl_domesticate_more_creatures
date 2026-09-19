package com.szypxj.tldomesticatemorecreatures.api.riding;

import java.util.Objects;

public record RideActionInfo(
        RideAction action,
        String nameTranslationKey,
        String descriptionTranslationKey,
        int defaultCooldownTicks,
        boolean holdable,
        boolean chargeable
) {
    public RideActionInfo {
        action = Objects.requireNonNull(action, "action");
        nameTranslationKey = nameTranslationKey == null ? "" : nameTranslationKey;
        descriptionTranslationKey = descriptionTranslationKey == null ? "" : descriptionTranslationKey;
        defaultCooldownTicks = Math.max(0, defaultCooldownTicks);
    }
}
