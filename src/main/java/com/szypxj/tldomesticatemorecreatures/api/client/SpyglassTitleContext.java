package com.szypxj.tldomesticatemorecreatures.api.client;

import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStats;
import net.minecraft.resources.ResourceLocation;

public record SpyglassTitleContext(
        ResourceLocation entityTypeId,
        boolean tdmcAffected,
        BaseStats baseStats,
        boolean elite,
        int radarPower,
        int radarLife,
        int radarSpeed
) {
    public SpyglassTitleContext(ResourceLocation entityTypeId, boolean tdmcAffected, BaseStats baseStats) {
        this(entityTypeId, tdmcAffected, baseStats, false, 0, 0, 0);
    }

    public SpyglassTitleContext(ResourceLocation entityTypeId, boolean tdmcAffected, BaseStats baseStats, boolean elite) {
        this(entityTypeId, tdmcAffected, baseStats, elite, 0, 0, 0);
    }

    public SpyglassTitleContext {
        baseStats = baseStats == null ? BaseStats.NONE : baseStats;
        radarPower = clamp(radarPower);
        radarLife = clamp(radarLife);
        radarSpeed = clamp(radarSpeed);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
