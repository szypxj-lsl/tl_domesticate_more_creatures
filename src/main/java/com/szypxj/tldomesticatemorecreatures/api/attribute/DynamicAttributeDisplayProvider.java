package com.szypxj.tldomesticatemorecreatures.api.attribute;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

@FunctionalInterface
public interface DynamicAttributeDisplayProvider {
    DynamicAttributeDisplayValue get(ServerPlayer viewer, LivingEntity target);
}
