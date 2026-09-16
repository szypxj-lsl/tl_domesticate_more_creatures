package com.szypxj.tldomesticatemorecreatures.game;

import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.config.TalentConfigManager;
import com.szypxj.tldomesticatemorecreatures.config.TalentDefinition;
import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.talent.TalentMigrationService;
import com.szypxj.tldomesticatemorecreatures.elite.EliteMath;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TalentService {
    private static final int WILD_MIN_TALENTS = 1;
    private static final int WILD_MAX_TALENTS = 3;
    private static final int INHERITED_TALENTS = 3;

    private TalentService() {
    }

    public static void initializeWildTalents(LivingEntity entity, ProgressData data, RandomSource random) {
        if (!LevelService.isAffected(entity)) {
            return;
        }
        if (entity instanceof Player) {
            data.clearTalents();
            data.talentsInitialized(true);
            data.talentSchemaVersion(TalentMigrationService.CURRENT_SCHEMA_VERSION);
            applyTalentPoints(entity);
            return;
        }
        List<TalentDefinition> pool = TalentConfigManager.all();
        if (pool.isEmpty()) {
            data.clearTalents();
            data.talentsInitialized(true);
            data.talentSchemaVersion(TalentMigrationService.CURRENT_SCHEMA_VERSION);
            applyTalentPoints(entity);
            return;
        }
        int max = Math.min(WILD_MAX_TALENTS, pool.size());
        int min = Math.min(WILD_MIN_TALENTS, max);
        int count = min >= max ? max : min + random.nextInt(max - min + 1);
        Map<String, Integer> selected = new LinkedHashMap<>();
        Set<String> excluded = new HashSet<>();
        while (selected.size() < count) {
            TalentDefinition definition = TalentConfigManager.weightedRandom(random, excluded);
            if (definition == null) {
                break;
            }
            int rolls = data.elite() ? Config.ELITE_TALENT_LEVEL_ROLLS.get() : 1;
            int level = EliteMath.biasedTalentLevel(
                    definition.minLevel(),
                    definition.maxLevel(),
                    rolls,
                    random::nextInt
            );
            selected.put(definition.id(), level);
            excluded.add(definition.id());
        }
        data.talents(selected);
        data.talentsInitialized(true);
        data.talentSchemaVersion(TalentMigrationService.CURRENT_SCHEMA_VERSION);
        applyTalentPoints(entity);
    }

    public static void ensureTalents(LivingEntity entity) {
        if (!LevelService.isAffected(entity) || !ProgressData.exists(entity)) {
            return;
        }
        ProgressData data = ProgressData.of(entity);
        TalentMigrationService.migrateIfNeeded(entity, data);
        if (entity instanceof Player) {
            if (!data.talents().isEmpty()) {
                data.clearTalents();
            }
            data.talentsInitialized(true);
            applyTalentPoints(entity);
            return;
        }
        if (!data.talentsInitialized()) {
            initializeWildTalents(entity, data, entity.getRandom());
            return;
        }
        applyTalentPoints(entity);
    }

    public static void initializeInheritedTalents(
            LivingEntity child,
            ProgressData childData,
            ProgressData parentA,
            ProgressData parentB,
            boolean mutated,
            RandomSource random
    ) {
        initializeInheritedTalents(child, childData, parentA.talents(), parentB.talents(), mutated, random);
    }

    public static void initializeInheritedTalents(
            LivingEntity child,
            ProgressData childData,
            Map<String, Integer> parentATalents,
            Map<String, Integer> parentBTalents,
            boolean mutated,
            RandomSource random
    ) {
        if (!LevelService.isAffected(child)) {
            return;
        }
        Map<String, Integer> selected = resolveInheritedTalents(parentATalents, parentBTalents, mutated, random);
        childData.talents(selected);
        childData.talentsInitialized(true);
        childData.talentSchemaVersion(TalentMigrationService.CURRENT_SCHEMA_VERSION);
        applyTalentPoints(child);
    }

    public static Map<String, Integer> resolveInheritedTalents(
            Map<String, Integer> parentATalents,
            Map<String, Integer> parentBTalents,
            boolean mutated,
            RandomSource random
    ) {
        Map<String, Integer> aTalents = validTalents(parentATalents == null ? Map.of() : parentATalents);
        Map<String, Integer> bTalents = validTalents(parentBTalents == null ? Map.of() : parentBTalents);
        return mutated
                ? selectMutationTalents(aTalents, bTalents, random)
                : selectNormalTalents(aTalents, bTalents, random);
    }

    public static void applyTalentPoints(LivingEntity entity) {
        if (!LevelService.isAffected(entity) || !ProgressData.exists(entity)) {
            return;
        }
        ProgressData data = ProgressData.of(entity);
        TalentMigrationService.migrateIfNeeded(entity, data);
        Map<String, ProgressData.StatPoints> current = data.allStats();
        for (Map.Entry<String, ProgressData.StatPoints> entry : current.entrySet()) {
            ProgressData.StatPoints points = entry.getValue();
            if (points.talent() != 0) {
                data.stat(entry.getKey(), points.withTalent(0));
            }
        }
        if (entity instanceof Player) {
            return;
        }
        for (Map.Entry<String, Integer> talentEntry : data.talents().entrySet()) {
            TalentDefinition definition = TalentConfigManager.byId(talentEntry.getKey());
            if (definition == null) {
                continue;
            }
            int level = definition.clampLevel(talentEntry.getValue());
            for (Map.Entry<String, Integer> effect : definition.effectsFor(level).entrySet()) {
                if (effect.getValue() <= 0
                        || com.szypxj.tldomesticatemorecreatures.torpor.TorporService.STAT_ID.equals(effect.getKey())) {
                    continue;
                }
                ProgressData.StatPoints points = data.stat(effect.getKey());
                long value = (long) points.talent() + effect.getValue();
                data.stat(effect.getKey(), points.withTalent((int) Math.min(Integer.MAX_VALUE, value)));
            }
        }
    }

    private static Map<String, Integer> selectNormalTalents(
            Map<String, Integer> aTalents,
            Map<String, Integer> bTalents,
            RandomSource random
    ) {
        List<String> parentPool = new ArrayList<>();
        Set<String> union = new HashSet<>();
        union.addAll(aTalents.keySet());
        union.addAll(bTalents.keySet());
        parentPool.addAll(union);
        shuffle(parentPool, random);

        Map<String, Integer> selected = new LinkedHashMap<>();
        for (String id : parentPool) {
            if (selected.size() >= INHERITED_TALENTS) {
                break;
            }
            TalentDefinition definition = TalentConfigManager.byId(id);
            if (definition == null) {
                continue;
            }
            int level = TalentConfigManager.randomLevel(definition, random);
            if (aTalents.containsKey(id) && bTalents.containsKey(id)) {
                level = Math.min(definition.maxLevel(), level + 1);
            }
            selected.put(id, level);
        }
        fillFromDefaultPool(selected, random);
        return Map.copyOf(selected);
    }

    private static Map<String, Integer> selectMutationTalents(
            Map<String, Integer> aTalents,
            Map<String, Integer> bTalents,
            RandomSource random
    ) {
        Map<String, Integer> highest = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : aTalents.entrySet()) {
            highest.merge(entry.getKey(), entry.getValue(), (left, right) -> Math.max(left, right));
        }
        for (Map.Entry<String, Integer> entry : bTalents.entrySet()) {
            highest.merge(entry.getKey(), entry.getValue(), (left, right) -> Math.max(left, right));
        }

        List<Map.Entry<String, Integer>> ranked = new ArrayList<>(highest.entrySet());
        shuffleEntries(ranked, random);
        ranked.sort(Comparator.comparingInt((Map.Entry<String, Integer> entry) -> entry.getValue()).reversed());

        Map<String, Integer> selected = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : ranked) {
            if (selected.size() >= INHERITED_TALENTS) {
                break;
            }
            TalentDefinition definition = TalentConfigManager.byId(entry.getKey());
            if (definition == null) {
                continue;
            }
            int level = definition.clampLevel(entry.getValue());
            if (aTalents.containsKey(entry.getKey()) && bTalents.containsKey(entry.getKey())) {
                level = Math.min(definition.maxLevel(), level + 1);
            }
            selected.put(entry.getKey(), level);
        }
        fillFromDefaultPool(selected, random);
        return Map.copyOf(selected);
    }

    private static void fillFromDefaultPool(Map<String, Integer> selected, RandomSource random) {
        Set<String> excluded = new HashSet<>(selected.keySet());
        while (selected.size() < INHERITED_TALENTS) {
            TalentDefinition definition = TalentConfigManager.weightedRandom(random, excluded);
            if (definition == null) {
                break;
            }
            selected.put(definition.id(), TalentConfigManager.randomLevel(definition, random));
            excluded.add(definition.id());
        }
    }

    private static Map<String, Integer> validTalents(Map<String, Integer> source) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            TalentDefinition definition = TalentConfigManager.byId(entry.getKey());
            if (definition != null) {
                result.put(entry.getKey(), definition.clampLevel(entry.getValue()));
            }
        }
        return result;
    }

    private static void shuffle(List<String> list, RandomSource random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(list, i, j);
        }
    }

    private static void shuffleEntries(List<Map.Entry<String, Integer>> list, RandomSource random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(list, i, j);
        }
    }
}
