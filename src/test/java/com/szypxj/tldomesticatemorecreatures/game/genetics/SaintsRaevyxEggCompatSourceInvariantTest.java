package com.szypxj.tldomesticatemorecreatures.game.genetics;

import java.nio.file.Files;
import java.nio.file.Path;

public final class SaintsRaevyxEggCompatSourceInvariantTest {
    private SaintsRaevyxEggCompatSourceInvariantTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        Path mixinConfig = root.resolve("src/main/resources/tl_domesticate_more_creatures.mixins.json");
        Path mixin = root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/mixin/compat/saintsdragons/SaintsRaevyxEggBlockMixin.java");

        assertContains(mixinConfig, "SaintsRaevyxEggBlockMixin");
        assertContains(mixin, "com.leon.saintsdragons.common.block.RaevyxEggBlock");
        assertContains(mixin, "method = \"hatchEgg\"");
        assertContains(mixin, "@At(\"HEAD\")");
        assertContains(mixin, "@At(\"RETURN\")");
        assertContains(mixin, "GeneticBlockCarrierData.get(blockEntity)");
        assertContains(mixin, "CompatEntityPredicates.exact(child, \"saintsdragons:raevyx\")");
        assertContains(mixin, "GeneticBlockCarrierData.clear(blockEntity)");
        assertContains(mixin, "HatchScope.begin(");
        assertContains(mixin, "HatchScope.end()");

        System.out.println("SAINTS_RAEVYX_EGG_COMPAT_SOURCE_PASS");
    }

    private static void assertContains(Path path, String needle) throws Exception {
        if (!Files.exists(path)) {
            throw new AssertionError("Missing required file: " + path);
        }
        String text = Files.readString(path);
        if (!text.contains(needle)) {
            throw new AssertionError("Missing source invariant: " + needle + " in " + path);
        }
    }
}
