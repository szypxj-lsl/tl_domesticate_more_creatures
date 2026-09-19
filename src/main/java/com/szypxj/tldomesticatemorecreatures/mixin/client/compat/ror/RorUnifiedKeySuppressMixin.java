package com.szypxj.tldomesticatemorecreatures.mixin.client.compat.ror;

import com.szypxj.tldomesticatemorecreatures.client.riding.ClientRideControlState;
import com.szypxj.tldomesticatemorecreatures.compat.ror.RorRideControlProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
        "net.mcreator.raxioresagain.init.RorModKeyMappings$1",
        "net.mcreator.raxioresagain.init.RorModKeyMappings$2",
        "net.mcreator.raxioresagain.init.RorModKeyMappings$4",
        "net.mcreator.raxioresagain.init.RorModKeyMappings$6",
        "net.mcreator.raxioresagain.init.RorModKeyMappings$7"
}, remap = false)
public abstract class RorUnifiedKeySuppressMixin {
    @Inject(method = "m_7249_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void tdmc$suppressLegacyRorRideKeys(boolean down, CallbackInfo ci) {
        if (tdmc$usesUnifiedRorControl()) ci.cancel();
    }

    @Unique
    private static boolean tdmc$usesUnifiedRorControl() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && minecraft.player.getVehicle() instanceof LivingEntity mount
                && RorRideControlProvider.isControllable(mount)
                && ClientRideControlState.activeForCurrentMount(minecraft);
    }
}
