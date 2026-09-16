package com.szypxj.tldomesticatemorecreatures.mixin.talent;

import com.szypxj.tldomesticatemorecreatures.talent.active.ShadowstepSlowRegistry;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobServerAiStepMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void tdmc$throttleShadowstepAi(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (!mob.level().isClientSide && !ShadowstepSlowRegistry.shouldRunActionThisTick(mob)) {
            ci.cancel();
        }
    }
}
