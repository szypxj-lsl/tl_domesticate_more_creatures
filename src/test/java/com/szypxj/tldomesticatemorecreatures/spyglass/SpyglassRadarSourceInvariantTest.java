package com.szypxj.tldomesticatemorecreatures.spyglass;

import java.nio.file.Files;
import java.nio.file.Path;

public final class SpyglassRadarSourceInvariantTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        Path snapshot = root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/network/SnapshotFactory.java");
        Path baseline = root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/spyglass/SpyglassRadarBaseline.java");
        String snapshotText = Files.readString(snapshot);
        String baselineText = Files.readString(baseline);

        assertContains(snapshotText, "BaseStats baseStats = CreatureInfoApi.getBaseStats(target.getType());");
        assertContains(snapshotText, "powerPercentile(baseStats.attackDamage())");
        assertContains(snapshotText, "lifePercentile(baseStats.maxHealth())");
        assertContains(snapshotText, "speedPercentile(baseStats.movementSpeed())");
        assertNotContains(snapshotText, "AttributeService.damageMultiplier(target)");
        assertNotContains(snapshotText, "SpyglassRadarMath.effectiveHealth(");
        assertContains(baselineText, "attack > 0.0D");
        assertContains(baselineText, "speed > 0.0D");
        assertContains(baselineText, "sortedUniqueCopy");
        System.out.println("SPYGLASS_RADAR_SOURCE_PASS");
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
