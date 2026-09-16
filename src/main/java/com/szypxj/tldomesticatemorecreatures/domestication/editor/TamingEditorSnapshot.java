package com.szypxj.tldomesticatemorecreatures.domestication.editor;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public record TamingEditorSnapshot(
        long generation,
        List<TamingEditorEntityInfo> entities,
        Map<ResourceLocation, TamingEditorRule> rules
) {
    public TamingEditorSnapshot {
        entities = List.copyOf(entities == null ? List.of() : entities);
        rules = Map.copyOf(rules == null ? Map.of() : rules);
    }
}
