package com.szypxj.tldomesticatemorecreatures.mixin.client.riding;

import com.mojang.blaze3d.vertex.PoseStack;
import com.szypxj.tldomesticatemorecreatures.client.riding.RiderVisualHooks;
import com.szypxj.tldomesticatemorecreatures.riding.config.RiderVisualProfile;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRidingScaleMixin {
    @Unique
    private boolean tdmc$rideScalePushed;

    @Inject(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            require = 1
    )
    private void tdmc$beginRideRender(
            AbstractClientPlayer player,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo ci
    ) {
        tdmc$rideScalePushed = false;
        RiderVisualProfile profile = RiderVisualHooks.profileForRider(player);
        if (profile == null) {
            return;
        }
        float scale = profile.visualPlayerScale();
        if (Math.abs(scale - 1.0F) < 0.0001F) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        tdmc$rideScalePushed = true;
    }

    @Inject(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"),
            require = 1
    )
    private void tdmc$endRideRender(
            AbstractClientPlayer player,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo ci
    ) {
        if (tdmc$rideScalePushed) {
            poseStack.popPose();
            tdmc$rideScalePushed = false;
        }
    }
}
