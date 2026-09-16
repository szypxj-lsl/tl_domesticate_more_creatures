package com.szypxj.tldomesticatemorecreatures.riding.config;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

public record RidingSettings(RideFilterMode filterMode, Set<ResourceLocation> filterEntries) {
    public RidingSettings {
        filterMode = filterMode == null ? RideFilterMode.BLACKLIST : filterMode;
        filterEntries = filterEntries == null ? Set.of() : Set.copyOf(filterEntries);
    }

    public static RidingSettings defaults() {
        return new RidingSettings(RideFilterMode.BLACKLIST, Set.of());
    }

    public RidingSettings validated() {
        LinkedHashSet<ResourceLocation> clean = new LinkedHashSet<>();
        for (ResourceLocation id : filterEntries) {
            if (id != null) {
                clean.add(id);
            }
        }
        return new RidingSettings(filterMode == null ? RideFilterMode.BLACKLIST : filterMode, clean);
    }

    public boolean allows(ResourceLocation entityId) {
        boolean listed = filterEntries.contains(entityId);
        return filterMode == RideFilterMode.WHITELIST ? listed : !listed;
    }
}
