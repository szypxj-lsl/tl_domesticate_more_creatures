package com.szypxj.tldomesticatemorecreatures.mixin.client.riding;

import com.szypxj.tldomesticatemorecreatures.client.riding.RiderVisualHooks;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraRidingOffsetMixin {
    @Shadow private Vec3 position;
    @Shadow protected abstract void setPosition(Vec3 position);

    @Inject(
            method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            at = @At("RETURN")
    )
    private void tdmc$applyRideCameraOffset(
            BlockGetter level,
            Entity cameraEntity,
            boolean detached,
            boolean mirrored,
            float partialTick,
            CallbackInfo ci
    ) {
        RiderVisualHooks.RideCameraContext context = RiderVisualHooks.cameraContext(cameraEntity);
        if (context == null) {
            return;
        }

        Vec3 configuredOffset = RiderVisualHooks.cameraOffset(context.mount());
        if (!detached) {
            setPosition(RiderVisualHooks.riderEyePosition(context.rider(), partialTick).add(configuredOffset));
            return;
        }

        Vec3 cameraAnchorCorrection = RiderVisualHooks.cameraAnchorCorrection(cameraEntity, context.rider(), partialTick);
        setPosition(this.position.add(cameraAnchorCorrection).add(configuredOffset));
    }
}
