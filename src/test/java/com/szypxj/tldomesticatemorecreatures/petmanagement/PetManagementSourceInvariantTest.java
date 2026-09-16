package com.szypxj.tldomesticatemorecreatures.petmanagement;

import java.nio.file.Files;
import java.nio.file.Path;

public final class PetManagementSourceInvariantTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        String service = Files.readString(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/petmanagement/PetManagementService.java"));
        String gameplay = Files.readString(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/game/GameplayEvents.java"));
        String ownership = Files.readString(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/game/PetOwnershipService.java"));
        String events = Files.readString(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/petmanagement/PetManagementEvents.java"));

        assertContains(service, "purgeInvalidOwnershipRecords");
        assertContains(service, "PetOwnershipService.isOwnedBy(entity, player)");
        assertContains(gameplay, "PetOwnershipService.observeTameInteraction(serverPlayer, target);");
        assertContains(events, "PetOwnershipService.tickPendingTameInteractions(event.getServer());");
        assertContains(ownership, "instanceof OwnableEntity ownable");
        assertContains(ownership, "adoptExternalTame");
        assertContains(ownership, "PetManagementService.track(entity)");

        System.out.println("PET_MANAGEMENT_EXTERNAL_TAME_BRIDGE_PASS");
    }

    private static void assertContains(String text, String needle) {
        if (!text.contains(needle)) {
            throw new AssertionError("Missing source invariant: " + needle);
        }
    }
}
