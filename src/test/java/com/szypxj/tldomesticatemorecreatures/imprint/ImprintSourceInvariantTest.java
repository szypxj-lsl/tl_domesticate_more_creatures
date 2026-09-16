package com.szypxj.tldomesticatemorecreatures.imprint;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ImprintSourceInvariantTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/game/LevelService.java"), "ImprintService.startIfEligible(child);");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/game/LevelService.java"), "ImprintData.of(entity).levelCapBonus()");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/game/AttributeService.java"), "ImprintService.bondHealthMultiplier(entity)");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/game/AttributeService.java"), "ImprintService.bondDamageMultiplier(entity)");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/command/pet/PetCommand.java"), "FOLLOW");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/command/pet/PetCommandService.java"), "if (command == PetCommand.FOLLOW)");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/command/pet/PetCommandService.java"), "ImprintService.onFollowCommand(owner, pet);");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/network/PanelSnapshot.java"), "ImprintSnapshot imprint");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/network/InspectSnapshot.java"), "ImprintSnapshot imprint");
        System.out.println("IMPRINT_SOURCE_PASS");
    }

    private static void assertContains(Path path, String needle) throws Exception {
        String text = Files.readString(path);
        if (!text.contains(needle)) throw new AssertionError("Missing " + needle + " in " + path);
    }
}
