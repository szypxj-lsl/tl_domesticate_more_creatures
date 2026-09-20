package com.szypxj.tldomesticatemorecreatures.api.creature.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Registry for optional third-party metadata adapters. */
public final class CreatureCompatProfileRegistry {
    private static final List<CreatureCompatProfileProvider> PROVIDERS = new ArrayList<>();

    private CreatureCompatProfileRegistry() {
    }

    public static synchronized void register(CreatureCompatProfileProvider provider) {
        Objects.requireNonNull(provider, "provider");
        if (PROVIDERS.stream().anyMatch(existing -> existing.id().equals(provider.id()))) {
            return;
        }
        PROVIDERS.add(provider);
        PROVIDERS.sort(Comparator.comparingInt(CreatureCompatProfileProvider::priority).reversed());
    }

    public static ResourceLocation canonicalEntityTypeId(ResourceLocation entityTypeId) {
        if (entityTypeId == null) {
            return null;
        }
        for (CreatureCompatProfileProvider provider : snapshot()) {
            try {
                if (!provider.supports(entityTypeId)) {
                    continue;
                }
                ResourceLocation canonical = provider.canonicalEntityTypeId(entityTypeId);
                return canonical == null ? entityTypeId : canonical;
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        return entityTypeId;
    }

    public static CreatureCompatProfile resolve(
            ResourceLocation entityTypeId,
            EntityType<?> entityType,
            LivingEntity liveEntity,
            ServerLevel level,
            String languageCode
    ) {
        if (entityTypeId == null) {
            return CreatureCompatProfile.EMPTY;
        }
        CreatureCompatProfileContext context = new CreatureCompatProfileContext(
                level,
                entityTypeId,
                entityType,
                liveEntity,
                languageCode
        );
        for (CreatureCompatProfileProvider provider : snapshot()) {
            try {
                if (!provider.supports(entityTypeId)) {
                    continue;
                }
                CreatureCompatProfile profile = provider.resolve(context);
                if (profile != null) {
                    ResourceLocation canonical = profile.canonicalEntityTypeId();
                    if (canonical == null) {
                        canonical = provider.canonicalEntityTypeId(entityTypeId);
                    }
                    CreatureCompatProfile normalized = new CreatureCompatProfile(
                            canonical,
                            profile.representativeEntityTypeId(),
                            profile.diet(),
                            profile.species(),
                            profile.description(),
                            profile.nativeTamingInfo()
                    ).withIdentityDefaults(entityTypeId);
                    return normalized;
                }
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        ResourceLocation canonical = canonicalEntityTypeId(entityTypeId);
        return new CreatureCompatProfile(
                canonical,
                canonical,
                CreatureDiet.UNKNOWN,
                net.minecraft.network.chat.Component.empty(),
                net.minecraft.network.chat.Component.empty(),
                com.szypxj.tldomesticatemorecreatures.api.creature.TamingInfo.NOT_TAMEABLE
        );
    }

    private static synchronized List<CreatureCompatProfileProvider> snapshot() {
        return List.copyOf(PROVIDERS);
    }
}
