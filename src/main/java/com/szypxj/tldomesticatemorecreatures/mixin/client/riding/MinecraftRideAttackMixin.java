package com.szypxj.tldomesticatemorecreatures.mixin.client.riding;

import com.szypxj.tldomesticatemorecreatures.client.riding.RideAttackClientHandler;
import com.szypxj.tldomesticatemorecreatures.client.riding.RideControlClientHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftRideAttackMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void tdmc$redirectRideAttack(CallbackInfoReturnable<Boolean> cir) {
        if (RideAttackClientHandler.tryHandleAttack()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void tdmc$redirectRideSecondaryAttack(CallbackInfo ci) {
        if (RideControlClientHandler.tryConsumeSecondaryAttack()) {
            ci.cancel();
        }
    }
}
