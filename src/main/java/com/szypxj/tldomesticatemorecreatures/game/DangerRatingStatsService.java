package com.szypxj.tldomesticatemorecreatures.game;

import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStats;
import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStatsSource;
import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CreatureThreatProfile;
import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CreatureThreatProviderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Danger-only stat resolver. Unlike BaseAttributeSnapshotService this service must never cache one observed
 * instance as the value for an entire EntityType: growth stages and forms can legitimately differ.
 */
public final class DangerRatingStatsService {
    private DangerRatingStatsService() {
    }

    public static BaseStats get(LivingEntity entity) {
        return getProfile(entity).toDangerStats(BaseStatsSource.INSTANCE_SNAPSHOT);
    }

    public static BaseStats get(EntityType<?> type) {
        return getRepresentativeProfile(type).toDangerStats(BaseStatsSource.DEFAULT_ATTRIBUTES);
    }

    public static CreatureThreatProfile getProfile(LivingEntity entity) {
        if (entity == null) {
            return CreatureThreatProfile.NONE;
        }
        CreatureThreatProfile fallback = basicLiveProfile(entity);
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return CreatureThreatProviderRegistry.resolveLive(id, entity, fallback);
    }

    public static CreatureThreatProfile getRepresentativeProfile(EntityType<?> type) {
        if (type == null || type == EntityType.PLAYER) {
            return CreatureThreatProfile.NONE;
        }
        CreatureThreatProfile fallback = basicRepresentativeProfile(type);
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return CreatureThreatProviderRegistry.resolveRepresentative(id, type, fallback);
    }

    /** Kept for binary/source compatibility with older internal callers; observed-type danger caching no longer exists. */
    public static void clearObserved() {
        // Intentionally empty.
    }

    private static CreatureThreatProfile basicLiveProfile(LivingEntity entity) {
        return CreatureThreatProfile.basic(
                valueWithoutTdmc(entity, entity.getAttribute(Attributes.MAX_HEALTH)),
                valueWithoutTdmc(entity, entity.getAttribute(Attributes.ATTACK_DAMAGE)),
                valueWithoutTdmc(entity, entity.getAttribute(Attributes.MOVEMENT_SPEED))
        );
    }

    private static CreatureThreatProfile basicRepresentativeProfile(EntityType<?> type) {
        if (!DefaultAttributes.hasSupplier(type)) {
            return CreatureThreatProfile.NONE;
        }
        @SuppressWarnings("unchecked")
        EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) type;
        AttributeSupplier supplier = DefaultAttributes.getSupplier(livingType);
        if (supplier == null) {
            return CreatureThreatProfile.NONE;
        }
        return CreatureThreatProfile.basic(
                supplier.hasAttribute(Attributes.MAX_HEALTH) ? supplier.getBaseValue(Attributes.MAX_HEALTH) : 0.0D,
                supplier.hasAttribute(Attributes.ATTACK_DAMAGE) ? supplier.getBaseValue(Attributes.ATTACK_DAMAGE) : 0.0D,
                supplier.hasAttribute(Attributes.MOVEMENT_SPEED) ? supplier.getBaseValue(Attributes.MOVEMENT_SPEED) : 0.0D
        );
    }

    private static double valueWithoutTdmc(LivingEntity entity, AttributeInstance instance) {
        if (instance == null) {
            return 0.0D;
        }
        double base = instance.getBaseValue();
        double value = base;
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (!AttributeService.isTdmcManagedModifier(entity, modifier)
                    && modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                value += modifier.getAmount();
            }
        }
        double afterAddition = value;
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (!AttributeService.isTdmcManagedModifier(entity, modifier)
                    && modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE) {
                value += afterAddition * modifier.getAmount();
            }
        }
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (!AttributeService.isTdmcManagedModifier(entity, modifier)
                    && modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) {
                value *= 1.0D + modifier.getAmount();
            }
        }
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }
}
