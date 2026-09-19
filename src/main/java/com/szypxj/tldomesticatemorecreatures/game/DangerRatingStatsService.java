package com.szypxj.tldomesticatemorecreatures.game;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStats;
import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStatsSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DangerRatingStatsService {
    private static final Map<ResourceLocation, BaseStats> OBSERVED_TYPE_STATS = new ConcurrentHashMap<>();

    private DangerRatingStatsService() {
    }

    public static BaseStats get(LivingEntity entity) {
        if (entity == null) {
            return BaseStats.NONE;
        }
        BaseStats stats = new BaseStats(
                valueWithoutTdmc(entity, entity.getAttribute(Attributes.MAX_HEALTH)),
                valueWithoutTdmc(entity, entity.getAttribute(Attributes.ATTACK_DAMAGE)),
                valueWithoutTdmc(entity, entity.getAttribute(Attributes.MOVEMENT_SPEED)),
                BaseStatsSource.INSTANCE_SNAPSHOT
        );
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id != null && stats.maxHealth() > 0.0D) {
            OBSERVED_TYPE_STATS.put(id, stats);
        }
        return stats;
    }

    public static BaseStats get(EntityType<?> type) {
        if (type == null || type == EntityType.PLAYER) {
            return BaseStats.NONE;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id != null) {
            BaseStats observed = OBSERVED_TYPE_STATS.get(id);
            if (observed != null) {
                return observed;
            }
        }
        return defaultStats(type);
    }

    public static void clearObserved() {
        OBSERVED_TYPE_STATS.clear();
    }

    private static double valueWithoutTdmc(LivingEntity entity, AttributeInstance instance) {
        if (instance == null) {
            return 0.0D;
        }
        double base = instance.getBaseValue();
        double value = base;
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (!AttributeService.isTdmcManagedModifier(entity, modifier) && modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                value += modifier.getAmount();
            }
        }
        double afterAddition = value;
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (!AttributeService.isTdmcManagedModifier(entity, modifier) && modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE) {
                value += afterAddition * modifier.getAmount();
            }
        }
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (!AttributeService.isTdmcManagedModifier(entity, modifier) && modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) {
                value *= 1.0D + modifier.getAmount();
            }
        }
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    private static BaseStats defaultStats(EntityType<?> type) {
        if (!DefaultAttributes.hasSupplier(type)) {
            return BaseStats.NONE;
        }
        @SuppressWarnings("unchecked")
        EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) type;
        AttributeSupplier supplier = DefaultAttributes.getSupplier(livingType);
        if (supplier == null) {
            return BaseStats.NONE;
        }
        return new BaseStats(
                supplier.hasAttribute(Attributes.MAX_HEALTH) ? supplier.getBaseValue(Attributes.MAX_HEALTH) : 0.0D,
                supplier.hasAttribute(Attributes.ATTACK_DAMAGE) ? supplier.getBaseValue(Attributes.ATTACK_DAMAGE) : 0.0D,
                supplier.hasAttribute(Attributes.MOVEMENT_SPEED) ? supplier.getBaseValue(Attributes.MOVEMENT_SPEED) : 0.0D,
                BaseStatsSource.DEFAULT_ATTRIBUTES
        );
    }
}
