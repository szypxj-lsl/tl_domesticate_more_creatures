package com.szypxj.tldomesticatemorecreatures.mixin.riding;

import com.szypxj.tldomesticatemorecreatures.riding.control.RidePhysicsHooks;
import com.szypxj.tldomesticatemorecreatures.talent.active.ActiveTalentService;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRideTravelMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void tdmc$beforeRideTravel(Vec3 input, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.level().isClientSide && ActiveTalentService.blocksRideInput(entity)) {
            entity.setDeltaMovement(Vec3.ZERO);
            ci.cancel();
            return;
        }
        RidePhysicsHooks.beforeTravel(entity);
    }

    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3 tdmc$overrideRideTravelVector(Vec3 input) {
        return RidePhysicsHooks.overrideTravelVector((LivingEntity) (Object) this, input);
    }

}
