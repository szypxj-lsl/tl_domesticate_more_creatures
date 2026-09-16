package com.szypxj.tldomesticatemorecreatures.game.genetics;

import java.nio.file.Files;
import java.nio.file.Path;

public final class InheritanceResultSourceInvariantTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        Path levelService = root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/game/LevelService.java");
        Path hatchService = root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/game/genetics/HatchGeneticsService.java");
        Path hatchScope = root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/game/genetics/HatchScope.java");
        Path talentService = root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/game/TalentService.java");

        assertContains(levelService, "resolveInheritedResult(");
        assertContains(levelService, "applyInheritedResult(");
        assertContains(levelService, "ChildGeneticsResult result = resolveInheritedResult(");
        assertContains(hatchService, "LevelService.resolveInheritedResult(");
        assertContains(hatchScope, "payload.result()");
        assertContains(hatchScope, "LevelService.applyInheritedResult(");
        assertContains(talentService, "resolveInheritedTalents(");
        System.out.println("INHERITANCE_RESULT_SOURCE_PASS");
    }

    private static void assertContains(Path path, String needle) throws Exception {
        String text = Files.readString(path);
        if (!text.contains(needle)) {
            throw new AssertionError("Missing source invariant: " + needle + " in " + path);
        }
    }
}
