package com.szypxj.tldomesticatemorecreatures.mixin.client.riding;

import com.szypxj.tldomesticatemorecreatures.client.riding.RideInputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerDismountInputMixin {
    @Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
    private void tdmc$keepRidingWhileDescending(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this != Minecraft.getInstance().player) {
            return;
        }
        if (RideInputHandler.consumesDescendKey()) {
            cir.setReturnValue(false);
        }
    }
}
