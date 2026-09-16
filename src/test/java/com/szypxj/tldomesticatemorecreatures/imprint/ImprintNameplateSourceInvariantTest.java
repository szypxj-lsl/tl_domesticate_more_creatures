package com.szypxj.tldomesticatemorecreatures.imprint;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ImprintNameplateSourceInvariantTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        String network = Files.readString(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/network/NetworkHandler.java"));
        String clientState = Files.readString(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/client/ClientState.java"));
        String clientEvents = Files.readString(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/client/ClientEvents.java"));
        String zh = Files.readString(root.resolve("src/main/resources/assets/tl_domesticate_more_creatures/lang/zh_cn.json"));
        String en = Files.readString(root.resolve("src/main/resources/assets/tl_domesticate_more_creatures/lang/en_us.json"));

        assertContains(network, "private static final String VERSION = \"11\";");
        assertContains(network, "S2CImprintStatePacket");
        assertContains(clientState, "setImprintState");
        assertContains(clientState, "imprintRemainingTicks");
        assertContains(clientEvents, "RenderLivingEvent.Post");
        assertContains(clientEvents, "nameplate.tl_domesticate_more_creatures.imprint_remaining");
        assertContains(zh, "nameplate.tl_domesticate_more_creatures.imprint_remaining");
        assertContains(en, "nameplate.tl_domesticate_more_creatures.imprint_remaining");

        System.out.println("IMPRINT_NAMEPLATE_SOURCE_PASS");
    }

    private static void assertContains(String text, String needle) {
        if (!text.contains(needle)) throw new AssertionError("Missing source invariant: " + needle);
    }
}
