package com.szypxj.tldomesticatemorecreatures.network;

import java.nio.file.Files;
import java.nio.file.Path;

public final class EggInspectResourcesInvariantTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        String zh = Files.readString(root.resolve("src/main/resources/assets/tl_domesticate_more_creatures/lang/zh_cn.json"));
        String en = Files.readString(root.resolve("src/main/resources/assets/tl_domesticate_more_creatures/lang/en_us.json"));
        String network = Files.readString(root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/network/NetworkHandler.java"));
        String mixins = Files.readString(root.resolve("src/main/resources/tl_domesticate_more_creatures.mixins.json"));
        String[] keys = {
                "egg_unhatched",
                "egg_no_genetics",
                "egg_expected_level",
                "egg_paternal_mutations",
                "egg_maternal_mutations",
                "egg_mutation_result",
                "egg_owner_inherited"
        };
        for (String key : keys) {
            if (!zh.contains(key) || !en.contains(key)) throw new AssertionError("missing lang key " + key);
        }
        if (network.contains("VERSION = \"7\"") || network.contains("VERSION = \"8\"") || network.contains("VERSION = \"9\"") || network.contains("VERSION = \"10\"") || !network.contains("VERSION = \"11\"")) {
            throw new AssertionError("inspect protocol version was not bumped");
        }
        if (!mixins.contains("SaintsDragonEggBlockEntityMixin")) {
            throw new AssertionError("Saint's Dragons egg block entity marker missing");
        }
        if (zh.contains("egg_unhatched\": \"§7") || zh.contains("egg_unhatched\": \"§8")) {
            throw new AssertionError("forbidden gray formatting in new keys");
        }
        System.out.println("EGG_INSPECT_RESOURCES_PASS");
    }
}
