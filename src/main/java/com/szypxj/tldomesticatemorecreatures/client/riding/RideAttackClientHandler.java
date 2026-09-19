package com.szypxj.tldomesticatemorecreatures.client.riding;

import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import net.minecraft.client.Minecraft;

/** Compatibility facade kept for the existing Minecraft attack mixin. */
public final class RideAttackClientHandler {
    private RideAttackClientHandler() {
    }

    public static boolean tryHandleAttack() {
        return RideControlClientHandler.tryConsumePrimaryAttack();
    }

    public static boolean isControlledPetRide(Minecraft minecraft) {
        return RideControlClientHandler.isControlledRide(minecraft)
                && ClientRideControlState.supports(RideAction.PRIMARY_ATTACK);
    }
}
