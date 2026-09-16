package com.szypxj.tldomesticatemorecreatures.imprint;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ImprintBirthTimingSourceInvariantTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        String gameplay = Files.readString(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/game/GameplayEvents.java"));
        String imprintEvents = Files.readString(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/imprint/ImprintEvents.java"));
        String service = Files.readString(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/imprint/ImprintService.java"));

        assertContains(gameplay, "@SubscribeEvent(priority = EventPriority.LOWEST)\n    public static void onBabySpawn");
        assertContains(gameplay, "ImprintService.markOffspring(child);");
        assertNotContains(gameplay, "ImprintService.startIfEligible(child);");
        assertContains(imprintEvents, "ImprintService.startMarkedOffspring(living);");
        assertContains(service, "PENDING_OFFSPRING_KEY");
        assertContains(service, "markOffspring(LivingEntity child)");
        assertContains(service, "startMarkedOffspring(LivingEntity child)");

        System.out.println("IMPRINT_BIRTH_TIMING_SOURCE_PASS");
    }

    private static void assertContains(String text, String needle) {
        if (!text.contains(needle)) throw new AssertionError("Missing source invariant: " + needle);
    }

    private static void assertNotContains(String text, String needle) {
        if (text.contains(needle)) throw new AssertionError("Forbidden source invariant present: " + needle);
    }
}
