package com.szypxj.tldomesticatemorecreatures.api.riding;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

@FunctionalInterface
public interface RideActionGuard {
    boolean allows(Player rider, LivingEntity mount, RideAction action);
}
