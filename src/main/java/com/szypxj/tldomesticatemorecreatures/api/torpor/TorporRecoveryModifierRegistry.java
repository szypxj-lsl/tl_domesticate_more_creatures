package com.szypxj.tldomesticatemorecreatures.api.torpor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TorporRecoveryModifierRegistry {
    private static final Map<ResourceLocation, TorporRecoveryModifier> MODIFIERS = new ConcurrentHashMap<>();

    private TorporRecoveryModifierRegistry() {
    }

    public static void register(ResourceLocation id, TorporRecoveryModifier modifier) {
        if (id == null || modifier == null) {
            throw new IllegalArgumentException("Torpor recovery modifier id and modifier must not be null");
        }
        TorporRecoveryModifier previous = MODIFIERS.putIfAbsent(id, modifier);
        if (previous != null) {
            throw new IllegalArgumentException("Torpor recovery modifier already registered: " + id);
        }
    }

    public static double resolveRecoveryPerSecond(
            LivingEntity entity,
            double currentTorpor,
            double maxTorpor,
            double normalRecoveryPerSecond,
            boolean unconscious
    ) {
        double resolved = sanitize(normalRecoveryPerSecond);
        for (TorporRecoveryModifier modifier : MODIFIERS.values()) {
            double candidate;
            try {
                candidate = modifier.recoveryPerSecond(entity, currentTorpor, maxTorpor, resolved, unconscious);
            } catch (RuntimeException ignored) {
                continue;
            }
            if (!Double.isFinite(candidate) || candidate < 0.0D) {
                continue;
            }
            resolved = Math.min(resolved, candidate);
        }
        return resolved;
    }

    public static void clearForTests() {
        MODIFIERS.clear();
    }

    private static double sanitize(double value) {
        if (!Double.isFinite(value)) {
            return Double.MAX_VALUE;
        }
        return Math.max(0.0D, value);
    }
}
