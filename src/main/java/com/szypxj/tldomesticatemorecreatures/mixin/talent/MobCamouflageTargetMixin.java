package com.szypxj.tldomesticatemorecreatures.mixin.talent;

import com.szypxj.tldomesticatemorecreatures.talent.active.CamouflageService;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobCamouflageTargetMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void tdmc$limitCamouflageTargeting(LivingEntity target, CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (!mob.level().isClientSide && target != null && !CamouflageService.mayAcquireTarget(mob, target)) {
            ci.cancel();
        }
    }

    @Inject(method = "setTarget", at = @At("TAIL"))
    private void tdmc$trackTargetChanges(LivingEntity target, CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (!mob.level().isClientSide) CamouflageService.onMobTargetChanged(mob, target);
    }
}
