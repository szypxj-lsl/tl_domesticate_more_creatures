package com.szypxj.tldomesticatemorecreatures.api.creature;

import com.szypxj.tldomesticatemorecreatures.game.BaseAttributeSnapshotService;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import com.szypxj.tldomesticatemorecreatures.elite.EliteService;
import com.szypxj.tldomesticatemorecreatures.game.DangerRatingStatsService;
import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CreatureThreatProfile;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingFoodDisplay;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingRule;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingRuleManager;
import com.szypxj.tldomesticatemorecreatures.riding.NativeRideCapabilityResolver;
import com.szypxj.tldomesticatemorecreatures.riding.RideMode;
import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingConfigManager;
import com.szypxj.tldomesticatemorecreatures.spyglass.SpyglassRadarBaseline;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraftforge.registries.ForgeRegistries;

public final class CreatureInfoApi {
    public static final int DANGER_MIN_STARS = 1;
    public static final int DANGER_MAX_STARS = 9;

    private static final double DANGER_POWER_WEIGHT = 0.50D;
    private static final double DANGER_LIFE_WEIGHT = 0.35D;
    private static final double DANGER_SPEED_WEIGHT = 0.15D;

    private CreatureInfoApi() {
    }

    public static ResourceLocation getEntityId(EntityType<?> type) {
        return type == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(type);
    }

    public static BaseStats getBaseStats(LivingEntity entity) {
        return BaseAttributeSnapshotService.get(entity);
    }

    public static BaseStats getBaseStats(EntityType<?> type) {
        if (type == null || type == EntityType.PLAYER || !DefaultAttributes.hasSupplier(type)) {
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

    public static BaseStats getDangerRatingStats(LivingEntity entity) {
        return DangerRatingStatsService.get(entity);
    }

    public static BaseStats getDangerRatingStats(EntityType<?> type) {
        return DangerRatingStatsService.get(type);
    }

    /** Current-instance, stage-aware threat profile used by live TDMC scans. */
    public static CreatureThreatProfile getThreatProfile(LivingEntity entity) {
        return DangerRatingStatsService.getProfile(entity);
    }

    /** Species-level representative profile used by encyclopedic consumers such as TCB. */
    public static CreatureThreatProfile getRepresentativeThreatProfile(EntityType<?> type) {
        return DangerRatingStatsService.getRepresentativeProfile(type);
    }

    public static void refreshDangerRatingData() {
        BaseAttributeSnapshotService.clearTypeCache();
        SpyglassRadarBaseline.refresh();
    }

    public static int dangerPowerPercentile(double value) {
        return SpyglassRadarBaseline.powerPercentile(value);
    }

    public static int dangerLifePercentile(double value) {
        return SpyglassRadarBaseline.lifePercentile(value);
    }

    public static int dangerSpeedPercentile(double value) {
        return SpyglassRadarBaseline.speedPercentile(value);
    }

    public static int dangerStars(LivingEntity entity) {
        if (entity == null) {
            return DANGER_MIN_STARS;
        }
        return dangerStars(getDangerRatingStats(entity), EliteService.isDisplayElite(entity));
    }

    public static int dangerStars(BaseStats stats) {
        return dangerStars(stats, false);
    }

    public static int dangerStars(BaseStats stats, boolean elite) {
        if (stats == null) {
            return DANGER_MIN_STARS;
        }
        return dangerStarsFromRadarScores(
                dangerPowerPercentile(stats.attackDamage()),
                dangerLifePercentile(stats.maxHealth()),
                dangerSpeedPercentile(stats.movementSpeed()),
                elite
        );
    }

    public static int dangerStarsFromRadarScores(int power, int life, int speed) {
        return dangerStarsFromRadarScores(power, life, speed, false);
    }

    public static int dangerStarsFromRadarScores(int power, int life, int speed, boolean elite) {
        double score = clampDangerScore(power) * DANGER_POWER_WEIGHT
                + clampDangerScore(life) * DANGER_LIFE_WEIGHT
                + clampDangerScore(speed) * DANGER_SPEED_WEIGHT;
        int stars;
        if (score < 20.0D) {
            stars = DANGER_MIN_STARS;
        } else {
            stars = (int) Math.floor(score / 10.0D);
            stars = Math.max(DANGER_MIN_STARS, Math.min(DANGER_MAX_STARS, stars));
        }
        return elite ? Math.min(DANGER_MAX_STARS, stars + 1) : stars;
    }

    private static double clampDangerScore(int value) {
        return Math.max(0, Math.min(100, value));
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
