package com.szypxj.tldomesticatemorecreatures.imprint;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ImprintInteractionInvariantTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/imprint/ImprintData.java"), "lastCareAt()");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/imprint/ImprintService.java"), "data.lastCareAt() == now");
        System.out.println("IMPRINT_INTERACTION_PASS");
    }
    private static void assertContains(Path path, String needle) throws Exception {
        if (!Files.readString(path).contains(needle)) throw new AssertionError("Missing " + needle);
    }
}
