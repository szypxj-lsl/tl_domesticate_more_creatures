package com.szypxj.tldomesticatemorecreatures.api.attribute;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DynamicAttributeDisplayRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, DynamicAttributeDisplayProvider> PROVIDERS = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> WARNED_FAILURES = ConcurrentHashMap.newKeySet();

    private DynamicAttributeDisplayRegistry() {
    }

    public static void register(ResourceLocation attributeId, DynamicAttributeDisplayProvider provider) {
        Objects.requireNonNull(attributeId, "attributeId");
        Objects.requireNonNull(provider, "provider");
        PROVIDERS.put(attributeId, provider);
        WARNED_FAILURES.remove(attributeId);
    }

    public static void unregister(ResourceLocation attributeId) {
        if (attributeId == null) {
            return;
        }
        PROVIDERS.remove(attributeId);
        WARNED_FAILURES.remove(attributeId);
    }

    public static Optional<DynamicAttributeDisplayValue> resolve(
            ResourceLocation attributeId,
            ServerPlayer viewer,
            LivingEntity target
    ) {
        if (attributeId == null || viewer == null || target == null) {
            return Optional.empty();
        }
        DynamicAttributeDisplayProvider provider = PROVIDERS.get(attributeId);
        if (provider == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(provider.get(viewer, target));
        } catch (RuntimeException exception) {
            if (WARNED_FAILURES.add(attributeId)) {
                LOGGER.warn("Dynamic attribute display provider {} failed. Falling back to the normal TDMC display.", attributeId, exception);
            }
            return Optional.empty();
        }
    }
}
