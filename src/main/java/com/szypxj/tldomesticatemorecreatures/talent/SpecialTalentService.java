package com.szypxj.tldomesticatemorecreatures.talent;

import com.szypxj.tldomesticatemorecreatures.config.SpecialTalentConfigManager;
import com.szypxj.tldomesticatemorecreatures.config.SpecialTalentDefinition;
import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import com.szypxj.tldomesticatemorecreatures.talent.active.ActiveTalentRegistry;
import com.szypxj.tldomesticatemorecreatures.talent.active.ActiveTalentService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashSet;
import java.util.Set;

public final class SpecialTalentService {
    private SpecialTalentService() {
    }

    public static void initializeWildTalents(LivingEntity entity, ProgressData data, RandomSource random) {
        if (entity == null || data == null || !LevelService.isAffected(entity)) {
            return;
        }
        if (entity instanceof Player) {
            data.clearSpecialTalents();
            data.specialTalentsInitialized(true);
            return;
        }
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null) {
            data.clearSpecialTalents();
            data.specialTalentsInitialized(true);
            return;
        }
        RandomSource actualRandom = random == null ? entity.getRandom() : random;
        Set<String> selected = new LinkedHashSet<>();
        for (SpecialTalentDefinition definition : SpecialTalentConfigManager.all()) {
            if (definition.allows(entityId) && actualRandom.nextDouble() < definition.spawnChance()) {
                selected.add(definition.id());
            }
        }
        data.specialTalents(ActiveTalentRegistry.normalizeToSingleActive(entity.getUUID(), selected));
        data.specialTalentsInitialized(true);
        refreshRuntime(entity);
    }

    public static void ensureTalents(LivingEntity entity) {
        if (entity == null || !LevelService.isAffected(entity) || !ProgressData.exists(entity)) {
            return;
        }
        ProgressData data = ProgressData.of(entity);
        if (entity instanceof Player) {
            if (!data.specialTalents().isEmpty()) {
                data.clearSpecialTalents();
            }
            data.specialTalentsInitialized(true);
            return;
        }
        if (!data.specialTalentsInitialized()) {
            initializeWildTalents(entity, data, entity.getRandom());
            return;
        }
        refreshRuntime(entity);
    }

    public static Set<String> resolveInheritedTalents(
            String childEntityId,
            Set<String> parentATalents,
            Set<String> parentBTalents,
            RandomSource random
    ) {
        ResourceLocation childId = ResourceLocation.tryParse(childEntityId == null ? "" : childEntityId.trim());
        if (childId == null) {
            return Set.of();
        }
        Set<String> a = parentATalents == null ? Set.of() : parentATalents;
        Set<String> b = parentBTalents == null ? Set.of() : parentBTalents;
        RandomSource actualRandom = random == null ? RandomSource.create() : random;
        Set<String> selected = new LinkedHashSet<>();
        for (SpecialTalentDefinition definition : SpecialTalentConfigManager.all()) {
            if (!definition.allows(childId)) {
                continue;
            }
            boolean hasA = a.contains(definition.id());
            boolean hasB = b.contains(definition.id());
            if (!hasA && !hasB) {
                continue;
            }
            double chance = hasA && hasB
                    ? definition.inheritBothParentsChance()
                    : definition.inheritOneParentChance();
            if (actualRandom.nextDouble() < chance) {
                selected.add(definition.id());
            }
        }
        return ActiveTalentRegistry.normalizeToSingleActive(actualRandom, selected);
    }

    public static boolean has(LivingEntity entity, String id) {
        return entity != null
                && id != null
                && ProgressData.exists(entity)
                && ProgressData.of(entity).hasSpecialTalent(id);
    }

    public static boolean hasActive(LivingEntity entity, String id) {
        if (!has(entity, id)) {
            return false;
        }
        SpecialTalentDefinition definition = SpecialTalentConfigManager.byId(id);
        return definition != null && definition.enabled();
    }

    public static Set<String> talents(LivingEntity entity) {
        return entity != null && ProgressData.exists(entity)
                ? ProgressData.of(entity).specialTalents()
                : Set.of();
    }

    public static void applyInheritedTalents(LivingEntity child, ProgressData data, Set<String> inherited) {
        if (child == null || data == null) {
            return;
        }
        data.specialTalents(ActiveTalentRegistry.normalizeToSingleActive(child.getUUID(), inherited == null ? Set.of() : inherited));
        data.specialTalentsInitialized(true);
        refreshRuntime(child);
    }

    public static void refreshRuntime(LivingEntity entity) {
        FuryService.refreshRegistration(entity);
        ActiveTalentService.selfHealActiveTalents(entity);
    }
}
