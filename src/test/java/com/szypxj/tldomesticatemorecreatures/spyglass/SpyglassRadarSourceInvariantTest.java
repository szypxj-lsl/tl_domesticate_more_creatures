package com.szypxj.tldomesticatemorecreatures.spyglass;

import java.nio.file.Files;
import java.nio.file.Path;

public final class SpyglassRadarSourceInvariantTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        Path snapshot = root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/network/SnapshotFactory.java");
        Path baseline = root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/spyglass/SpyglassRadarBaseline.java");
        Path danger = root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/game/DangerRatingStatsService.java");
        String snapshotText = Files.readString(snapshot);
        String baselineText = Files.readString(baseline);
        String dangerText = Files.readString(danger);

        assertContains(snapshotText, "CreatureInfoApi.getDangerRatingStats(target)");
        assertContains(snapshotText, "powerPercentile(baseStats.attackDamage())");
        assertContains(snapshotText, "lifePercentile(baseStats.maxHealth())");
        assertContains(snapshotText, "speedPercentile(baseStats.movementSpeed())");
        assertContains(baselineText, "DangerRatingStatsService.get(type)");
        assertContains(baselineText, "sortedUniqueCopy");
        assertContains(dangerText, "CreatureThreatProviderRegistry.resolveLive");
        assertContains(dangerText, "CreatureThreatProviderRegistry.resolveRepresentative");
        assertNotContains(dangerText, "OBSERVED_TYPE_STATS");
        System.out.println("SPYGLASS_RADAR_THREAT_SOURCE_PASS");
    }

    private static void assertContains(String text, String needle) {
        if (!text.contains(needle)) {
            throw new AssertionError("Missing " + needle);
        }
    }

    private static void assertNotContains(String text, String needle) {
        if (text.contains(needle)) {
            throw new AssertionError("Unexpected " + needle);
        }
    }
}
