package com.szypxj.tldomesticatemorecreatures.mixin.compat.saintsdragons;

import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticBlockCarrierData;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchGeneticPayload;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchGeneticsService;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.leon.saintsdragons.server.entity.base.DragonEntity", remap = false)
public abstract class SaintsDragonEntityMixin {
    @Inject(method = "configureEggBlockEntity", at = @At("TAIL"), remap = false, require = 0)
    private void tdmc$captureEggGenetics(BlockEntity blockEntity, @Coerce Object mateObject, CallbackInfo ci) {
        Object selfObject = this;
        if (!(selfObject instanceof LivingEntity parentA)
                || !(mateObject instanceof LivingEntity parentB)
                || parentA.level().isClientSide) {
            return;
        }
        HatchGeneticPayload payload = HatchGeneticsService.createPayload(parentA, parentB, parentA.getRandom().nextLong());
        if (payload != null) {
            GeneticBlockCarrierData.set(blockEntity, payload);
        }
    }
}
