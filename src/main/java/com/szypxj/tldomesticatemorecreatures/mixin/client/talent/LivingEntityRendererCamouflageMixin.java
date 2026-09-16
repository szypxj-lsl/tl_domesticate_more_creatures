package com.szypxj.tldomesticatemorecreatures.mixin.client.talent;

import com.szypxj.tldomesticatemorecreatures.client.talent.CamouflageRenderHooks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererCamouflageMixin {
    @Unique
    private static final ThreadLocal<Float> TDMC_CAMOUFLAGE_ALPHA = ThreadLocal.withInitial(() -> 1.0F);

    @Inject(method = "getRenderType", at = @At("RETURN"), cancellable = true)
    private void tdmc$useCamouflageRenderType(LivingEntity entity, boolean bodyVisible, boolean translucent, boolean glowing,
                                               CallbackInfoReturnable<RenderType> cir) {
        float alpha = CamouflageRenderHooks.renderAlpha(entity);
        if (bodyVisible && alpha < 0.999F) {
            @SuppressWarnings("rawtypes")
            LivingEntityRenderer renderer = (LivingEntityRenderer) (Object) this;
            ResourceLocation texture = renderer.getTextureLocation(entity);
            cir.setReturnValue(RenderType.entityTranslucent(texture));
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void tdmc$captureCamouflageAlpha(LivingEntity entity, float entityYaw, float partialTicks,
                                               com.mojang.blaze3d.vertex.PoseStack poseStack,
                                               MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        TDMC_CAMOUFLAGE_ALPHA.set(CamouflageRenderHooks.renderAlpha(entity));
    }

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"
            ),
            index = 7
    )
    private float tdmc$applyCamouflageModelAlpha(float originalAlpha) {
        return originalAlpha * TDMC_CAMOUFLAGE_ALPHA.get();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void tdmc$clearCamouflageAlpha(LivingEntity entity, float entityYaw, float partialTicks,
                                            com.mojang.blaze3d.vertex.PoseStack poseStack,
                                            MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        TDMC_CAMOUFLAGE_ALPHA.remove();
    }
}
