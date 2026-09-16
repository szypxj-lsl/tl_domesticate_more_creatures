package com.szypxj.tldomesticatemorecreatures.domestication;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

public final class DomesticationData {
    private static final String ROOT_KEY = "tl_domesticate_more_creatures_domestication";
    private final CompoundTag root;

    private DomesticationData(CompoundTag root) {
        this.root = root;
    }

    public static DomesticationData of(LivingEntity entity) {
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_KEY, new CompoundTag());
        }
        return new DomesticationData(persistent.getCompound(ROOT_KEY));
    }

    public boolean customTamed() {
        return root.getBoolean("customTamed") && root.hasUUID("ownerUuid");
    }

    public void customTamed(boolean value) {
        root.putBoolean("customTamed", value);
    }

    public Optional<UUID> ownerUuid() {
        return root.hasUUID("ownerUuid") ? Optional.of(root.getUUID("ownerUuid")) : Optional.empty();
    }

    public void ownerUuid(UUID value) {
        if (value == null) {
            root.remove("ownerUuid");
        } else {
            root.putUUID("ownerUuid", value);
        }
    }

    public Optional<UUID> tamingPlayerUuid() {
        return root.hasUUID("tamingPlayerUuid") ? Optional.of(root.getUUID("tamingPlayerUuid")) : Optional.empty();
    }

    public void tamingPlayerUuid(UUID value) {
        if (value == null) {
            root.remove("tamingPlayerUuid");
        } else {
            root.putUUID("tamingPlayerUuid", value);
        }
    }

    public double tamingProgress() {
        return Math.max(0.0D, Math.min(1.0D, root.getDouble("tamingProgress")));
    }

    public void tamingProgress(double value) {
        root.putDouble("tamingProgress", Math.max(0.0D, Math.min(1.0D, value)));
    }

    public double knockoutDamage() {
        return Math.max(0.0D, root.getDouble("knockoutDamage"));
    }

    public void knockoutDamage(double value) {
        root.putDouble("knockoutDamage", Math.max(0.0D, value));
    }

    public boolean allowOtherRiders() {
        return root.getBoolean("tdmcAllowOtherRiders");
    }

    public void allowOtherRiders(boolean value) {
        root.putBoolean("tdmcAllowOtherRiders", value);
    }

    public double knockoutMaxHealth() {
        return Math.max(0.0D, root.getDouble("knockoutMaxHealth"));
    }

    public void knockoutMaxHealth(double value) {
        root.putDouble("knockoutMaxHealth", Math.max(0.0D, value));
    }

    public void resetTamingSession() {
        tamingPlayerUuid(null);
        tamingProgress(0.0D);
        knockoutDamage(0.0D);
        knockoutMaxHealth(0.0D);
    }
}
