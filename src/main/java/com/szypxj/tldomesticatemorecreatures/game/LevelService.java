package com.szypxj.tldomesticatemorecreatures.game;

import com.szypxj.tldomesticatemorecreatures.compat.arsnouveau.ArsNouveauManaCompat;
import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.config.StatDefinition;
import com.szypxj.tldomesticatemorecreatures.data.EntityRule;
import com.szypxj.tldomesticatemorecreatures.data.EntityRuleManager;
import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.elite.EliteMath;
import com.szypxj.tldomesticatemorecreatures.elite.EliteService;
import com.szypxj.tldomesticatemorecreatures.game.genetics.ChildGeneticsResult;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticCarrierEntityMarker;
import com.szypxj.tldomesticatemorecreatures.game.genetics.ParentGeneticsSnapshot;
import com.szypxj.tldomesticatemorecreatures.imprint.ImprintData;
import com.szypxj.tldomesticatemorecreatures.imprint.ImprintService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.torpor.TorporService;
import com.szypxj.tldomesticatemorecreatures.talent.SpecialTalentService;
import com.szypxj.tldomesticatemorecreatures.talent.TalentMigrationService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.util.RandomSource;

public final class LevelService {
    private static final int TORPOR_POINT_MIGRATION_VERSION = 1;

    private LevelService() {
    }

