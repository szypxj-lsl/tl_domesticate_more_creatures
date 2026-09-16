package com.szypxj.tldomesticatemorecreatures.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ProgressData {
    public static final String ROOT_KEY = "tl_domesticate_more_creatures_progress";
    private static final String LEGACY_ROOT_KEY = "tl_biological_attribute_panel_progress";
    private static final String STATS_KEY = "stats";
    private static final String TALENTS_KEY = "talents";
    private static final String SPECIAL_TALENTS_KEY = "specialTalents";

    private final CompoundTag root;

    private ProgressData(CompoundTag root) {
        this.root = root;
    }

    public static ProgressData of(LivingEntity entity) {
        migrateLegacy(entity);
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_KEY, new CompoundTag());
        }
        return new ProgressData(persistent.getCompound(ROOT_KEY));
    }

    public static boolean exists(LivingEntity entity) {
        migrateLegacy(entity);
        return entity.getPersistentData().contains(ROOT_KEY, Tag.TAG_COMPOUND)
                && entity.getPersistentData().getCompound(ROOT_KEY).getBoolean("initialized");
    }

    public static void copy(LivingEntity from, LivingEntity to) {
        migrateLegacy(from);
        if (!from.getPersistentData().contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return;
        }
        to.getPersistentData().put(ROOT_KEY, from.getPersistentData().getCompound(ROOT_KEY).copy());
    }

    private static void migrateLegacy(LivingEntity entity) {
        CompoundTag persistent = entity.getPersistentData();
        if (persistent.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return;
        }
        if (persistent.contains(LEGACY_ROOT_KEY, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_KEY, persistent.getCompound(LEGACY_ROOT_KEY).copy());
            persistent.remove(LEGACY_ROOT_KEY);
        }
    }

    public boolean initialized() {
        return root.getBoolean("initialized");
    }

    public void initialized(boolean value) {
        root.putBoolean("initialized", value);
    }

    public int initialLevel() {
        return Math.max(1, root.getInt("initialLevel"));
    }

    public void initialLevel(int value) {
        root.putInt("initialLevel", Math.max(1, value));
    }

    public int level() {
        return Math.max(1, root.getInt("level"));
    }

    public void level(int value) {
        root.putInt("level", Math.max(1, value));
    }

    public int maxLevel() {
        return Math.max(1, root.getInt("maxLevel"));
    }

    public void maxLevel(int value) {
        root.putInt("maxLevel", Math.max(1, value));
    }

    public long experience() {
        return Math.max(0L, root.getLong("experience"));
    }

    public void experience(long value) {
        root.putLong("experience", Math.max(0L, value));
    }

    public int unspentPoints() {
        return Math.max(0, root.getInt("unspentPoints"));
    }

    public void unspentPoints(int value) {
        root.putInt("unspentPoints", Math.max(0, value));
    }

    public int torporPointMigrationVersion() {
        return Math.max(0, root.getInt("torporPointMigrationVersion"));
    }

    public void torporPointMigrationVersion(int value) {
        root.putInt("torporPointMigrationVersion", Math.max(0, value));
    }

    public boolean tamed() {
        return root.getBoolean("tamed");
    }

    public void tamed(boolean value) {
        root.putBoolean("tamed", value);
    }

    public boolean tamingBonusApplied() {
        return root.getBoolean("tamingBonusApplied");
    }

    public void tamingBonusApplied(boolean value) {
        root.putBoolean("tamingBonusApplied", value);
    }

    public int tamingBonusLevels() {
        return Math.max(0, root.getInt("tamingBonusLevels"));
    }

    public void tamingBonusLevels(int value) {
        root.putInt("tamingBonusLevels", Math.max(0, value));
    }

    public boolean elite() {
        return root.getBoolean("elite");
    }

    public void elite(boolean value) {
        root.putBoolean("elite", value);
    }

    public boolean eliteDetermined() {
        return root.getBoolean("eliteDetermined");
    }

    public void eliteDetermined(boolean value) {
        root.putBoolean("eliteDetermined", value);
    }


    public boolean talentsInitialized() {
        return root.getBoolean("talentsInitialized");
    }

    public void talentsInitialized(boolean value) {
        root.putBoolean("talentsInitialized", value);
    }

    public int talentSchemaVersion() {
        return Math.max(0, root.getInt("talentSchemaVersion"));
    }

    public void talentSchemaVersion(int value) {
        root.putInt("talentSchemaVersion", Math.max(0, value));
    }

    public String activeTalentCooldownSkillId() {
        return root.getString("activeTalentCooldownSkillId");
    }

    public void activeTalentCooldownSkillId(String id) {
        if (id == null || id.isBlank()) {
            root.remove("activeTalentCooldownSkillId");
        } else {
            root.putString("activeTalentCooldownSkillId", id);
        }
    }

    public long activeTalentCooldownEndGameTime() {
        return Math.max(0L, root.getLong("activeTalentCooldownEndGameTime"));
    }

    public void activeTalentCooldownEndGameTime(long gameTime) {
        root.putLong("activeTalentCooldownEndGameTime", Math.max(0L, gameTime));
    }

    public String activeTalentInProgress() {
        return root.getString("activeTalentInProgress");
    }

    public void activeTalentInProgress(String id) {
        if (id == null || id.isBlank()) {
            clearActiveTalentInProgress();
        } else {
            root.putString("activeTalentInProgress", id);
        }
    }

    public void clearActiveTalentInProgress() {
        root.remove("activeTalentInProgress");
    }


    public boolean specialTalentsInitialized() {
        return root.getBoolean("specialTalentsInitialized");
    }

    public void specialTalentsInitialized(boolean value) {
        root.putBoolean("specialTalentsInitialized", value);
    }

    public Set<String> specialTalents() {
        Set<String> result = new LinkedHashSet<>();
        CompoundTag talents = specialTalentsTag();
        for (String id : talents.getAllKeys()) {
            if (talents.contains(id, Tag.TAG_BYTE) && talents.getBoolean(id)) {
                result.add(id);
            }
        }
        return Set.copyOf(result);
    }

    public boolean hasSpecialTalent(String id) {
        return id != null && !id.isBlank() && specialTalentsTag().getBoolean(id);
    }

    public void specialTalent(String id, boolean value) {
        if (id == null || id.isBlank()) {
            return;
        }
        if (value) {
            specialTalentsTag().putBoolean(id, true);
        } else {
            specialTalentsTag().remove(id);
        }
    }

    public void specialTalents(Set<String> values) {
        CompoundTag talents = new CompoundTag();
        if (values != null) {
            for (String id : values) {
                if (id != null && !id.isBlank()) {
                    talents.putBoolean(id, true);
                }
            }
        }
        root.put(SPECIAL_TALENTS_KEY, talents);
    }

    public void clearSpecialTalents() {
        root.put(SPECIAL_TALENTS_KEY, new CompoundTag());
    }

    public double furyAnger() {
        return Math.max(0.0D, root.getDouble("furyAnger"));
    }

    public void furyAnger(double value) {
        root.putDouble("furyAnger", Math.max(0.0D, value));
    }

    public boolean furyBerserk() {
        return root.getBoolean("furyBerserk");
    }

    public void furyBerserk(boolean value) {
        root.putBoolean("furyBerserk", value);
    }

    public long furyLastDamageGameTime() {
        return root.contains("furyLastDamageGameTime", Tag.TAG_LONG) ? root.getLong("furyLastDamageGameTime") : Long.MIN_VALUE;
    }

    public void furyLastDamageGameTime(long value) {
        root.putLong("furyLastDamageGameTime", value);
    }

    public long furyLastUpdateGameTime() {
        return root.contains("furyLastUpdateGameTime", Tag.TAG_LONG) ? root.getLong("furyLastUpdateGameTime") : Long.MIN_VALUE;
    }

    public void furyLastUpdateGameTime(long value) {
        root.putLong("furyLastUpdateGameTime", value);
    }

    public int paternalMutations() {
        return Math.max(0, root.getInt("paternalMutations"));
    }

    public void paternalMutations(int value) {
        root.putInt("paternalMutations", Math.max(0, value));
    }

    public int maternalMutations() {
        return Math.max(0, root.getInt("maternalMutations"));
    }

    public void maternalMutations(int value) {
        root.putInt("maternalMutations", Math.max(0, value));
    }

    public StatPoints stat(String id) {
        CompoundTag stats = statsTag();
        if (!stats.contains(id, Tag.TAG_COMPOUND)) {
            stats.put(id, new CompoundTag());
        }
        CompoundTag tag = stats.getCompound(id);
        int talent = tag.contains("talent", Tag.TAG_INT)
                ? Math.max(0, tag.getInt("talent"))
                : Math.max(0, tag.getInt("mutation"));
        return new StatPoints(
                Math.max(0, tag.getInt("wild")),
                Math.max(0, tag.getInt("trained")),
                talent
        );
    }

    public void stat(String id, StatPoints points) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("wild", Math.max(0, points.wild()));
        tag.putInt("trained", Math.max(0, points.trained()));
        tag.putInt("talent", Math.max(0, points.talent()));
        statsTag().put(id, tag);
    }

    public Map<String, StatPoints> allStats() {
        Map<String, StatPoints> result = new LinkedHashMap<>();
        CompoundTag stats = statsTag();
        for (String key : stats.getAllKeys()) {
            if (stats.contains(key, Tag.TAG_COMPOUND)) {
                result.put(key, stat(key));
            }
        }
        return result;
    }

    public void clearStats() {
        root.put(STATS_KEY, new CompoundTag());
    }

    public Map<String, Integer> talents() {
        Map<String, Integer> result = new LinkedHashMap<>();
        CompoundTag talents = talentsTag();
        for (String id : talents.getAllKeys()) {
            if (talents.contains(id, Tag.TAG_INT)) {
                result.put(id, Math.max(1, talents.getInt(id)));
            }
        }
        return result;
    }

    public void talent(String id, int level) {
        if (id == null || id.isBlank()) {
            return;
        }
        talentsTag().putInt(id, Math.max(1, level));
    }

    public void talents(Map<String, Integer> values) {
        CompoundTag talents = new CompoundTag();
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null) {
                talents.putInt(entry.getKey(), Math.max(1, entry.getValue()));
            }
        }
        root.put(TALENTS_KEY, talents);
    }

    public void clearTalents() {
        root.put(TALENTS_KEY, new CompoundTag());
    }

    public CompoundTag copyTag() {
        return root.copy();
    }

    public void loadTag(CompoundTag tag) {
        for (String key : root.getAllKeys().toArray(String[]::new)) {
            root.remove(key);
        }
        root.merge(tag.copy());
    }

    private CompoundTag statsTag() {
        if (!root.contains(STATS_KEY, Tag.TAG_COMPOUND)) {
            root.put(STATS_KEY, new CompoundTag());
        }
        return root.getCompound(STATS_KEY);
    }

    private CompoundTag talentsTag() {
        if (!root.contains(TALENTS_KEY, Tag.TAG_COMPOUND)) {
            root.put(TALENTS_KEY, new CompoundTag());
        }
        return root.getCompound(TALENTS_KEY);
    }


    private CompoundTag specialTalentsTag() {
        if (!root.contains(SPECIAL_TALENTS_KEY, Tag.TAG_COMPOUND)) {
            root.put(SPECIAL_TALENTS_KEY, new CompoundTag());
        }
        return root.getCompound(SPECIAL_TALENTS_KEY);
    }

    public record StatPoints(int wild, int trained, int talent) {
        public int total() {
            return saturatingSum(wild, trained, talent);
        }

        public int regular() {
            return saturatingSum(wild, trained);
        }

        public StatPoints withWild(int value) {
            return new StatPoints(value, trained, talent);
        }

        public StatPoints withTrained(int value) {
            return new StatPoints(wild, value, talent);
        }

        public StatPoints withTalent(int value) {
            return new StatPoints(wild, trained, value);
        }

        private static int saturatingSum(int... values) {
            long total = 0L;
            for (int value : values) {
                total += Math.max(0, value);
                if (total >= Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
            }
            return (int) total;
        }
    }
}
