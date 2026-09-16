package com.szypxj.tldomesticatemorecreatures.api.client;

import java.util.Objects;

public record PanelAction(String id, String nameKey, int width, Runnable action) {
    public PanelAction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nameKey, "nameKey");
        Objects.requireNonNull(action, "action");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        if (nameKey.isBlank()) {
            throw new IllegalArgumentException("nameKey cannot be blank");
        }
        width = Math.max(40, Math.min(100, width));
    }
}
