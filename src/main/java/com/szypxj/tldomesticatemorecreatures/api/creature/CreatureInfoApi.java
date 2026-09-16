package com.szypxj.tldomesticatemorecreatures.api.creature;

import com.szypxj.tldomesticatemorecreatures.game.BaseAttributeSnapshotService;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingFoodDisplay;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingRule;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingRuleManager;
import com.szypxj.tldomesticatemorecreatures.riding.NativeRideCapabilityResolver;
import com.szypxj.tldomesticatemorecreatures.riding.RideMode;
import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingConfigManager;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public final class CreatureInfoApi {
    private CreatureInfoApi() {
    }

    public static ResourceLocation getEntityId(EntityType<?> type) {
        return type == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(type);
    }

    public static BaseStats getBaseStats(LivingEntity entity) {
        return BaseAttributeSnapshotService.get(entity);
    }

    public static BaseStats getBaseStats(EntityType<?> type) {
        return BaseAttributeSnapshotService.get(type);
    }

    public static boolean isTdmcAffected(EntityType<?> type) {
        ResourceLocation id = getEntityId(type);
        return id != null && LevelService.isAffectedEntityId(id.toString());
    }
    public static TamingInfo getTamingInfo(LivingEntity entity) {
        if (entity == null) {
            return TamingInfo.NOT_TAMEABLE;
        }
        TamingRule rule = TamingRuleManager.ruleFor(entity).orElse(null);
        if (rule == null) {
            return TamingInfo.NOT_TAMEABLE;
        }
        List<TamingFoodInfo> foods = TamingRuleManager.displayFoodsFor(entity).stream()
                .map(CreatureInfoApi::toFoodInfo)
                .toList();
        return new TamingInfo(true, rule.method().name(), rule.requiredPlayerLevel(), foods);
    }

    public static TamingInfo getTamingInfo(ServerLevel level, EntityType<?> type) {
        TamingRule rule = TamingRuleManager.ruleFor(type).orElse(null);
        if (rule == null || level == null) {
            return TamingInfo.NOT_TAMEABLE;
        }
        List<TamingFoodInfo> foods = TamingRuleManager.displayFoodsFor(level, type).stream()
                .map(CreatureInfoApi::toFoodInfo)
                .toList();
        return new TamingInfo(true, rule.method().name(), rule.requiredPlayerLevel(), foods);
    }

    public static boolean isRideable(LivingEntity entity) {
        if (entity == null || entity instanceof net.minecraft.world.entity.player.Player) {
            return false;
        }
        if (NativeRideCapabilityResolver.hasNativePlayerControl(entity)) {
            return true;
        }
        ResourceLocation entityId = getEntityId(entity.getType());
        if (entityId == null || TamingRuleManager.ruleFor(entityId).isEmpty()) {
            return false;
        }
        EntityRideProfile profile = RidingConfigManager.profile(entityId);
        return profile.mode() != RideMode.DISABLED
                && (RidingConfigManager.settings().allows(entityId) || profile.mode() == RideMode.FORCE_GENERIC);
    }

    public static boolean isRideable(ServerLevel level, EntityType<?> type) {
        if (level == null || type == null || type == EntityType.PLAYER) {
            return false;
        }
        try {
            Entity created = type.create(level);
            return created instanceof LivingEntity living && isRideable(living);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static TamingFoodInfo toFoodInfo(TamingFoodDisplay food) {
        return new TamingFoodInfo(food.itemId(), Math.max(0, food.amount()), food.configured());
    }

}
