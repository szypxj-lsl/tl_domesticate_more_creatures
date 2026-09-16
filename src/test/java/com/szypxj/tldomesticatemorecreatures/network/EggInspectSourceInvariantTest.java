package com.szypxj.tldomesticatemorecreatures.network;

import java.nio.file.Files;
import java.nio.file.Path;

public final class EggInspectSourceInvariantTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/network/InspectSnapshot.java"), "boolean unborn");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/network/InspectSnapshot.java"), "InspectTarget target");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/network/packet/C2SInspectPacket.java"), "GeneticBlockCarrierData.get");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/network/packet/C2SInspectPacket.java"), "GeneticItemCarrier.get");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/client/ClientEvents.java"), "InspectTarget.block");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/client/ClientEvents.java"), "GeneticCarrierEntityMarker");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/network/SnapshotFactory.java"), "inspectEgg(");
        assertContains(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/client/SpyglassPanelRenderer.java"), "snapshot.unborn()");
        System.out.println("EGG_INSPECT_SOURCE_PASS");
    }

    private static void assertContains(Path path, String needle) throws Exception {
        String text = Files.readString(path);
        if (!text.contains(needle)) throw new AssertionError("Missing " + needle + " in " + path);
    }
}
