package com.szypxj.tldomesticatemorecreatures.api.riding;

import net.minecraft.world.entity.LivingEntity;

public interface RideStateProvider {
    boolean supports(LivingEntity entity);

    RideStateSnapshot snapshot(LivingEntity entity);
}
