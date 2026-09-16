package com.szypxj.tldomesticatemorecreatures.api.riding;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RideStateApi {
    private static final Map<ResourceLocation, RideStateProvider> PROVIDERS = new ConcurrentHashMap<>();

    private RideStateApi() {
    }

    public static void register(ResourceLocation id, RideStateProvider provider) {
        if (id == null || provider == null) {
            throw new IllegalArgumentException("Ride state provider id and provider must not be null");
        }
        RideStateProvider previous = PROVIDERS.putIfAbsent(id, provider);
        if (previous != null) {
            throw new IllegalArgumentException("Ride state provider already registered: " + id);
        }
    }

    public static RideStateSnapshot snapshot(LivingEntity entity) {
        if (entity == null) {
            return RideStateSnapshot.NONE;
        }
        for (RideStateProvider provider : PROVIDERS.values()) {
            try {
                if (!provider.supports(entity)) {
                    continue;
                }
                RideStateSnapshot snapshot = provider.snapshot(entity);
                return snapshot == null ? RideStateSnapshot.NONE : snapshot;
            } catch (RuntimeException ignored) {
            }
        }
        return RideStateSnapshot.NONE;
    }

    public static boolean isRiddenFlying(LivingEntity entity) {
        RideStateSnapshot state = snapshot(entity);
        return state.ridden() && state.flying();
    }

    public static boolean isAccelerating(LivingEntity entity) {
        RideStateSnapshot state = snapshot(entity);
        return state.ridden() && state.accelerating();
    }

    public static void clearForTests() {
        PROVIDERS.clear();
    }
}