    public static boolean isAffected(LivingEntity entity) {
        if (entity instanceof Player) {
            return true;
        }
        if (entity instanceof GeneticCarrierEntityMarker) {
            return false;
        }
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null) {
            return false;
        }
        EntityRule rule = EntityRuleManager.INSTANCE.get(entityId);
        return EntityFilterPolicy.isAffected(
                entityId.toString(),
                Config.ENTITY_FILTER_MODE.get(),
                Config.ENTITY_FILTER.get(),
                Config.ENTITY_MOD_BLACKLIST.get(),
                Config.ENTITY_MOD_BLACKLIST_EXCEPTIONS.get(),
                rule.enabled()
        );
    }

    public static boolean isAffectedEntityId(String entityId) {
        ResourceLocation id = ResourceLocation.tryParse(entityId == null ? "" : entityId.trim());
        if (id == null) {
            return false;
        }
        EntityRule rule = EntityRuleManager.INSTANCE.get(id);
        return EntityFilterPolicy.isAffected(
                id.toString(),
                Config.ENTITY_FILTER_MODE.get(),
                Config.ENTITY_FILTER.get(),
                Config.ENTITY_MOD_BLACKLIST.get(),
                Config.ENTITY_MOD_BLACKLIST_EXCEPTIONS.get(),
                rule.enabled()
        );
    }

    public static void initializeIfNeeded(LivingEntity entity) {
        BaseAttributeSnapshotService.captureIfAbsent(entity);
        if (!isAffected(entity)) {
            return;
        }
        if (ProgressData.exists(entity)) {
            ProgressData data = ProgressData.of(entity);
            boolean torporMigrated = migrateLegacyTorporPoints(entity, data);
            EliteService.migrateExisting(entity);
            syncTameState(entity);
            if (torporMigrated) {
                AttributeService.applyAll(entity);
            }
            return;
        }
        if (entity instanceof Player) {
            initializePlayer(entity);
        } else {
            initializeMob(entity);
        }
        migrateLegacyTorporPoints(entity, ProgressData.of(entity));
        AttributeService.applyAll(entity);
    }

    public static ParentGeneticsSnapshot captureParentGenetics(LivingEntity parent) {
        if (parent == null || !isAffected(parent)) {
            return null;
        }
        initializeIfNeeded(parent);
        TalentService.ensureTalents(parent);
        SpecialTalentService.ensureTalents(parent);
        if (!ProgressData.exists(parent)) {
            return null;
        }

        ProgressData data = ProgressData.of(parent);
        Map<String, ParentGeneticsSnapshot.StatPoints> stats = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, ProgressData.StatPoints> entry : data.allStats().entrySet()) {
            ProgressData.StatPoints points = entry.getValue();
            stats.put(entry.getKey(), new ParentGeneticsSnapshot.StatPoints(
                    points.wild(),
                    points.trained(),
                    points.talent()
            ));
        }
        UUID ownerUuid = PetOwnershipService.ownerUuid(parent).orElse(null);
        return new ParentGeneticsSnapshot(
                data.level(),
                data.paternalMutations(),
                data.maternalMutations(),
                stats,
                data.talents(),
                data.specialTalents(),
                ownerUuid
        );
    }

    public static void initializeInherited(LivingEntity child, LivingEntity parentA, LivingEntity parentB, RandomSource random) {
        if (child == null || !isAffected(child)) {
            return;
        }
        ParentGeneticsSnapshot a = captureParentGenetics(parentA);
        ParentGeneticsSnapshot b = captureParentGenetics(parentB);
        if (a == null || b == null) {
            initializeIfNeeded(child);
            return;
        }
        initializeInherited(child, a, b, random);
    }

    public static boolean initializeInherited(
            LivingEntity child,
            ParentGeneticsSnapshot a,
            ParentGeneticsSnapshot b,
            RandomSource random
    ) {
        if (child == null || a == null || b == null || !isAffected(child)) {
            return false;
        }
        ResourceLocation childId = ForgeRegistries.ENTITY_TYPES.getKey(child.getType());
        if (childId == null) {
            return false;
        }
        ChildGeneticsResult result = resolveInheritedResult(
                childId.toString(),
                a,
                b,
                random == null ? child.getRandom() : random
        );
        return result != null && applyInheritedResult(child, result);
    }

    public static ChildGeneticsResult resolveInheritedResult(
            String childEntityId,
            ParentGeneticsSnapshot a,
            ParentGeneticsSnapshot b,
            RandomSource random
    ) {
        if (a == null || b == null) {
            return null;
        }
        ResourceLocation childId = ResourceLocation.tryParse(childEntityId == null ? "" : childEntityId.trim());
        if (childId == null || !isAffectedEntityId(childId.toString())) {
            return null;
        }
        RandomSource inheritanceRandom = random == null ? RandomSource.create() : random;
        boolean mutated = inheritanceRandom.nextDouble() < Config.MUTATION_CHANCE.get();
        List<StatDefinition> definitions = AttributeService.autoAssignableDefinitions(childId);
        Map<String, Integer> wildStats = new LinkedHashMap<>();
        long inheritedTotal = 0L;

        for (StatDefinition definition : definitions) {
            int aValue = a.stat(definition.id()).wild();
            int bValue = b.stat(definition.id()).wild();
            int inherited = inheritWildValue(aValue, bValue, mutated, inheritanceRandom);
            wildStats.put(definition.id(), inherited);
            inheritedTotal = Math.min(Integer.MAX_VALUE, inheritedTotal + inherited);
        }

        int legacyTorporInherited = inheritWildValue(
                a.stat(TorporService.STAT_ID).wild(),
                b.stat(TorporService.STAT_ID).wild(),
                mutated,
                inheritanceRandom
        );
        int redistributedTorpor = redistributeInheritedTorporPoints(
                wildStats,
                definitions,
                legacyTorporInherited,
                inheritanceRandom
        );
        inheritedTotal = Math.min(Integer.MAX_VALUE, inheritedTotal + redistributedTorpor);

        int parentALevel = a.level();
        int parentBLevel = b.level();
        int highestParentLevel = Math.max(parentALevel, parentBLevel);
        long average = ((long) parentALevel + parentBLevel) / 2L;
        long calculatedLevel = average + 20L;
        EntityRule childRule = EntityRuleManager.INSTANCE.get(childId);
        int normalInitialCap = childRule.maxLevel() == null
                ? Math.max(1, Config.WILD_MAX_LEVEL.get())
                : Math.max(1, childRule.maxLevel());
        int initialLevel = (int) Math.max(1L, Math.min((long) normalInitialCap, calculatedLevel));

        if (mutated && inheritedTotal + 1L > normalInitialCap) {
            long mutationLevel = Math.min(inheritedTotal + 1L, highestParentLevel);
            if (mutationLevel > initialLevel) {
                initialLevel = (int) Math.min(Integer.MAX_VALUE, mutationLevel);
            }
        }

        long allowedInheritedPoints = Math.max(0L, (long) initialLevel - 1L);
        if (inheritedTotal > allowedInheritedPoints) {
            trimInheritedWildStats(wildStats, definitions, inheritedTotal - allowedInheritedPoints);
        }

        int paternalMutations = a.paternalMutations() + a.maternalMutations();
        int maternalMutations = b.paternalMutations() + b.maternalMutations();
        if (mutated) {
            if (inheritanceRandom.nextBoolean()) {
                paternalMutations++;
            } else {
                maternalMutations++;
            }
        }

        UUID inheritedOwner = a.ownerUuid() != null
                && b.ownerUuid() != null
                && a.ownerUuid().equals(b.ownerUuid())
                ? a.ownerUuid()
                : null;
        Map<String, Integer> talents = TalentService.resolveInheritedTalents(
                a.talents(),
                b.talents(),
                mutated,
                inheritanceRandom
        );
        java.util.Set<String> specialTalents = SpecialTalentService.resolveInheritedTalents(
                childId.toString(),
                a.specialTalents(),
                b.specialTalents(),
                inheritanceRandom
        );
        return new ChildGeneticsResult(
                initialLevel,
                paternalMutations,
                maternalMutations,
                mutated,
                inheritedOwner,
                wildStats,
                talents,
                specialTalents
        );
    }

    public static boolean applyInheritedResult(LivingEntity child, ChildGeneticsResult result) {
        if (child == null || result == null || !isAffected(child)) {
            return false;
        }
        ProgressData data = ProgressData.of(child);
        data.clearStats();
        data.clearTalents();
        data.clearSpecialTalents();
        int inheritedTorporPoints = Math.max(0, result.wildStat(TorporService.STAT_ID));
        for (StatDefinition definition : AttributeService.definitionsFor(child)) {
            int inheritedPoints = AttributeService.isPointAssignable(definition)
                    ? result.wildStat(definition.id())
                    : 0;
            data.stat(definition.id(), new ProgressData.StatPoints(inheritedPoints, 0, 0));
        }

        if (result.ownerUuid() != null) {
            PetOwnershipService.setInheritedOwner(child, result.ownerUuid());
        }

        int initialLevel = result.initialLevel();
        data.initialized(true);
        data.elite(false);
        data.eliteDetermined(true);
        data.initialLevel(initialLevel);
        data.level(initialLevel);
        data.experience(0L);
        data.unspentPoints(0);
        data.tamed(result.ownerUuid() != null || isCurrentlyTamed(child));
        data.tamingBonusLevels(0);
        data.tamingBonusApplied(data.tamed());
        data.maxLevel(calculateMaxLevel(child, initialLevel));
        data.paternalMutations(result.paternalMutations());
        data.maternalMutations(result.maternalMutations());
        data.talents(TalentMigrationService.migrateLegacyTalents(child.getUUID(), result.talents()));
        data.talentsInitialized(true);
        data.talentSchemaVersion(TalentMigrationService.CURRENT_SCHEMA_VERSION);
        SpecialTalentService.applyInheritedTalents(child, data, result.specialTalents());
        if (inheritedTorporPoints > 0) {
            allocateWildRandom(child, data, inheritedTorporPoints);
        }
        data.torporPointMigrationVersion(TORPOR_POINT_MIGRATION_VERSION);
        TalentService.applyTalentPoints(child);
        AttributeService.applyAll(child);
        ImprintService.startIfEligible(child);
        return true;
    }

    private static int inheritWildValue(int aValue, int bValue, boolean mutated, RandomSource random) {
        if (mutated) {
            return aValue == bValue && random.nextBoolean() ? aValue : Math.max(aValue, bValue);
        }
        if (aValue == bValue) {
            return random.nextBoolean() ? aValue : bValue;
        }
        int high = Math.max(aValue, bValue);
        int low = Math.min(aValue, bValue);
        return random.nextDouble() < Config.INHERIT_HIGHER_STAT_CHANCE.get() ? high : low;
    }

    private static int redistributeInheritedTorporPoints(
            Map<String, Integer> wildStats,
            List<StatDefinition> definitions,
            int points,
            RandomSource random
    ) {
        int remaining = Math.max(0, points);
        int assigned = 0;
        List<StatDefinition> candidates = definitions.stream()
                .filter(StatDefinition::wildRandom)
                .filter(definition -> definition.randomWeight() > 0.0D)
                .toList();
        while (remaining > 0 && !candidates.isEmpty()) {
            double totalWeight = 0.0D;
            for (StatDefinition definition : candidates) {
                int current = Math.max(0, wildStats.getOrDefault(definition.id(), 0));
                if (definition.maxPoints() < 0 || current < definition.maxPoints()) {
                    totalWeight += definition.randomWeight();
                }
            }
            if (totalWeight <= 0.0D) {
                break;
            }
            double roll = random.nextDouble() * totalWeight;
            StatDefinition selected = null;
            for (StatDefinition definition : candidates) {
                int current = Math.max(0, wildStats.getOrDefault(definition.id(), 0));
                if (definition.maxPoints() >= 0 && current >= definition.maxPoints()) {
                    continue;
                }
                roll -= definition.randomWeight();
                if (roll <= 0.0D) {
                    selected = definition;
                    break;
                }
            }
            if (selected == null) {
                break;
            }
            int current = Math.max(0, wildStats.getOrDefault(selected.id(), 0));
            wildStats.put(selected.id(), current == Integer.MAX_VALUE ? Integer.MAX_VALUE : current + 1);
            remaining--;
            assigned++;
        }
        return assigned;
    }

    private static void trimInheritedWildStats(
            Map<String, Integer> wildStats,
            List<StatDefinition> definitions,
            long pointsToRemove
    ) {
        if (pointsToRemove <= 0L || definitions.isEmpty()) {
            return;
        }
        while (pointsToRemove > 0L) {
            boolean removedAny = false;
            for (StatDefinition definition : definitions) {
                if (pointsToRemove <= 0L) {
                    break;
                }
                int value = wildStats.getOrDefault(definition.id(), 0);
                if (value <= 0) {
                    continue;
                }
                wildStats.put(definition.id(), value - 1);
                pointsToRemove--;
                removedAny = true;
            }
            if (!removedAny) {
                break;
            }
        }
    }

    public static void markTamed(LivingEntity entity) {
        markTamed(entity, Config.TAMING_BONUS_RATE.get());
    }

    public static void markTamed(LivingEntity entity, double tamingBonusRate) {
        initializeIfNeeded(entity);
        if (!ProgressData.exists(entity) || EliteService.isElite(entity)) {
            return;
        }
        ProgressData data = ProgressData.of(entity);
        data.tamed(true);
        applyTamingBonus(entity, data, Math.max(0.0D, tamingBonusRate));
        data.maxLevel(Math.max(data.level(), calculateMaxLevel(entity, data.initialLevel())));
        AttributeService.applyAll(entity);
        syncLevel(entity);
    }

    public static void normalizeEliteForTaming(LivingEntity entity) {
        initializeIfNeeded(entity);
        if (!ProgressData.exists(entity) || !EliteService.isElite(entity) || !EliteService.canBeTamed(entity)) {
            return;
        }

        ProgressData data = ProgressData.of(entity);
        EntityRule entityRule = rule(entity);
        if (entityRule.fixedStats().isEmpty()) {
            Map<String, Integer> wild = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, ProgressData.StatPoints> entry : data.allStats().entrySet()) {
                wild.put(entry.getKey(), entry.getValue().wild());
            }
            Map<String, Integer> normalized = EliteMath.normalizeWildPoints(
                    wild,
                    EliteMath.wildPointBudget(data.initialLevel(), false)
            );
            for (Map.Entry<String, ProgressData.StatPoints> entry : data.allStats().entrySet()) {
                data.stat(entry.getKey(), entry.getValue().withWild(normalized.getOrDefault(entry.getKey(), 0)));
            }
        } else {
            for (Map.Entry<String, ProgressData.StatPoints> entry : data.allStats().entrySet()) {
                data.stat(entry.getKey(), entry.getValue().withWild(0));
            }
            int torporFixedPoints = 0;
            for (Map.Entry<String, Integer> entry : entityRule.fixedStats().entrySet()) {
                int value = Math.max(0, entry.getValue());
                if (TorporService.STAT_ID.equals(entry.getKey())) {
                    torporFixedPoints = saturatingIntSum(torporFixedPoints, value);
                    continue;
                }
                ProgressData.StatPoints points = data.stat(entry.getKey());
                data.stat(entry.getKey(), points.withWild(value));
            }
            if (torporFixedPoints > 0) {
                allocateWildRandom(entity, data, torporFixedPoints);
            }
        }

        EliteService.clearEliteState(entity);
        TalentService.applyTalentPoints(entity);
        AttributeService.applyAll(entity);
        syncLevel(entity);
    }

    public static boolean canGainExperience(LivingEntity entity) {
        if (!isAffected(entity) || !ProgressData.exists(entity)) {
            return false;
        }
        ProgressData data = ProgressData.of(entity);
        if (!(entity instanceof Player) && !data.tamed()) {
            return false;
        }
        EntityRule rule = rule(entity);
        if (rule.canGainExperience() != null) {
            return rule.canGainExperience();
        }
        return true;
    }

    public static boolean canLevelUp(LivingEntity entity) {
        if (!isAffected(entity) || !ProgressData.exists(entity)) {
            return false;
        }
        ProgressData data = ProgressData.of(entity);
        if (!(entity instanceof Player) && !data.tamed()) {
            return false;
        }
        EntityRule rule = rule(entity);
        if (rule.canLevelUp() != null) {
            return rule.canLevelUp();
        }
        return true;
    }

    public static boolean canStoreKillExperience(LivingEntity entity) {
        if (!isAffected(entity) || entity instanceof Player || !ProgressData.exists(entity) || !canGainExperience(entity)) {
            return false;
        }
        ProgressData data = ProgressData.of(entity);
        return data.tamed() && data.level() >= data.maxLevel();
    }

    public static long addExperience(LivingEntity entity, long amount) {
        return addExperience(entity, amount, ExperienceSource.COMMAND);
    }

    public static long addExperience(LivingEntity entity, long amount, ExperienceSource source) {
        if (amount <= 0L) {
            return 0L;
        }
        initializeIfNeeded(entity);
        if (!canGainExperience(entity)) {
            return 0L;
        }

        ProgressData data = ProgressData.of(entity);
        boolean maxLevel = data.level() >= data.maxLevel();
        if (maxLevel) {
            if (source != ExperienceSource.KILL || !canStoreKillExperience(entity)) {
                return 0L;
            }
            long before = data.experience();
            long after = ExperienceMath.saturatingAdd(before, amount);
            data.experience(after);
            return Math.max(0L, after - before);
        }

        if (!canLevelUp(entity)) {
            return 0L;
        }

        long beforeExperience = data.experience();
        long pooledExperience = ExperienceMath.saturatingAdd(beforeExperience, amount);
        long acceptedInput = Math.max(0L, pooledExperience - beforeExperience);
        long experience = pooledExperience;
        int oldLevel = data.level();

        while (data.level() < data.maxLevel()) {
            long required = requiredExperience(entity, data.level() + 1);
            if (experience < required) {
                break;
            }
            experience -= required;
            data.level(data.level() + 1);
            data.unspentPoints(data.unspentPoints() + 1);
        }

        long consumed = acceptedInput;
        if (data.level() >= data.maxLevel()) {
            boolean storeOverflow = source == ExperienceSource.KILL
                    && !(entity instanceof Player)
                    && data.tamed();
            if (!storeOverflow) {
                consumed = ExperienceMath.clampConsumed(acceptedInput - experience, acceptedInput);
                experience = 0L;
            }
        }

        data.experience(experience);
        if (data.level() != oldLevel) {
            syncLevel(entity);
        }
        return consumed;
    }

    public static long requiredExperience(LivingEntity entity, int targetLevel) {
        if (!isAffected(entity)) {
            return 0L;
        }
        double levelFactor = Math.max(1, targetLevel);

        if (!(entity instanceof Player) && ProgressData.exists(entity)) {
            ProgressData data = ProgressData.of(entity);
            if (data.tamed()) {
                levelFactor = Math.max(1, targetLevel - data.initialLevel() - data.tamingBonusLevels());
            }
        }

        double raw = Config.LEVEL_BASE_XP.get() * levelFactor * Config.LEVEL_RATE.get();
        if (!Double.isFinite(raw) || raw >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, Math.round(raw));
    }

    public static long killExperience(LivingEntity victim) {
        if (!isAffected(victim) || victim instanceof Player || !ProgressData.exists(victim)) {
            return 0L;
        }
        double entityMultiplier = rule(victim).killExperienceMultiplier() == null ? 1.0D : rule(victim).killExperienceMultiplier();
        double baseExperience = EliteService.isElite(victim) ? Config.ELITE_KILL_BASE_XP.get() : Config.KILL_BASE_XP.get();
        double raw = baseExperience * ProgressData.of(victim).level() * Config.KILL_XP_MULTIPLIER.get() * entityMultiplier;
        if (!Double.isFinite(raw) || raw >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, Math.round(raw));
    }

    public static boolean allocatePoint(LivingEntity entity, String statId) {
        if (!isAffected(entity)) {
            return false;
        }
        initializeIfNeeded(entity);
        if (!ProgressData.exists(entity)) {
            return false;
        }
        ProgressData data = ProgressData.of(entity);
        if (data.unspentPoints() <= 0) {
            return false;
        }
        StatDefinition definition = null;
        for (StatDefinition candidate : AttributeService.definitionsFor(entity)) {
            if (candidate.id().equals(statId)) {
                definition = candidate;
                break;
            }
        }
        if (definition == null
                || !AttributeService.isPointAssignable(definition)
                || !AttributeService.isVisible(entity, definition)
                || !AttributeService.isTargetAvailable(entity, definition)) {
            return false;
        }
        ProgressData.StatPoints points = data.stat(statId);
        if (!AttributeService.canReceivePoint(entity, definition, points.regular())) {
            return false;
        }
        data.stat(statId, points.withTrained(points.trained() + 1));
        data.unspentPoints(data.unspentPoints() - 1);
        AttributeService.applyAll(entity);
        return true;
    }

    public static void setLevel(LivingEntity entity, int level) {
        if (!isAffected(entity)) {
            return;
        }
        initializeIfNeeded(entity);
        if (!ProgressData.exists(entity)) {
            return;
        }
        ProgressData data = ProgressData.of(entity);
        data.level(Math.max(1, Math.min(data.maxLevel(), level)));
        data.experience(0L);
        syncLevel(entity);
    }

    public static void addUnspentPoints(LivingEntity entity, int amount) {
        if (!isAffected(entity)) {
            return;
        }
        initializeIfNeeded(entity);
        if (!ProgressData.exists(entity)) {
            return;
        }
        ProgressData data = ProgressData.of(entity);
        long value = (long) data.unspentPoints() + amount;
        data.unspentPoints((int) Math.max(0L, Math.min(Integer.MAX_VALUE, value)));
    }

    public static int refundTrainedPoints(LivingEntity entity) {
        if (!isAffected(entity)) {
            return 0;
        }
        initializeIfNeeded(entity);
        if (!ProgressData.exists(entity)) {
            return 0;
        }
        ProgressData data = ProgressData.of(entity);
        long refunded = 0L;
        for (Map.Entry<String, ProgressData.StatPoints> entry : data.allStats().entrySet()) {
            ProgressData.StatPoints points = entry.getValue();
            if (points.trained() <= 0) {
                continue;
            }
            refunded += points.trained();
            data.stat(entry.getKey(), new ProgressData.StatPoints(points.wild(), 0, points.talent()));
        }
        refunded += ArsNouveauManaCompat.refundAllocatedPoints(entity);
        if (refunded <= 0L) {
            return 0;
        }
        long totalUnspent = (long) data.unspentPoints() + refunded;
        data.unspentPoints((int) Math.min(Integer.MAX_VALUE, totalUnspent));
        AttributeService.applyAll(entity);
        return (int) Math.min(Integer.MAX_VALUE, refunded);
    }

    public static boolean setStatPoints(LivingEntity entity, String statId, int points) {
        if (!isAffected(entity)) {
            return false;
        }
        initializeIfNeeded(entity);
        if (!ProgressData.exists(entity)) {
            return false;
        }
        if (TorporService.STAT_ID.equals(statId)) {
            return false;
        }
        boolean exists = AttributeService.definitionsFor(entity).stream()
                .anyMatch(definition -> definition.id().equals(statId) && AttributeService.isPointAssignable(definition));
        if (!exists) {
            return false;
        }
        ProgressData data = ProgressData.of(entity);
        ProgressData.StatPoints current = data.stat(statId);
        data.stat(statId, new ProgressData.StatPoints(0, Math.max(0, points), current.talent()));
        AttributeService.applyAll(entity);
        return true;
    }

    public static void reset(LivingEntity entity) {
        if (ProgressData.exists(entity)) {
            ProgressData.of(entity).loadTag(new net.minecraft.nbt.CompoundTag());
        }
        initializeIfNeeded(entity);
        syncLevel(entity);
    }

    public static void reapplyAll(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living) {
                    if (!isAffected(living)) {
                        AttributeService.applyAll(living);
                        continue;
                    }
                    initializeIfNeeded(living);
                    if (ProgressData.exists(living)) {
                        ProgressData data = ProgressData.of(living);
                        data.maxLevel(Math.max(data.level(), calculateMaxLevel(living, data.initialLevel())));
                    }
                    TalentService.ensureTalents(living);
                    SpecialTalentService.ensureTalents(living);
                    AttributeService.applyAll(living);
                    syncLevel(living);
                }
            }
        }
    }

    public static void syncLevel(LivingEntity entity) {
        if (!isAffected(entity)) {
            return;
        }
        if (!entity.level().isClientSide && ProgressData.exists(entity)) {
            NetworkHandler.sendLevel(entity);
        }
    }

    public static int calculateMaxLevel(LivingEntity entity, int initialLevel) {
        if (entity instanceof Player) {
            return Math.max(1, Config.PLAYER_MAX_LEVEL.get());
        }

        EntityRule rule = rule(entity);
        double multiplier = rule.maxLevelMultiplier() == null ? Config.MAX_LEVEL_MULTIPLIER.get() : rule.maxLevelMultiplier();
        double upgradeRate = Math.max(0.0D, multiplier - 1.0D);
        long upgradeLevels = safeRoundedLevels(initialLevel, upgradeRate);
        long tamingBonusLevels = ProgressData.exists(entity) ? ProgressData.of(entity).tamingBonusLevels() : 0L;
        long imprintBonusLevels = ImprintData.exists(entity) ? ImprintData.of(entity).levelCapBonus() : 0L;
        long maxLevel = saturatingLevelSum(initialLevel, tamingBonusLevels, upgradeLevels, imprintBonusLevels);
        return (int) Math.max(initialLevel, Math.min(Integer.MAX_VALUE, maxLevel));
    }

    public static EntityRule rule(LivingEntity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id == null ? EntityRule.empty("") : EntityRuleManager.INSTANCE.get(id);
    }

    private static void trimInheritedPoints(
            ProgressData data,
            List<StatDefinition> definitions,
            long pointsToRemove
    ) {
        if (pointsToRemove <= 0L || definitions.isEmpty()) {
            return;
        }

        while (pointsToRemove > 0L) {
            boolean removedAny = false;
            for (StatDefinition definition : definitions) {
                if (pointsToRemove <= 0L) {
                    break;
                }
                ProgressData.StatPoints points = data.stat(definition.id());
                if (points.wild() <= 0) {
                    continue;
                }
                data.stat(definition.id(), points.withWild(points.wild() - 1));
                pointsToRemove--;
                removedAny = true;
            }
            if (!removedAny) {
                break;
            }
        }
    }

    private static ProgressData.StatPoints inheritNormalPoints(
            ProgressData.StatPoints a,
            ProgressData.StatPoints b,
            RandomSource random
    ) {
        int aValue = a.wild();
        int bValue = b.wild();
        int inherited;
        if (aValue == bValue) {
            inherited = random.nextBoolean() ? aValue : bValue;
        } else {
            int high = Math.max(aValue, bValue);
            int low = Math.min(aValue, bValue);
            inherited = random.nextDouble() < Config.INHERIT_HIGHER_STAT_CHANCE.get() ? high : low;
        }
        return new ProgressData.StatPoints(inherited, 0, 0);
    }

    private static ProgressData.StatPoints inheritHigherPoints(
            ProgressData.StatPoints a,
            ProgressData.StatPoints b,
            RandomSource random
    ) {
        int aValue = a.wild();
        int bValue = b.wild();
        int inherited = aValue == bValue && random.nextBoolean() ? aValue : Math.max(aValue, bValue);
        return new ProgressData.StatPoints(inherited, 0, 0);
    }

    private static int inheritedInitialLevelCap(LivingEntity entity) {
        EntityRule rule = rule(entity);

        if (rule.maxLevel() != null) {
            return Math.max(1, rule.maxLevel());
        }

        return Math.max(1, Config.WILD_MAX_LEVEL.get());
    }

    private static void initializePlayer(LivingEntity entity) {
        ProgressData data = ProgressData.of(entity);
        data.initialized(true);
        data.elite(false);
        data.eliteDetermined(true);
        data.initialLevel(1);
        data.level(1);
        data.maxLevel(Math.max(1, Config.PLAYER_MAX_LEVEL.get()));
        data.experience(0L);
        data.unspentPoints(0);
        data.tamed(true);
        data.tamingBonusApplied(true);
        data.tamingBonusLevels(0);
        data.clearStats();
        data.clearTalents();
        data.talentsInitialized(true);
        data.clearSpecialTalents();
        data.specialTalentsInitialized(true);
    }

    private static void initializeMob(LivingEntity entity) {
        EntityRule rule = rule(entity);
        int min = rule.minLevel() == null ? Config.WILD_MIN_LEVEL.get() : Math.max(1, rule.minLevel());
        int max = rule.maxLevel() == null ? Config.WILD_MAX_LEVEL.get() : Math.max(1, rule.maxLevel());
        if (max < min) {
            int swap = min;
            min = max;
            max = swap;
        }
        int level = rule.fixedLevel() == null ? randomInclusive(entity.getRandom(), min, max) : Math.max(1, rule.fixedLevel());
        ProgressData data = ProgressData.of(entity);
        data.initialized(true);
        data.initialLevel(level);
        data.level(level);
        data.experience(0L);
        data.unspentPoints(0);
        data.tamed(isCurrentlyTamed(entity));
        data.tamingBonusApplied(false);
        data.tamingBonusLevels(0);
        data.clearStats();
        EliteService.initializeFresh(entity, data);

        if (rule.fixedStats().isEmpty()) {
            allocateWildRandom(entity, data, EliteMath.wildPointBudget(level, data.elite()));
        } else {
            int torporFixedPoints = 0;
            for (Map.Entry<String, Integer> entry : rule.fixedStats().entrySet()) {
                int points = Math.max(0, entry.getValue());
                if (TorporService.STAT_ID.equals(entry.getKey())) {
                    torporFixedPoints = saturatingIntSum(torporFixedPoints, points);
                    continue;
                }
                data.stat(entry.getKey(), new ProgressData.StatPoints(points, 0, 0));
            }
            if (torporFixedPoints > 0) {
                allocateWildRandom(entity, data, torporFixedPoints);
            }
            if (data.elite()) {
                allocateWildRandom(entity, data, EliteMath.eliteBonusPoints(level));
            }
        }
        TalentService.initializeWildTalents(entity, data, entity.getRandom());
        SpecialTalentService.initializeWildTalents(entity, data, entity.getRandom());

        if (data.tamed()) {
            applyTamingBonus(entity, data, Config.TAMING_BONUS_RATE.get());
        } else {
            data.maxLevel(calculateMaxLevel(entity, level));
        }
    }

    private static void allocateWildRandom(LivingEntity entity, ProgressData data, int remaining) {
        if (remaining <= 0) {
            return;
        }

        List<StatDefinition> candidates = new ArrayList<>();

        for (StatDefinition definition : AttributeService.autoAssignableDefinitions(entity)) {
            if (AttributeService.isTargetAvailable(entity, definition)) {
                candidates.add(definition);
            }
        }

        int safety = 0;

        while (remaining > 0 && !candidates.isEmpty() && safety < 2_000_000) {
            safety++;

            double totalWeight = 0.0D;

            for (StatDefinition definition : candidates) {
                ProgressData.StatPoints points = data.stat(definition.id());

                if (AttributeService.canReceivePoint(entity, definition, points.regular())) {
                    totalWeight += randomWeightFor(data, definition);
                }
            }

            if (totalWeight <= 0.0D) {
                break;
            }

            double roll = entity.getRandom().nextDouble() * totalWeight;
            StatDefinition selected = null;

            for (StatDefinition definition : candidates) {
                ProgressData.StatPoints points = data.stat(definition.id());

                if (!AttributeService.canReceivePoint(entity, definition, points.regular())) {
                    continue;
                }

                roll -= randomWeightFor(data, definition);

                if (roll <= 0.0D) {
                    selected = definition;
                    break;
                }
            }

            if (selected == null) {
                break;
            }

            ProgressData.StatPoints points = data.stat(selected.id());

            data.stat(
                    selected.id(),
                    points.withWild(points.wild() + 1)
            );

            remaining--;
        }
    }

    private static double randomWeightFor(ProgressData data, StatDefinition definition) {
        boolean combatStat = data.elite() && Config.ELITE_COMBAT_STATS.get().contains(definition.id());
        return EliteMath.statWeight(
                definition.randomWeight(),
                combatStat,
                Config.ELITE_COMBAT_STAT_WEIGHT_MULTIPLIER.get()
        );
    }

    private static boolean migrateLegacyTorporPoints(LivingEntity entity, ProgressData data) {
        if (data.torporPointMigrationVersion() >= TORPOR_POINT_MIGRATION_VERSION) {
            return false;
        }
        ProgressData.StatPoints points = data.stat(TorporService.STAT_ID);
        int oldWild = points.wild();
        int oldTrained = points.trained();
        boolean changed = points.total() > 0;

        data.stat(TorporService.STAT_ID, new ProgressData.StatPoints(0, 0, 0));

        if (oldTrained > 0) {
            data.unspentPoints(saturatingIntSum(data.unspentPoints(), oldTrained));
        }
        if (oldWild > 0) {
            allocateWildRandom(entity, data, oldWild);
        }

        data.torporPointMigrationVersion(TORPOR_POINT_MIGRATION_VERSION);
        return changed;
    }

    private static int saturatingIntSum(int left, int right) {
        long total = (long) Math.max(0, left) + Math.max(0, right);
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    private static void applyTamingBonus(LivingEntity entity, ProgressData data, double rate) {
        if (entity instanceof Player || data.tamingBonusApplied()) {
            return;
        }

        int bonusLevels = calculateTamingBonusLevels(data.initialLevel(), rate);
        data.tamingBonusApplied(true);
        data.tamingBonusLevels(bonusLevels);

        if (bonusLevels > 0) {
            allocateWildRandom(entity, data, bonusLevels);
            long boostedLevel = Math.min(Integer.MAX_VALUE, (long) data.level() + bonusLevels);
            data.level((int) boostedLevel);
        }

        data.maxLevel(Math.max(data.level(), calculateMaxLevel(entity, data.initialLevel())));
    }

    private static int calculateTamingBonusLevels(int initialLevel, double rate) {
        long rounded = safeRoundedLevels(initialLevel, rate);
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, rounded));
    }

    private static long safeRoundedLevels(int initialLevel, double rate) {
        if (initialLevel <= 0 || rate <= 0.0D) {
            return 0L;
        }
        double raw = initialLevel * rate;
        if (!Double.isFinite(raw) || raw >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0L, Math.round(raw));
    }

    private static long saturatingLevelSum(long... values) {
        long result = 0L;
        for (long value : values) {
            long add = Math.max(0L, value);
            if (add > Long.MAX_VALUE - result) {
                return Long.MAX_VALUE;
            }
            result += add;
        }
        return result;
    }

    private static void syncTameState(LivingEntity entity) {
        if (!ProgressData.exists(entity) || entity instanceof Player || EliteService.isElite(entity)) {
            return;
        }

        ProgressData data = ProgressData.of(entity);
        if (data.tamed() && data.tamingBonusApplied()) {
            return;
        }
        if (!isCurrentlyTamed(entity)) {
            return;
        }

        boolean changed = false;
        if (!data.tamed()) {
            data.tamed(true);
            changed = true;
        }

        if (!data.tamingBonusApplied()) {
            applyTamingBonus(entity, data, Config.TAMING_BONUS_RATE.get());
            changed = true;
        }

        int maxLevel = Math.max(data.level(), calculateMaxLevel(entity, data.initialLevel()));
        if (data.maxLevel() != maxLevel) {
            data.maxLevel(maxLevel);
            changed = true;
        }

        if (changed) {
            AttributeService.applyAll(entity);
            syncLevel(entity);
        }
    }

    private static boolean isCurrentlyTamed(LivingEntity entity) {
        return PetOwnershipService.isManagedPet(entity);
    }

    private static int randomInclusive(RandomSource random, int min, int max) {
        if (min >= max) {
            return min;
        }
        long range = (long) max - min + 1L;
        if (range <= Integer.MAX_VALUE) {
            return min + random.nextInt((int) range);
        }
        long value = Math.floorMod(random.nextLong(), range);
        return (int) (min + value);
    }

}
