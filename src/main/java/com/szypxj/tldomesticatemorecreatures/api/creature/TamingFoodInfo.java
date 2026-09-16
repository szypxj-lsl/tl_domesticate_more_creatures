package com.szypxj.tldomesticatemorecreatures.api.creature;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record TamingFoodInfo(ResourceLocation itemId, int amount, boolean configured) {
    public TamingFoodInfo {
        Objects.requireNonNull(itemId, "itemId");
        amount = Math.max(0, amount);
    }
}
