package com.szypxj.tldomesticatemorecreatures.api.creature.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;
import java.util.Objects;

/** Context passed to optional third-party metadata providers without exposing their classes in the public API. */
public record CreatureCompatProfileContext(
        ServerLevel level,
        ResourceLocation entityTypeId,
        EntityType<?> entityType,
        LivingEntity liveEntity,
        String languageCode
) {
    public CreatureCompatProfileContext {
        entityTypeId = Objects.requireNonNull(entityTypeId, "entityTypeId");
        languageCode = normalizeLanguage(languageCode);
    }

    private static String normalizeLanguage(String value) {
        if (value == null || value.isBlank()) {
            return "en_us";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return normalized.matches("[a-z0-9_]+") ? normalized : "en_us";
    }
}
