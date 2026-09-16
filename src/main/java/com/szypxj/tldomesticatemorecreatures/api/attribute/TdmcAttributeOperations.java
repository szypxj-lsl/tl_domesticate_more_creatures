package com.szypxj.tldomesticatemorecreatures.api.attribute;

import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.OptionalDouble;

public final class TdmcAttributeOperations {
    private static final double EPSILON = 0.0000001D;

    private TdmcAttributeOperations() {
    }

    public static OptionalDouble get(LivingEntity entity, ResourceLocation id) {
        Optional<TdmcAttributeValue> value = value(entity, id);
        return value.isEmpty() ? OptionalDouble.empty() : OptionalDouble.of(value.get().current());
    }

    public static Optional<TdmcAttributeValue> value(LivingEntity entity, ResourceLocation id) {
        Optional<TdmcAttributeDefinition> optional = TdmcAttributeRegistry.get(id);
        if (entity == null || optional.isEmpty()) {
            return Optional.empty();
        }
        TdmcAttributeDefinition definition = optional.get();
        if (!definition.appliesTo(entity)) {
            return Optional.empty();
        }

        TdmcAttributeValueProvider provider = definition.valueProvider();
        if (provider != null) {
            TdmcAttributeValue provided = provider.read(entity);
            if (provided == null) {
                return Optional.of(new TdmcAttributeValue(definition.defaultValue(), definition.maxValue()));
            }
            double current = clamp(provided.current(), definition.minValue(), effectiveMax(definition, provided.max()));
            double max = effectiveMax(definition, provided.max());
            return Optional.of(new TdmcAttributeValue(current, max));
        }

        double current = TdmcAttributeValueStore.get(entity, id, definition.defaultValue());
        return Optional.of(new TdmcAttributeValue(
                clamp(current, definition.minValue(), definition.maxValue()),
                definition.maxValue()
        ));
    }

    public static boolean set(LivingEntity entity, ResourceLocation id, double value) {
        Optional<TdmcAttributeDefinition> optional = TdmcAttributeRegistry.get(id);
        if (entity == null || optional.isEmpty()) {
            return false;
        }
        TdmcAttributeDefinition definition = optional.get();
        if (!definition.appliesTo(entity) || !Double.isFinite(value)) {
            return false;
        }

        double max = value(entity, id).map(TdmcAttributeValue::max).orElse(definition.maxValue());
        double clamped = clamp(value, definition.minValue(), effectiveMax(definition, max));
        if (definition.valueProvider() != null) {
            TdmcAttributeValueWriter writer = definition.valueWriter();
            return writer != null && writer.write(entity, clamped);
        }

        TdmcAttributeValueStore.set(entity, id, clamped);
        return true;
    }

    public static boolean add(LivingEntity entity, ResourceLocation id, double amount) {
        if (!Double.isFinite(amount)) {
            return false;
        }
        OptionalDouble current = get(entity, id);
        return current.isPresent() && set(entity, id, current.getAsDouble() + amount);
    }

    public static boolean subtract(LivingEntity entity, ResourceLocation id, double amount) {
        if (!Double.isFinite(amount)) {
            return false;
        }
        return add(entity, id, -amount);
    }

    public static boolean remove(LivingEntity entity, ResourceLocation id) {
        Optional<TdmcAttributeDefinition> optional = TdmcAttributeRegistry.get(id);
        if (entity == null || id == null) {
            return false;
        }
        if (optional.isPresent() && optional.get().valueProvider() != null) {
            TdmcAttributeValueWriter writer = optional.get().valueWriter();
            if (writer == null || !writer.write(entity, optional.get().defaultValue())) {
                return false;
            }
        }
        TdmcAttributeValueStore.remove(entity, id);
        return true;
    }

    public static boolean canAllocate(LivingEntity entity, ResourceLocation id, int pointCount) {
        if (entity == null || id == null || pointCount <= 0 || pointCount > 100) {
            return false;
        }
        Optional<TdmcAttributeDefinition> optional = TdmcAttributeRegistry.get(id);
        if (optional.isEmpty()) {
            return false;
        }
        TdmcAttributeDefinition definition = optional.get();
        if (!definition.flags().upgradeable() || !definition.appliesTo(entity)) {
            return false;
        }
        if (definition.valueProvider() != null && definition.valueWriter() == null) {
            return false;
        }

        ProgressData data = ProgressData.of(entity);
        long cost = (long) definition.pointCost() * pointCount;
        if (cost > data.unspentPoints()) {
            return false;
        }

        Optional<TdmcAttributeValue> value = value(entity, id);
        if (value.isEmpty()) {
            return false;
        }
        double next = value.get().current() + definition.pointIncrement() * pointCount;
        double max = effectiveMax(definition, value.get().max());
        return Double.isFinite(next) && next <= max + EPSILON;
    }

    public static boolean allocatePoints(ServerPlayer actor, LivingEntity entity, ResourceLocation id, int pointCount) {
        if (actor == null || !canAllocate(entity, id, pointCount)) {
            return false;
        }
        TdmcAttributeDefinition definition = TdmcAttributeRegistry.get(id).orElse(null);
        if (definition == null) {
            return false;
        }
        OptionalDouble current = get(entity, id);
        if (current.isEmpty()) {
            return false;
        }

        double next = current.getAsDouble() + definition.pointIncrement() * pointCount;
        if (!set(entity, id, next)) {
            return false;
        }

        ProgressData data = ProgressData.of(entity);
        int cost = Math.toIntExact((long) definition.pointCost() * pointCount);
        data.unspentPoints(data.unspentPoints() - cost);
        long allocated = (long) TdmcAttributeValueStore.allocatedPoints(entity, id) + pointCount;
        TdmcAttributeValueStore.allocatedPoints(entity, id, allocated >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) allocated);
        return true;
    }

    private static double effectiveMax(TdmcAttributeDefinition definition, double providerMax) {
        if (!Double.isFinite(providerMax) || providerMax <= definition.minValue()) {
            return definition.maxValue();
        }
        return Math.min(definition.maxValue(), providerMax);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
