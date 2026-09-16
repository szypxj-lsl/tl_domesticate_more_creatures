package com.szypxj.tldomesticatemorecreatures.mixin.compat.tlmarking;

import com.szypxj.tldomesticatemorecreatures.compat.tlmarking.TlMarkingClientCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Pseudo
@Mixin(targets = "com.szypxj.tlmarking.input.PingKeyHandler", remap = false)
public abstract class PingKeyHandlerMixin {
    @Inject(
            method = "handleSinglePress(Lcom/szypxj/tlmarking/input/PingKeyHandler$TargetHit;Ljava/util/UUID;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private static void tdmc$handleConfirmedSingle(@Coerce Object rawTarget, UUID selectedPingId, CallbackInfo ci) {
        if (TlMarkingClientCompat.handleConfirmedSingle(rawTarget, selectedPingId)) {
            ci.cancel();
        }
    }
}
