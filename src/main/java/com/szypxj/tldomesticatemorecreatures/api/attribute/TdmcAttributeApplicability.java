package com.szypxj.tldomesticatemorecreatures.api.attribute;

import net.minecraft.world.entity.LivingEntity;

@FunctionalInterface
public interface TdmcAttributeApplicability {
    TdmcAttributeApplicability ALL = entity -> true;

    boolean appliesTo(LivingEntity entity);
}
