package com.szypxj.tldomesticatemorecreatures.api.client;

import net.minecraft.network.chat.Component;

import java.util.List;

@FunctionalInterface
public interface PanelSummaryProvider {
    List<Component> lines(Context context);

    record Context(int entityId, boolean player) {
    }
}
