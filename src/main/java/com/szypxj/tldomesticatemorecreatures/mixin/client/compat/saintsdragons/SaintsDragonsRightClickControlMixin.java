package com.szypxj.tldomesticatemorecreatures.mixin.client.compat.saintsdragons;

import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import com.szypxj.tldomesticatemorecreatures.client.riding.ClientRideControlState;
import com.szypxj.tldomesticatemorecreatures.compat.saintsdragons.SaintsDragonsRideControlProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.leon.saintsdragons.client.input.DragonRideInputHandler", remap = false)
public abstract class SaintsDragonsRightClickControlMixin {
    @Unique
    private static boolean tdmc$restoreUseDown;
    @Unique
    private static boolean tdmc$suppressedUse;

    @Inject(method = "handleControls", at = @At("HEAD"), remap = false, require = 0)
    private static void tdmc$beforeHandleControls(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!tdmc$usesUnifiedAttackMode(minecraft)) return;
        tdmc$restoreUseDown = minecraft.options.keyUse.isDown();
        tdmc$suppressedUse = true;
        minecraft.options.keyUse.setDown(false);
    }

    @Inject(method = "handleControls", at = @At("RETURN"), remap = false, require = 0)
    private static void tdmc$afterHandleControls(CallbackInfo ci) {
        if (!tdmc$suppressedUse) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options != null) {
            minecraft.options.keyUse.setDown(tdmc$restoreUseDown);
        }
        tdmc$suppressedUse = false;
        tdmc$restoreUseDown = false;
    }

    @Unique
    private static boolean tdmc$usesUnifiedAttackMode(Minecraft minecraft) {
        return minecraft.player != null
                && minecraft.player.getVehicle() instanceof LivingEntity mount
                && SaintsDragonsRideControlProvider.isRideableDragon(mount)
                && ClientRideControlState.activeForCurrentMount(minecraft)
                && ClientRideControlState.supports(RideAction.SECONDARY_ATTACK);
    }
}
