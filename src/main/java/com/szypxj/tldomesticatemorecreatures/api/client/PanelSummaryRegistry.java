package com.szypxj.tldomesticatemorecreatures.api.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PanelSummaryRegistry {
    private static final Map<ResourceLocation, PanelSummaryProvider> PROVIDERS = new LinkedHashMap<>();

    private PanelSummaryRegistry() {
    }

    public static synchronized void register(ResourceLocation id, PanelSummaryProvider provider) {
        if (id == null || provider == null) return;
        PROVIDERS.put(id, provider);
    }

    public static synchronized void unregister(ResourceLocation id) {
        if (id != null) PROVIDERS.remove(id);
    }

    public static synchronized List<Component> lines(PanelSummaryProvider.Context context) {
        if (context == null || PROVIDERS.isEmpty()) return List.of();
        List<Component> result = new ArrayList<>();
        for (PanelSummaryProvider provider : PROVIDERS.values()) {
            List<Component> lines = provider.lines(context);
            if (lines == null || lines.isEmpty()) continue;
            for (Component line : lines) {
                if (line != null) result.add(line);
            }
        }
        return List.copyOf(result);
    }
}
