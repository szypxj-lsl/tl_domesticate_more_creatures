package com.szypxj.tldomesticatemorecreatures.command.pet;

import java.nio.file.Files;
import java.nio.file.Path;

public final class PetCommandIntentSourceInvariantTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        String command = Files.readString(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/command/pet/PetCommand.java"));
        String service = Files.readString(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/command/pet/PetCommandService.java"));
        String compat = Files.readString(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/compat/tlmarking/TlMarkingCompat.java"));

        assertContains(command, "MOVE,");
        assertContains(command, "ATTACK,");
        assertContains(command, "FOLLOW,");
        assertContains(command, "DEFEND,");
        assertContains(command, "RETREAT");
        assertNotContains(command, "CAUTION");
        assertNotContains(command, "GATHER");
        assertNotContains(command, "DANGER");

        assertContains(service, "PetCommandAdapterRegistry.applyCommand");
        assertContains(service, "PetCommandAdapterRegistry.tick");
        assertContains(service, "public static void clearForOwner(ServerPlayer owner)");
        assertNotContains(service, "mob.doHurtTarget(target)");
        assertNotContains(service, "case CAUTION");
        assertNotContains(service, "case GATHER");
        assertNotContains(service, "case DANGER");

        assertNotContains(compat, "processPing(");
        assertNotContains(compat, "PetCommandService.issue(");

        System.out.println("PET_COMMAND_INTENT_SOURCE_PASS");
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
