package com.szypxj.tldomesticatemorecreatures.imprint;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ImprintAllOffspringSourceInvariantTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        String gameplay = Files.readString(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/game/GameplayEvents.java"));
        String service = Files.readString(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/imprint/ImprintService.java"));

        assertContains(gameplay, "ImprintService.markOffspring(child);");
        assertContains(service, "startIfEligible(LivingEntity child)");
        assertContains(service, "startMarkedOffspring(LivingEntity child)");
        assertNotContains(service, "inheritedOwnerUuid == null");
        assertContains(service, "PetOwnershipService.ownerUuid(child).map(owner.getUUID()::equals).orElse(true)");
        assertContains(service, "private static final long NEXT_NEED_DELAY_TICKS = 30L * 20L;");
        assertContains(service, "data.needAt(0, now);");
        assertContains(service, "data.needAt(index + 1, saturatingAdd(now, NEXT_NEED_DELAY_TICKS));");

        System.out.println("IMPRINT_ALL_OFFSPRING_SOURCE_PASS");
    }

    private static void assertContains(String text, String needle) {
        if (!text.contains(needle)) {
            throw new AssertionError("Missing source invariant: " + needle);
        }
    }

    private static void assertNotContains(String text, String needle) {
        if (text.contains(needle)) {
            throw new AssertionError("Forbidden source invariant present: " + needle);
        }
    }
}
