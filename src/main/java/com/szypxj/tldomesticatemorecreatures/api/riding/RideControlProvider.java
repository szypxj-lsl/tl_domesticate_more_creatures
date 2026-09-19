package com.szypxj.tldomesticatemorecreatures.api.riding;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public interface RideControlProvider {
    default int priority() {
        return 0;
    }

    boolean supports(LivingEntity mount);

    RideCapabilities capabilities(LivingEntity mount);

    default RideActionInfo actionInfo(LivingEntity mount, RideAction action) {
        return null;
    }

    default RideActionStatus actionStatus(ServerPlayer rider, LivingEntity mount, RideAction action) {
        return RideActionStatus.READY;
    }

    RideActionResult execute(RideActionContext context, RideAction action);
}
