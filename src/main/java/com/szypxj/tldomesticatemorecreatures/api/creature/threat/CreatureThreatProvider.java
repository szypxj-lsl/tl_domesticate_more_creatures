package com.szypxj.tldomesticatemorecreatures.api.creature.threat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Optional compatibility hook for creatures whose real threat cannot be represented by vanilla attributes alone.
 */
public interface CreatureThreatProvider {
    String id();

    default int priority() {
        return 0;
    }

    boolean supports(ResourceLocation entityTypeId);

    default CreatureThreatProfile liveProfile(LivingEntity entity, CreatureThreatProfile fallback) {
        return fallback;
    }

    default CreatureThreatProfile representativeProfile(
            EntityType<?> type,
            ResourceLocation entityTypeId,
            CreatureThreatProfile fallback
    ) {
        return fallback;
    }
}
