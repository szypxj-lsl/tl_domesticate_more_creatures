package com.szypxj.tldomesticatemorecreatures.client;

public final class ClientTorporState {
    private static volatile boolean unconscious;

    private ClientTorporState() {
    }

    public static boolean isUnconscious() {
        return unconscious;
    }

    public static void setUnconscious(boolean value) {
        unconscious = value;
    }

    public static void clear() {
        unconscious = false;
    }
}
