package com.szypxj.tldomesticatemorecreatures.game.genetics;

public final class CompatEntityIdPolicyTest {
    public static void main(String[] args) {
        require(CompatEntityIdPolicy.matchesNamespace("iceandfire:fire_dragon", "iceandfire"));
        require(CompatEntityIdPolicy.matchesNamespace(" SAINTSDRAGONS:ignivorus ", "saintsdragons"));
        require(!CompatEntityIdPolicy.matchesNamespace("minecraft:wolf", "iceandfire"));
        require(CompatEntityIdPolicy.matchesExact("ICEANDFIRE:dragon_egg", "iceandfire:dragon_egg"));
        require(!CompatEntityIdPolicy.matchesExact("iceandfire:fire_dragon", "iceandfire:dragon_egg"));
        require(CompatEntityIdPolicy.matchesAnyNamespace("ers:saevus", "ers", "oasis"));
        require(CompatEntityIdPolicy.matchesAnyNamespace("oasis:creature", "ers", "oasis"));
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError("condition failed");
        }
    }
}
