package com.szypxj.tldomesticatemorecreatures.client.riding;

import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingSettings;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

public final class RidingEditorModel {
    private final Map<ResourceLocation, EntityRideProfile> drafts = new LinkedHashMap<>();
    private final Set<ResourceLocation> dirtyProfiles = new LinkedHashSet<>();
    private ResourceLocation selected;
    private EntityRideProfile clipboard;
    private RidingSettings workingSettings = ClientRidingConfigCache.settings();
    private boolean settingsDirty;
    private long savedGeneration = ClientRidingConfigCache.generation();

    public void select(ResourceLocation entityId) {
        if (entityId == null) {
            return;
        }
        selected = entityId;
        drafts.computeIfAbsent(entityId, ClientRidingConfigCache::profile);
    }

    public ResourceLocation selected() {
        return selected;
    }

    public void clearSelection() {
        selected = null;
    }

    public EntityRideProfile workingProfile() {
        if (selected == null) {
            return null;
        }
        return drafts.computeIfAbsent(selected, ClientRidingConfigCache::profile);
    }

    public boolean dirty() {
        return selected != null && dirtyProfiles.contains(selected);
    }

    public boolean isDirty(ResourceLocation entityId) {
        return entityId != null && dirtyProfiles.contains(entityId);
    }

    public Set<ResourceLocation> dirtyProfiles() {
        return Set.copyOf(dirtyProfiles);
    }

    public EntityRideProfile draft(ResourceLocation entityId) {
        if (entityId == null) {
            return null;
        }
        return drafts.computeIfAbsent(entityId, ClientRidingConfigCache::profile);
    }

    public boolean anyDirty() {
        return settingsDirty || !dirtyProfiles.isEmpty();
    }

    public void update(UnaryOperator<EntityRideProfile> edit) {
        EntityRideProfile current = workingProfile();
        if (current == null || edit == null) {
            return;
        }
        EntityRideProfile updated = edit.apply(current);
        if (updated == null) {
            return;
        }
        updated = withEntityId(updated.validated(), selected);
        drafts.put(selected, updated);
        dirtyProfiles.add(selected);
    }

    public void resetCurrent() {
        if (selected == null) {
            return;
        }
        drafts.put(selected, ClientRidingConfigCache.profile(selected));
        dirtyProfiles.remove(selected);
    }

    public void copyCurrent() {
        EntityRideProfile current = workingProfile();
        if (current != null) {
            clipboard = current;
        }
    }

    public boolean canPaste() {
        return clipboard != null && selected != null;
    }

    public void pasteToCurrent() {
        if (!canPaste()) {
            return;
        }
        drafts.put(selected, withEntityId(clipboard, selected));
        dirtyProfiles.add(selected);
    }

    public void markSaved(long generation) {
        if (selected != null) {
            dirtyProfiles.remove(selected);
            drafts.put(selected, ClientRidingConfigCache.profile(selected));
        }
        savedGeneration = Math.max(savedGeneration, generation);
    }

    public void markSaved(ResourceLocation entityId, long generation) {
        if (entityId != null) {
            dirtyProfiles.remove(entityId);
            drafts.put(entityId, ClientRidingConfigCache.profile(entityId));
        }
        savedGeneration = Math.max(savedGeneration, generation);
    }

    public RidingSettings workingSettings() {
        return workingSettings;
    }

    public boolean settingsDirty() {
        return settingsDirty;
    }

    public void updateSettings(UnaryOperator<RidingSettings> edit) {
        if (edit == null) {
            return;
        }
        RidingSettings updated = edit.apply(workingSettings);
        if (updated != null) {
            workingSettings = updated.validated();
            settingsDirty = true;
        }
    }

    public void markSettingsSaved(long generation) {
        workingSettings = ClientRidingConfigCache.settings();
        settingsDirty = false;
        savedGeneration = Math.max(savedGeneration, generation);
    }

    public void refreshCleanDrafts() {
        for (ResourceLocation entityId : Set.copyOf(drafts.keySet())) {
            if (!dirtyProfiles.contains(entityId)) {
                drafts.put(entityId, ClientRidingConfigCache.profile(entityId));
            }
        }
        if (!settingsDirty) {
            workingSettings = ClientRidingConfigCache.settings();
        }
    }

    public void discardAll() {
        drafts.clear();
        dirtyProfiles.clear();
        workingSettings = ClientRidingConfigCache.settings();
        settingsDirty = false;
        if (selected != null) {
            drafts.put(selected, ClientRidingConfigCache.profile(selected));
        }
    }

    public long savedGeneration() {
        return savedGeneration;
    }

    private static EntityRideProfile withEntityId(EntityRideProfile p, ResourceLocation id) {
        return new EntityRideProfile(
                id,
                p.mode(), p.movementMode(),
                p.groundSpeedMultiplier(), p.turnRateDegrees(), p.acceleration(), p.deceleration(), p.jumpStrength(),
                p.flightSpeedMultiplier(), p.ascentSpeed(), p.descentSpeed(), p.swimSpeedMultiplier(),
                p.autoAttackWhileRidden(), p.visualOverride(), p.visual()
        ).validated();
    }
}
