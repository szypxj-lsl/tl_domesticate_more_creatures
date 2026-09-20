package com.szypxj.tldomesticatemorecreatures.api.creature.taming;

import com.szypxj.tldomesticatemorecreatures.api.creature.TamingInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/** Optional compatibility hook for native third-party taming systems not described by TDMC rules. */
public interface CreatureTamingInfoProvider {
    String id();

    default int priority() {
        return 0;
    }

    boolean supports(ResourceLocation entityTypeId);

    default TamingInfo liveInfo(LivingEntity entity) {
        return TamingInfo.NOT_TAMEABLE;
    }

    default TamingInfo representativeInfo(ServerLevel level, EntityType<?> type, ResourceLocation entityTypeId) {
        return TamingInfo.NOT_TAMEABLE;
    }
}
