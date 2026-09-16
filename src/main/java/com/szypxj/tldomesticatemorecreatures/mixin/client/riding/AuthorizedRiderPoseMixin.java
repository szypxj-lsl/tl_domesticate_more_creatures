package com.szypxj.tldomesticatemorecreatures.mixin.client.riding;

import com.szypxj.tldomesticatemorecreatures.client.riding.RiderVisualHooks;
import com.szypxj.tldomesticatemorecreatures.riding.config.RiderVisualProfile;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Salvation-style riding pose selection. LivingEntityRenderer computes the
 * boolean that becomes model.riding; replacing that value here is earlier and
 * more reliable than trying to reconstruct the whole sitting pose afterwards.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class AuthorizedRiderPoseMixin {
    @ModifyVariable(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("STORE"),
            ordinal = 0,
            require = 1
    )
    private boolean tdmc$applyAuthorizedPose(boolean original, LivingEntity rendered) {
        RiderVisualProfile profile = RiderVisualHooks.profileForRider(rendered);
        if (profile == null) {
            return original;
        }
        return RiderVisualHooks.usesSittingBase(profile);
    }
}
