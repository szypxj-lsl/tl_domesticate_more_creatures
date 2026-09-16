package com.szypxj.tldomesticatemorecreatures.riding.config;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;

public record RidingConfigSnapshot(
        long generation,
        RidingSettings settings,
        Map<ResourceLocation, EntityRideProfile> modifiedProfiles,
        Set<ResourceLocation> configurableEntityIds
) {
    public RidingConfigSnapshot {
        settings = settings == null ? RidingSettings.defaults() : settings.validated();
        modifiedProfiles = modifiedProfiles == null ? Map.of() : Map.copyOf(modifiedProfiles);
        configurableEntityIds = configurableEntityIds == null ? Set.of() : Set.copyOf(configurableEntityIds);
    }
}
