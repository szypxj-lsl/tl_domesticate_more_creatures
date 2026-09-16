package com.szypxj.tldomesticatemorecreatures.api.attribute;

import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TdmcAttributeValueStore {
    private static final String VALUES_KEY = "dynamicAttributes";
    private static final String POINTS_KEY = "dynamicAttributePoints";

    private TdmcAttributeValueStore() {
    }

    public static boolean contains(LivingEntity entity, ResourceLocation id) {
        if (entity == null || id == null) {
            return false;
        }
        CompoundTag values = values(entity);
        return values.contains(id.toString(), Tag.TAG_DOUBLE);
    }

    public static double get(LivingEntity entity, ResourceLocation id, double fallback) {
        if (entity == null || id == null) {
            return fallback;
        }
        CompoundTag values = values(entity);
        String key = id.toString();
        return values.contains(key, Tag.TAG_DOUBLE) ? values.getDouble(key) : fallback;
    }

    public static void set(LivingEntity entity, ResourceLocation id, double value) {
        if (entity == null || id == null || !Double.isFinite(value)) {
            return;
        }
        values(entity).putDouble(id.toString(), value);
    }

    public static void remove(LivingEntity entity, ResourceLocation id) {
        if (entity == null || id == null) {
            return;
        }
        values(entity).remove(id.toString());
        points(entity).remove(id.toString());
    }

    public static int allocatedPoints(LivingEntity entity, ResourceLocation id) {
        if (entity == null || id == null) {
            return 0;
        }
        return Math.max(0, points(entity).getInt(id.toString()));
    }

    public static void allocatedPoints(LivingEntity entity, ResourceLocation id, int value) {
        if (entity == null || id == null) {
            return;
        }
        if (value <= 0) {
            points(entity).remove(id.toString());
        } else {
            points(entity).putInt(id.toString(), value);
        }
    }

    public static Map<ResourceLocation, Double> all(LivingEntity entity) {
        if (entity == null) {
            return Map.of();
        }
        Map<ResourceLocation, Double> result = new LinkedHashMap<>();
        CompoundTag values = values(entity);
        for (String key : values.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null && values.contains(key, Tag.TAG_DOUBLE)) {
                result.put(id, values.getDouble(key));
            }
        }
        return Map.copyOf(result);
    }

    private static CompoundTag values(LivingEntity entity) {
        CompoundTag root = root(entity);
        if (!root.contains(VALUES_KEY, Tag.TAG_COMPOUND)) {
            root.put(VALUES_KEY, new CompoundTag());
        }
        return root.getCompound(VALUES_KEY);
    }

    private static CompoundTag points(LivingEntity entity) {
        CompoundTag root = root(entity);
        if (!root.contains(POINTS_KEY, Tag.TAG_COMPOUND)) {
            root.put(POINTS_KEY, new CompoundTag());
        }
        return root.getCompound(POINTS_KEY);
    }

    private static CompoundTag root(LivingEntity entity) {
        ProgressData.of(entity);
        return entity.getPersistentData().getCompound(ProgressData.ROOT_KEY);
    }
}
