package com.szypxj.tldomesticatemorecreatures.mixin.client.riding;

import com.szypxj.tldomesticatemorecreatures.client.riding.RiderVisualHooks;
import com.szypxj.tldomesticatemorecreatures.riding.config.RiderVisualProfile;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies TDMC's configurable body/limb adjustments after vanilla has already
 * built the base riding or standing pose selected in LivingEntityRenderer.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelRidingPoseMixin {
    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At("TAIL"),
            require = 1
    )
    private void tdmc$applyConfiguredRidingPose(
            LivingEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        RiderVisualProfile profile = RiderVisualHooks.profileForRider(entity);
        if (profile == null) {
            return;
        }
        RiderVisualHooks.applyProfilePose((PlayerModel<?>) (Object) this, profile);
    }
}
