package com.szypxj.tldomesticatemorecreatures.game.genetics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class GeneticPayloadCodec {
    public static final String ROOT_KEY = "tl_domesticate_more_creatures_genetics";
    private static final String VERSION = "version";
    private static final String SEED = "seed";
    private static final String CHILD_ENTITY_ID = "childEntityId";
    private static final String RESULT = "result";
    private static final String PARENT_A = "parentA";
    private static final String PARENT_B = "parentB";
    private static final String LEVEL = "level";
    private static final String PATERNAL_MUTATIONS = "paternalMutations";
    private static final String MATERNAL_MUTATIONS = "maternalMutations";
    private static final String OWNER = "owner";
    private static final String STATS = "stats";
    private static final String TALENTS = "talents";
    private static final String SPECIAL_TALENTS = "specialTalents";

    private GeneticPayloadCodec() {
    }

    public static CompoundTag encode(HatchGeneticPayload payload) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION, payload.version());
        tag.putLong(SEED, payload.seed());
        if (!payload.childEntityId().isBlank()) {
            tag.putString(CHILD_ENTITY_ID, payload.childEntityId());
        }
        if (payload.result() != null) {
            tag.put(RESULT, encodeResult(payload.result()));
        }
        tag.put(PARENT_A, encodeParent(payload.parentA()));
        tag.put(PARENT_B, encodeParent(payload.parentB()));
        return tag;
    }

    public static Optional<HatchGeneticPayload> decode(CompoundTag tag) {
        if (tag == null
                || !tag.contains(VERSION, Tag.TAG_INT)
                || !tag.contains(PARENT_A, Tag.TAG_COMPOUND)
                || !tag.contains(PARENT_B, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        int version = tag.getInt(VERSION);
        if (version != HatchGeneticPayload.LEGACY_VERSION
                && version != HatchGeneticPayload.RESOLVED_LEGACY_VERSION
                && version != HatchGeneticPayload.CURRENT_VERSION) {
            return Optional.empty();
        }
        ParentGeneticsSnapshot a = decodeParent(tag.getCompound(PARENT_A));
        ParentGeneticsSnapshot b = decodeParent(tag.getCompound(PARENT_B));
        if (a == null || b == null) {
            return Optional.empty();
        }
        if (version == HatchGeneticPayload.LEGACY_VERSION) {
            return Optional.of(HatchGeneticPayload.legacy(tag.getLong(SEED), a, b));
        }
        if (!tag.contains(CHILD_ENTITY_ID, Tag.TAG_STRING) || !tag.contains(RESULT, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        String childEntityId = tag.getString(CHILD_ENTITY_ID);
        ChildGeneticsResult result = decodeResult(tag.getCompound(RESULT));
        if (childEntityId.isBlank() || result == null) {
            return Optional.empty();
        }
        return Optional.of(new HatchGeneticPayload(
                version,
                tag.getLong(SEED),
                childEntityId,
                a,
                b,
                result
        ));
    }

    private static CompoundTag encodeResult(ChildGeneticsResult result) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("initialLevel", result.initialLevel());
        tag.putInt(PATERNAL_MUTATIONS, result.paternalMutations());
        tag.putInt(MATERNAL_MUTATIONS, result.maternalMutations());
        tag.putBoolean("mutated", result.mutated());
        if (result.ownerUuid() != null) {
            tag.putUUID(OWNER, result.ownerUuid());
        }
        CompoundTag wildStats = new CompoundTag();
        for (Map.Entry<String, Integer> entry : result.wildStats().entrySet()) {
            wildStats.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("wildStats", wildStats);
        CompoundTag talents = new CompoundTag();
        for (Map.Entry<String, Integer> entry : result.talents().entrySet()) {
            talents.putInt(entry.getKey(), entry.getValue());
        }
        tag.put(TALENTS, talents);
        CompoundTag specialTalents = new CompoundTag();
        for (String id : result.specialTalents()) {
            specialTalents.putBoolean(id, true);
        }
        tag.put(SPECIAL_TALENTS, specialTalents);
        return tag;
    }

    private static ChildGeneticsResult decodeResult(CompoundTag tag) {
        if (tag == null || !tag.contains("initialLevel", Tag.TAG_INT)) {
            return null;
        }
        Map<String, Integer> wildStats = new LinkedHashMap<>();
        if (tag.contains("wildStats", Tag.TAG_COMPOUND)) {
            CompoundTag statsTag = tag.getCompound("wildStats");
            for (String id : statsTag.getAllKeys()) {
                if (statsTag.contains(id, Tag.TAG_INT)) {
                    wildStats.put(id, Math.max(0, statsTag.getInt(id)));
                }
            }
        }
        Map<String, Integer> talents = new LinkedHashMap<>();
        if (tag.contains(TALENTS, Tag.TAG_COMPOUND)) {
            CompoundTag talentsTag = tag.getCompound(TALENTS);
            for (String id : talentsTag.getAllKeys()) {
                if (talentsTag.contains(id, Tag.TAG_INT)) {
                    talents.put(id, Math.max(1, talentsTag.getInt(id)));
                }
            }
        }
        java.util.Set<String> specialTalents = new java.util.LinkedHashSet<>();
        if (tag.contains(SPECIAL_TALENTS, Tag.TAG_COMPOUND)) {
            CompoundTag specialTag = tag.getCompound(SPECIAL_TALENTS);
            for (String id : specialTag.getAllKeys()) {
                if (specialTag.getBoolean(id)) {
                    specialTalents.add(id);
                }
            }
        }
        UUID owner = tag.hasUUID(OWNER) ? tag.getUUID(OWNER) : null;
        return new ChildGeneticsResult(
                tag.getInt("initialLevel"),
                tag.getInt(PATERNAL_MUTATIONS),
                tag.getInt(MATERNAL_MUTATIONS),
                tag.getBoolean("mutated"),
                owner,
                wildStats,
                talents,
                specialTalents
        );
    }

    private static CompoundTag encodeParent(ParentGeneticsSnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(LEVEL, snapshot.level());
        tag.putInt(PATERNAL_MUTATIONS, snapshot.paternalMutations());
        tag.putInt(MATERNAL_MUTATIONS, snapshot.maternalMutations());
        if (snapshot.ownerUuid() != null) {
            tag.putUUID(OWNER, snapshot.ownerUuid());
        }

        CompoundTag stats = new CompoundTag();
        for (Map.Entry<String, ParentGeneticsSnapshot.StatPoints> entry : snapshot.stats().entrySet()) {
            CompoundTag points = new CompoundTag();
            points.putInt("wild", entry.getValue().wild());
            points.putInt("trained", entry.getValue().trained());
            points.putInt("talent", entry.getValue().talent());
            stats.put(entry.getKey(), points);
        }
        tag.put(STATS, stats);

        CompoundTag talents = new CompoundTag();
        for (Map.Entry<String, Integer> entry : snapshot.talents().entrySet()) {
            talents.putInt(entry.getKey(), Math.max(1, entry.getValue()));
        }
        tag.put(TALENTS, talents);
        CompoundTag specialTalents = new CompoundTag();
        for (String id : snapshot.specialTalents()) {
            specialTalents.putBoolean(id, true);
        }
        tag.put(SPECIAL_TALENTS, specialTalents);
        return tag;
    }

    private static ParentGeneticsSnapshot decodeParent(CompoundTag tag) {
        Map<String, ParentGeneticsSnapshot.StatPoints> stats = new LinkedHashMap<>();
        if (tag.contains(STATS, Tag.TAG_COMPOUND)) {
            CompoundTag statsTag = tag.getCompound(STATS);
            for (String id : statsTag.getAllKeys()) {
                if (!statsTag.contains(id, Tag.TAG_COMPOUND)) {
                    continue;
                }
                CompoundTag points = statsTag.getCompound(id);
                stats.put(id, new ParentGeneticsSnapshot.StatPoints(
                        points.getInt("wild"),
                        points.getInt("trained"),
                        points.getInt("talent")
                ));
            }
        }

        Map<String, Integer> talents = new LinkedHashMap<>();
        if (tag.contains(TALENTS, Tag.TAG_COMPOUND)) {
            CompoundTag talentsTag = tag.getCompound(TALENTS);
            for (String id : talentsTag.getAllKeys()) {
                if (talentsTag.contains(id, Tag.TAG_INT)) {
                    talents.put(id, Math.max(1, talentsTag.getInt(id)));
                }
            }
        }

        java.util.Set<String> specialTalents = new java.util.LinkedHashSet<>();
        if (tag.contains(SPECIAL_TALENTS, Tag.TAG_COMPOUND)) {
            CompoundTag specialTag = tag.getCompound(SPECIAL_TALENTS);
            for (String id : specialTag.getAllKeys()) {
                if (specialTag.getBoolean(id)) {
                    specialTalents.add(id);
                }
            }
        }
        UUID owner = tag.hasUUID(OWNER) ? tag.getUUID(OWNER) : null;
        return new ParentGeneticsSnapshot(
                tag.getInt(LEVEL),
                tag.getInt(PATERNAL_MUTATIONS),
                tag.getInt(MATERNAL_MUTATIONS),
                stats,
                talents,
                specialTalents,
                owner
        );
    }
}
