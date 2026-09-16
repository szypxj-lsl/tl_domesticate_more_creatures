package com.szypxj.tldomesticatemorecreatures.riding.config;

import com.mojang.logging.LogUtils;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingRuleManager;
import com.szypxj.tldomesticatemorecreatures.riding.NativeRideCapabilityResolver;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.LinkedHashSet;
import java.util.Set;

public final class RidingConfigScope {
    private static final Logger LOGGER = LogUtils.getLogger();

    private RidingConfigScope() {
    }

    public static boolean isConfigurable(ServerLevel level, ResourceLocation entityId) {
        if (level == null || entityId == null || TamingRuleManager.ruleFor(entityId).isEmpty()) {
            return false;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (type == null || type == EntityType.PLAYER) {
            return false;
        }
        try {
            Entity created = type.create(level);
            if (!(created instanceof LivingEntity living)) {
                return false;
            }
            return !NativeRideCapabilityResolver.hasNativePlayerControl(living);
        } catch (RuntimeException exception) {
            LOGGER.warn("Unable to inspect riding scope for {}. The entity will not be exposed to the TDMC riding editor.", entityId, exception);
            return false;
        }
    }

    public static Set<ResourceLocation> configurableEntityIds(ServerLevel level) {
        if (level == null) {
            return Set.of();
        }
        LinkedHashSet<ResourceLocation> result = new LinkedHashSet<>();
        for (ResourceLocation entityId : TamingRuleManager.all().keySet()) {
            if (isConfigurable(level, entityId)) {
                result.add(entityId);
            }
        }
        return Set.copyOf(result);
    }
}
