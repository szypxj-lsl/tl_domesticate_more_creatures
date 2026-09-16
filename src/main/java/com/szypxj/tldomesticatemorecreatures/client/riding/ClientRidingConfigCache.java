package com.szypxj.tldomesticatemorecreatures.client.riding;

import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingConfigSnapshot;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingSettings;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientRidingConfigCache {
    private static final Map<ResourceLocation, EntityRideProfile> modifiedProfiles = new ConcurrentHashMap<>();
    private static volatile RidingSettings settings = RidingSettings.defaults();
    private static volatile Set<ResourceLocation> configurableEntityIds = Set.of();
    private static volatile long generation = -1L;

    private ClientRidingConfigCache() {
    }

    public static synchronized void applySnapshot(RidingConfigSnapshot snapshot) {
        if (snapshot == null || snapshot.generation() < generation) {
            return;
        }
        generation = snapshot.generation();
        settings = snapshot.settings().validated();
        modifiedProfiles.clear();
        modifiedProfiles.putAll(snapshot.modifiedProfiles());
        configurableEntityIds = Set.copyOf(snapshot.configurableEntityIds());
    }

    public static synchronized void applyProfile(long newGeneration, EntityRideProfile profile) {
        if (profile == null || newGeneration < generation) {
            return;
        }
        generation = newGeneration;
        modifiedProfiles.put(profile.entityId(), profile.validated());
    }

    public static EntityRideProfile profile(ResourceLocation entityId) {
        if (entityId == null) {
            throw new IllegalArgumentException("entityId");
        }
        return modifiedProfiles.getOrDefault(entityId, EntityRideProfile.defaults(entityId));
    }

    public static RidingSettings settings() {
        return settings;
    }

    public static Map<ResourceLocation, EntityRideProfile> modifiedProfiles() {
        return Map.copyOf(modifiedProfiles);
    }

    public static boolean hasModifiedProfile(ResourceLocation entityId) {
        return entityId != null && modifiedProfiles.containsKey(entityId);
    }

    public static Set<ResourceLocation> configurableEntityIds() {
        return configurableEntityIds;
    }

    public static long generation() {
        return generation;
    }

    public static synchronized void clear() {
        settings = RidingSettings.defaults();
        modifiedProfiles.clear();
        configurableEntityIds = Set.of();
        generation = -1L;
    }
}
