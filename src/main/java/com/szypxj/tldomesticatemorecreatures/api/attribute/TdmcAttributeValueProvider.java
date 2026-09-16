package com.szypxj.tldomesticatemorecreatures.api.attribute;

import net.minecraft.world.entity.LivingEntity;

@FunctionalInterface
public interface TdmcAttributeValueProvider {
    TdmcAttributeValue read(LivingEntity entity);
}
