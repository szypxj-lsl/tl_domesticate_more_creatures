package com.szypxj.tldomesticatemorecreatures.domestication;

import net.minecraft.resources.ResourceLocation;

public record TamingFood(ResourceLocation itemId, int amount) {
    public TamingFood {
        amount = Math.max(1, amount);
    }
}
