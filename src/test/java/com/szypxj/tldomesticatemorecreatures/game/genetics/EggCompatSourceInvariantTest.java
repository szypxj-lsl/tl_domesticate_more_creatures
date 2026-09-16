package com.szypxj.tldomesticatemorecreatures.game.genetics;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class EggCompatSourceInvariantTest {
    private EggCompatSourceInvariantTest() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        Path gameplay = root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/game/GameplayEvents.java");
        Path levelService = root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/game/LevelService.java");
        Path mixinConfig = root.resolve("src/main/resources/tl_domesticate_more_creatures.mixins.json");
        Path compatRoot = root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/mixin/compat");

        assertContains(gameplay, "HatchScope.tryApply(living)");
        assertContains(gameplay, "GeneticEntityTransferScope.tryTransfer(event.getEntity())");
        assertContains(gameplay, "GeneticItemEntityTransferScope.tryTransfer(event.getEntity())");
        assertContains(levelService, "instanceof GeneticCarrierEntityMarker");
        assertContains(root.resolve("build.gradle"), "MixinConfigs");
        assertContains(mixinConfig, "IceAndFireDragonBaseMixin");
        assertContains(mixinConfig, "IceAndFireDragonEggMixin");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/mixin/compat/iceandfire/IceAndFireDragonEggMixin.java"), "com.github.alexthe666.iceandfire.item.ItemDragonEgg");
        assertContains(mixinConfig, "IceAndFireDragonEggItemMixin");
        assertContains(mixinConfig, "IceAndFireEggInIceMixin");
        assertContains(mixinConfig, "SaintsDragonEntityMixin");
        assertContains(mixinConfig, "SaintsTimedEggBlockMixin");
        assertContains(mixinConfig, "SaintsRaevyxEggBlockMixin");
        assertContains(mixinConfig, "ErsArtificialNestMixin");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/mixin/compat/ers/ErsArtificialNestMixin.java"), "anyNamespace(child, \"ers\", \"oasis\")");
        assertContains(mixinConfig, "\"defaultRequire\": 0");

        try (Stream<Path> files = Files.walk(compatRoot)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(EggCompatSourceInvariantTest::assertNoHardDependencyImport);
        }

        System.out.println("EGG_COMPAT_SOURCE_INVARIANTS_PASS");
    }

    private static void assertNoHardDependencyImport(Path path) {
        try {
            String text = Files.readString(path);
            if (text.contains("import com.github.alexthe666")
                    || text.contains("import com.leon.saintsdragons")
                    || text.contains("import cn.aurorian.ers")) {
                throw new AssertionError("Hard third-party import found: " + path);
            }
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void assertContains(Path path, String needle) throws Exception {
        String text = Files.readString(path);
        if (!text.contains(needle)) {
            throw new AssertionError("Missing source invariant: " + needle + " in " + path);
        }
    }
}
