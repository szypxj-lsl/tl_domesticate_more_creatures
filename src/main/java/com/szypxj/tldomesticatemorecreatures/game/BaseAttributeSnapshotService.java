package com.szypxj.tldomesticatemorecreatures.game;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStats;
import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStatsSource;
import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BaseAttributeSnapshotService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ROOT_KEY = "tdmc_base_attributes_v1";
    private static final String MAX_HEALTH_KEY = "maxHealth";
    private static final String ATTACK_DAMAGE_KEY = "attackDamage";
    private static final String MOVEMENT_SPEED_KEY = "movementSpeed";
    private static final Map<ResourceLocation, BaseStats> TYPE_CACHE = new ConcurrentHashMap<>();

    private BaseAttributeSnapshotService() {
    }

    public static BaseStats captureIfAbsent(LivingEntity entity) {
        if (entity == null) {
            return BaseStats.NONE;
        }
        BaseStats existing = read(entity);
        if (existing != null) {
            cache(entity.getType(), existing);
            return existing;
        }

        boolean legacy = ProgressData.exists(entity);
        if (legacy) {
            AttributeService.removeTdmcManagedModifiersForSnapshot(entity);
        }

        BaseStats stats = fromInstanceBaseValues(entity, BaseStatsSource.INSTANCE_SNAPSHOT);
        if (stats.maxHealth() <= 0.0D) {
            BaseStats fallback = defaultStats(entity.getType());
            if (fallback != BaseStats.NONE) {
                stats = fallback;
                LOGGER.warn("[{}] 无法从实体实例读取有效基础生命值，已回退默认属性: {}", TlDomesticateMoreCreatures.MOD_ID, entity.getType());
            }
        }
        write(entity, stats);
        cache(entity.getType(), stats);

        if (legacy && LevelService.isAffected(entity)) {
            AttributeService.applyAll(entity);
        }
        return stats;
    }

    public static BaseStats get(LivingEntity entity) {
        return entity == null ? BaseStats.NONE : captureIfAbsent(entity);
    }

    public static BaseStats get(EntityType<?> type) {
        if (type == null || type == EntityType.PLAYER) {
            return BaseStats.NONE;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id != null) {
            BaseStats cached = TYPE_CACHE.get(id);
            if (cached != null) {
                return cached;
            }
        }
        BaseStats fallback = defaultStats(type);
        cache(type, fallback);
        return fallback;
    }

    public static void clearTypeCache() {
        TYPE_CACHE.clear();
    }

    private static BaseStats read(LivingEntity entity) {
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag root = persistent.getCompound(ROOT_KEY);
        return new BaseStats(
                root.getDouble(MAX_HEALTH_KEY),
                root.getDouble(ATTACK_DAMAGE_KEY),
                root.getDouble(MOVEMENT_SPEED_KEY),
                BaseStatsSource.INSTANCE_SNAPSHOT
        );
    }

    private static void write(LivingEntity entity, BaseStats stats) {
        CompoundTag root = new CompoundTag();
        root.putDouble(MAX_HEALTH_KEY, stats.maxHealth());
        root.putDouble(ATTACK_DAMAGE_KEY, stats.attackDamage());
        root.putDouble(MOVEMENT_SPEED_KEY, stats.movementSpeed());
        entity.getPersistentData().put(ROOT_KEY, root);
    }

    private static BaseStats fromInstanceBaseValues(LivingEntity entity, BaseStatsSource source) {
        return new BaseStats(
                baseValue(entity.getAttribute(Attributes.MAX_HEALTH)),
                baseValue(entity.getAttribute(Attributes.ATTACK_DAMAGE)),
                baseValue(entity.getAttribute(Attributes.MOVEMENT_SPEED)),
                source
        );
    }

    private static double baseValue(AttributeInstance instance) {
        return instance == null ? 0.0D : instance.getBaseValue();
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

    private static void cache(EntityType<?> type, BaseStats stats) {
        if (type == null || stats == null || stats == BaseStats.NONE) {
            return;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id != null) {
            TYPE_CACHE.compute(id, (key, existing) -> {
                if (existing == null) {
                    return stats;
                }
                if (stats.source() == BaseStatsSource.INSTANCE_SNAPSHOT
                        && existing.source() != BaseStatsSource.INSTANCE_SNAPSHOT) {
                    return stats;
                }
                return existing;
            });
        }
    }
}
