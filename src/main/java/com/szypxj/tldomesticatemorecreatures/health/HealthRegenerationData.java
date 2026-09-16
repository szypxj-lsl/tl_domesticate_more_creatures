package com.szypxj.tldomesticatemorecreatures.health;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;

public final class HealthRegenerationData {
    private static final String ROOT_KEY = "tl_domesticate_more_creatures_health_regeneration";
    private final CompoundTag root;

    private HealthRegenerationData(CompoundTag root) {
        this.root = root;
    }

    public static boolean exists(LivingEntity entity) {
        return entity.getPersistentData().contains(ROOT_KEY, Tag.TAG_COMPOUND);
    }

    public static HealthRegenerationData of(LivingEntity entity) {
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_KEY, new CompoundTag());
        }
        return new HealthRegenerationData(persistent.getCompound(ROOT_KEY));
    }

    public long nextHealGameTime() {
        return Math.max(0L, root.getLong("nextHealGameTime"));
    }

    public void nextHealGameTime(long value) {
        root.putLong("nextHealGameTime", Math.max(0L, value));
    }

    public void clear() {
        nextHealGameTime(0L);
    }
}
