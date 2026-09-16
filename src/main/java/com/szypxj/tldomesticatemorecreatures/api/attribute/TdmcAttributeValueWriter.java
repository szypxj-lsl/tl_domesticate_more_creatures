package com.szypxj.tldomesticatemorecreatures.api.attribute;

import net.minecraft.world.entity.LivingEntity;

@FunctionalInterface
public interface TdmcAttributeValueWriter {
    boolean write(LivingEntity entity, double value);
}
