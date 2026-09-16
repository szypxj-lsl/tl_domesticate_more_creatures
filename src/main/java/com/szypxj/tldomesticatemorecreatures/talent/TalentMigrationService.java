package com.szypxj.tldomesticatemorecreatures.talent;

import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import net.minecraft.world.entity.LivingEntity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class TalentMigrationService {
    public static final int CURRENT_SCHEMA_VERSION = 2;
    private static final Set<String> LEGACY_IDS = Set.of(
            "fierce", "defender", "relentless", "shadowless", "peerless", "traceless"
    );
    private static final List<String> NEW_IDS = List.of(
            "vitality", "warbreaker", "windchaser", "wavewalker", "tenacity",
            "valor", "windhunter", "tideborn", "ironheart", "seaguard"
    );

    private TalentMigrationService() {
    }

    public static void migrateIfNeeded(LivingEntity entity, ProgressData data) {
        if (entity == null || data == null || data.talentSchemaVersion() >= CURRENT_SCHEMA_VERSION) {
            return;
        }
        if (!data.talentsInitialized()) {
            return;
        }
        Map<String, Integer> migrated = migrateLegacyTalents(entity.getUUID(), data.talents());
        data.talents(migrated);
        data.talentSchemaVersion(CURRENT_SCHEMA_VERSION);
    }

    public static Map<String, Integer> migrateLegacyTalents(UUID stableId, Map<String, Integer> oldTalents) {
        Map<String, Integer> source = oldTalents == null ? Map.of() : oldTalents;
        List<Map.Entry<String, Integer>> legacy = source.entrySet().stream()
                .filter(entry -> LEGACY_IDS.contains(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .toList();
        if (legacy.isEmpty()) {
            return Map.copyOf(new LinkedHashMap<>(source));
        }

        Set<String> occupiedNewIds = source.keySet().stream()
                .filter(NEW_IDS::contains)
                .collect(java.util.stream.Collectors.toSet());
        List<String> replacements = new ArrayList<>(NEW_IDS);
        replacements.removeIf(occupiedNewIds::contains);
        Random random = new Random(stableSeed(stableId, legacy));
        for (int i = replacements.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(replacements, i, j);
        }

        Map<String, Integer> result = new LinkedHashMap<>();
        source.entrySet().stream()
                .filter(entry -> !LEGACY_IDS.contains(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> result.put(entry.getKey(), Math.max(1, entry.getValue())));
        int count = Math.min(legacy.size(), replacements.size());
        for (int i = 0; i < count; i++) {
            result.put(replacements.get(i), Math.max(1, Math.min(3, legacy.get(i).getValue())));
        }
        return Map.copyOf(result);
    }

    public static UUID stableId(long seed, String discriminator) {
        String value = seed + ":" + (discriminator == null ? "" : discriminator);
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private static long stableSeed(UUID stableId, List<Map.Entry<String, Integer>> orderedLegacy) {
        UUID id = stableId == null ? new UUID(0L, 0L) : stableId;
        long seed = id.getMostSignificantBits() ^ Long.rotateLeft(id.getLeastSignificantBits(), 17);
        for (Map.Entry<String, Integer> entry : orderedLegacy) {
            seed = mix(seed ^ entry.getKey().hashCode());
            seed = mix(seed ^ Math.max(1, entry.getValue()));
        }
        return seed;
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdl;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53l;
        value ^= value >>> 33;
        return value;
    }
}
