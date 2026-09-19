package com.szypxj.tldomesticatemorecreatures.mixin.client.compat.ers;

import com.szypxj.tldomesticatemorecreatures.client.riding.ClientRideControlState;
import com.szypxj.tldomesticatemorecreatures.compat.ers.ErsRideBridge;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "cn.aurorian.ers.client.ClientForgeListener", remap = false)
public abstract class ErsUnifiedControlClientMixin {
    @Inject(method = "onKeyInput", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void tdmc$suppressNativeAttackKeys(InputEvent.Key event, CallbackInfo ci) {
        if (tdmc$usesUnifiedErsControl()) ci.cancel();
    }

    @Inject(method = "onDive", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void tdmc$suppressNativeDiveKey(TickEvent.ClientTickEvent event, CallbackInfo ci) {
        if (tdmc$usesUnifiedErsControl()) ci.cancel();
    }

    @Inject(method = "onFlightControl", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void tdmc$suppressNativeFlightKeys(TickEvent.ClientTickEvent event, CallbackInfo ci) {
        if (tdmc$usesUnifiedErsControl()) ci.cancel();
    }

    @Unique
    private static boolean tdmc$usesUnifiedErsControl() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && minecraft.player.getVehicle() instanceof ErsRideBridge
                && ClientRideControlState.activeForCurrentMount(minecraft);
    }
}
