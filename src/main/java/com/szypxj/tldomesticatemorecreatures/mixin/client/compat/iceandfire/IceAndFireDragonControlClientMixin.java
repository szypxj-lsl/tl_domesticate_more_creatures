package com.szypxj.tldomesticatemorecreatures.mixin.client.compat.iceandfire;

import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import com.szypxj.tldomesticatemorecreatures.client.riding.ClientRideControlState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents Ice and Fire's own client key poller from clearing TDMC-controlled
 * rider state bits while the unified profile is active. Dismount remains native.
 */
@Pseudo
@Mixin(targets = "com.github.alexthe666.iceandfire.entity.EntityDragonBase", remap = false)
public abstract class IceAndFireDragonControlClientMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void tdmc$suppressNativeAttackKey(boolean active, CallbackInfo ci) {
        if (tdmc$usesUnifiedControl(RideAction.PRIMARY_ATTACK)) {
            ci.cancel();
        }
    }

    @Inject(method = "strike", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void tdmc$suppressNativeBreathKey(boolean active, CallbackInfo ci) {
        if (tdmc$usesUnifiedControl(RideAction.SECONDARY_ATTACK)) {
            ci.cancel();
        }
    }

    @Inject(method = "up", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void tdmc$suppressNativeUpKey(boolean active, CallbackInfo ci) {
        if (tdmc$usesUnifiedControl(RideAction.MOVEMENT_SPECIAL)) {
            ci.cancel();
        }
    }

    @Inject(method = "down", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void tdmc$suppressNativeDownKey(boolean active, CallbackInfo ci) {
        if (tdmc$usesUnifiedControl(RideAction.UTILITY)) {
            ci.cancel();
        }
    }

    @Unique
    private boolean tdmc$usesUnifiedControl(RideAction action) {
        Object self = this;
        if (!(self instanceof LivingEntity mount)) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && minecraft.player.getVehicle() == mount
                && ClientRideControlState.activeForCurrentMount(minecraft)
                && ClientRideControlState.supports(action);
    }
}
