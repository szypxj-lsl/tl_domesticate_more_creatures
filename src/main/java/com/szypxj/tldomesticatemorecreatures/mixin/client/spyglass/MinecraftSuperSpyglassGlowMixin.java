package com.szypxj.tldomesticatemorecreatures.mixin.client.spyglass;

import com.szypxj.tldomesticatemorecreatures.client.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftSuperSpyglassGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true, require = 1)
    private void tdmc$forceSuperSpyglassGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ClientEvents.shouldForceSuperSpyglassGlow(entity)) {
            cir.setReturnValue(true);
        }
    }
}
