package com.szypxj.tldomesticatemorecreatures.elite;

import com.szypxj.tldomesticatemorecreatures.api.compat.ErsCompatApi;
import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

public final class EliteService {
    public static final TagKey<EntityType<?>> BOSS_TAG = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.tryBuild("forge", "bosses")
    );

    private EliteService() {
    }

    public static boolean isElite(LivingEntity entity) {
        return entity != null
                && !(entity instanceof Player)
                && LevelService.isAffected(entity)
                && ProgressData.exists(entity)
                && ProgressData.of(entity).elite();
    }

    public static boolean isDisplayElite(LivingEntity entity) {
        return isElite(entity) || ErsCompatApi.isExternalElite(entity);
    }

    public static boolean isBoss(LivingEntity entity) {
        return entity != null
                && !(entity instanceof Player)
                && entity.getType().builtInRegistryHolder().is(BOSS_TAG);
    }

    public static boolean canBeTamed(LivingEntity entity) {
        if (!isElite(entity)) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null && Config.ELITE_TAMEABLE_ENTITIES.get().contains(id.toString());
    }

    public static void clearEliteState(LivingEntity entity) {
        if (entity == null || !ProgressData.exists(entity)) {
            return;
        }
        ProgressData data = ProgressData.of(entity);
        data.elite(false);
        data.eliteDetermined(true);
        entity.setGlowingTag(false);
    }

    public static void initializeFresh(LivingEntity entity, ProgressData data) {
        if (entity instanceof Player || ErsCompatApi.isErsEntity(entity)) {
            data.elite(false);
            data.eliteDetermined(true);
            return;
        }

        boolean elite = isBoss(entity) || (!data.tamed() && shouldRollElite(entity));
        data.elite(elite);
        if (elite && !canBeTamed(entity)) {
            data.tamed(false);
            data.tamingBonusLevels(0);
            data.tamingBonusApplied(false);
        }
        data.eliteDetermined(true);
        ensurePresentation(entity);
    }

    public static void migrateExisting(LivingEntity entity) {
        if (entity instanceof Player || !ProgressData.exists(entity)) {
            return;
        }

        ProgressData data = ProgressData.of(entity);
        if (ErsCompatApi.isErsEntity(entity)) {
            boolean wasTdmcElite = data.elite();
            data.elite(false);
            data.eliteDetermined(true);
            if (wasTdmcElite) {
                entity.setGlowingTag(false);
            }
            return;
        }
        if (!data.eliteDetermined()) {
            data.elite(isBoss(entity));
            if (data.elite() && !canBeTamed(entity)) {
                data.tamed(false);
                data.tamingBonusLevels(0);
                data.tamingBonusApplied(false);
            }
            data.eliteDetermined(true);
        }
        ensurePresentation(entity);
    }

    public static void ensurePresentation(LivingEntity entity) {
        if (isElite(entity)) {
            ProgressData data = ProgressData.of(entity);
            if (!canBeTamed(entity)) {
                data.tamed(false);
            }
            entity.setGlowingTag(true);
        }
    }

    private static boolean shouldRollElite(LivingEntity entity) {
        if (ErsCompatApi.isErsEntity(entity) || !LevelService.isAffected(entity)) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null || !isEligibleByFilter(id)) {
            return false;
        }
        return entity.getRandom().nextDouble() < chanceFor(id);
    }

    private static boolean isEligibleByFilter(ResourceLocation id) {
        String mode = Config.ELITE_FILTER_MODE.get().toUpperCase(Locale.ROOT);
        boolean listed = Config.ELITE_ENTITIES.get().contains(id.toString());
        return "WHITELIST".equals(mode) ? listed : !listed;
    }

    private static double chanceFor(ResourceLocation id) {
        for (String entry : Config.ELITE_ENTITY_CHANCES.get()) {
            if (entry == null) {
                continue;
            }
            int split = entry.lastIndexOf('=');
            if (split <= 0 || split >= entry.length() - 1) {
                continue;
            }
            ResourceLocation configuredId = ResourceLocation.tryParse(entry.substring(0, split).trim());
            if (!id.equals(configuredId)) {
                continue;
            }
            try {
                double value = Double.parseDouble(entry.substring(split + 1).trim());
                if (Double.isFinite(value)) {
                    return Math.max(0.0D, Math.min(1.0D, value));
                }
            } catch (NumberFormatException ignored) {
                return Config.ELITE_SPAWN_CHANCE.get();
            }
        }
        return Config.ELITE_SPAWN_CHANCE.get();
    }
}
