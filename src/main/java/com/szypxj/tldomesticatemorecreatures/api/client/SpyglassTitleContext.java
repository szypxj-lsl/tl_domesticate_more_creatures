package com.szypxj.tldomesticatemorecreatures.api.client;

import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStats;
import net.minecraft.resources.ResourceLocation;

public record SpyglassTitleContext(ResourceLocation entityTypeId, boolean tdmcAffected, BaseStats baseStats) {
    public SpyglassTitleContext {
        baseStats = baseStats == null ? BaseStats.NONE : baseStats;
    }
}
