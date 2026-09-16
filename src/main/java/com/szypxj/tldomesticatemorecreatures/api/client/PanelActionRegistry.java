package com.szypxj.tldomesticatemorecreatures.api.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PanelActionRegistry {
    private static final Map<String, PanelAction> ACTIONS = new ConcurrentHashMap<>();

    private PanelActionRegistry() {
    }

    public static void register(PanelAction action) {
        PanelAction previous = ACTIONS.putIfAbsent(action.id(), action);
        if (previous != null) {
            throw new IllegalArgumentException("Panel action already registered: " + action.id());
        }
    }

    public static List<PanelAction> actions() {
        return ACTIONS.values().stream().sorted(java.util.Comparator.comparing(PanelAction::id)).toList();
    }

    public static void clearForTests() {
        ACTIONS.clear();
    }
}
