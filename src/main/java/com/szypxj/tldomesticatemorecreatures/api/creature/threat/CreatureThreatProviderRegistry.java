package com.szypxj.tldomesticatemorecreatures.api.creature.threat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Public registry so other creature mods/addons can provide stage- and skill-aware threat data. */
public final class CreatureThreatProviderRegistry {
    private static final CopyOnWriteArrayList<CreatureThreatProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private CreatureThreatProviderRegistry() {
    }

    public static void register(CreatureThreatProvider provider) {
        Objects.requireNonNull(provider, "provider");
        PROVIDERS.removeIf(existing -> existing.id().equals(provider.id()));
        PROVIDERS.add(provider);
        PROVIDERS.sort(Comparator.comparingInt(CreatureThreatProvider::priority).reversed());
    }

    public static List<CreatureThreatProvider> providers() {
        return List.copyOf(PROVIDERS);
    }

    public static CreatureThreatProfile resolveLive(
            ResourceLocation entityTypeId,
            LivingEntity entity,
            CreatureThreatProfile fallback
    ) {
        CreatureThreatProfile base = fallback == null ? CreatureThreatProfile.NONE : fallback;
        if (entityTypeId == null || entity == null) {
            return base;
        }
        for (CreatureThreatProvider provider : snapshotMatching(entityTypeId)) {
            try {
                CreatureThreatProfile resolved = provider.liveProfile(entity, base);
                if (resolved != null) {
                    return resolved;
                }
            } catch (RuntimeException ignored) {
                // A broken optional compatibility provider must never break the base radar.
            }
        }
        return base;
    }

    public static CreatureThreatProfile resolveRepresentative(
            ResourceLocation entityTypeId,
            EntityType<?> type,
            CreatureThreatProfile fallback
    ) {
        CreatureThreatProfile base = fallback == null ? CreatureThreatProfile.NONE : fallback;
        if (entityTypeId == null || type == null) {
            return base;
        }
        for (CreatureThreatProvider provider : snapshotMatching(entityTypeId)) {
            try {
                CreatureThreatProfile resolved = provider.representativeProfile(type, entityTypeId, base);
                if (resolved != null) {
                    return resolved;
                }
            } catch (RuntimeException ignored) {
                // Keep unknown/changed third-party versions on the vanilla fallback instead of crashing.
            }
        }
        return base;
    }

    private static List<CreatureThreatProvider> snapshotMatching(ResourceLocation entityTypeId) {
        List<CreatureThreatProvider> result = new ArrayList<>();
        for (CreatureThreatProvider provider : PROVIDERS) {
            try {
                if (provider.supports(entityTypeId)) {
                    result.add(provider);
                }
            } catch (RuntimeException ignored) {
                // Ignore only the faulty optional provider.
            }
        }
        return result;
    }
}
