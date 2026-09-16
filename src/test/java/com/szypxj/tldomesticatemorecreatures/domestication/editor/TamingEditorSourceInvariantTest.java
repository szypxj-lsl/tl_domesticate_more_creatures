package com.szypxj.tldomesticatemorecreatures.domestication.editor;

import java.nio.file.Files;
import java.nio.file.Path;

public final class TamingEditorSourceInvariantTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/domestication/editor/TamingEditorService.java"), "ATOMIC_MOVE");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/domestication/editor/TamingEditorService.java"), "DefaultAttributes.hasSupplier");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/domestication/editor/TamingEditorService.java"), "animal.isFood");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/command/ModCommands.java"), "Commands.literal(\"taming\")");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/client/taming/TamingEditorScreen.java"), "class TamingEditorScreen");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/client/taming/TamingItemPickerScreen.java"), "ForgeRegistries.ITEMS");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/network/packet/C2SSaveTamingEditorPacket.java"), "sendTamingEditorOperationResult");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/network/packet/C2SReloadTamingEditorPacket.java"), "sendTamingEditorOperationResult");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/client/taming/TamingEditorScreen.java"), "onOperationResult");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/api/creature/CreatureInfoApi.java"), "TamingRuleManager.displayFoodsFor");
        System.out.println("TAMING_EDITOR_SOURCE_PASS");
    }

    private static void assertContains(Path path, String needle) throws Exception {
        String text = Files.readString(path);
        if (!text.contains(needle)) {
            throw new AssertionError("Missing " + needle + " in " + path);
        }
    }
}
