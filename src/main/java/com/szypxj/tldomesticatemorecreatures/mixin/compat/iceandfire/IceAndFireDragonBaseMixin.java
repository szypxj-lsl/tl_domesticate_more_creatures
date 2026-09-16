package com.szypxj.tldomesticatemorecreatures.mixin.compat.iceandfire;

import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticEntityCarrier;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchGeneticPayload;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchGeneticsService;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexthe666.iceandfire.entity.EntityDragonBase", remap = false)
public abstract class IceAndFireDragonBaseMixin {
    @Inject(method = "createEgg", at = @At("RETURN"), remap = false, require = 0)
    private void tdmc$captureEggGenetics(@Coerce Object mateObject, CallbackInfoReturnable<Object> cir) {
        Object selfObject = this;
        Object eggObject = cir.getReturnValue();
        if (!(selfObject instanceof LivingEntity parentA)
                || !(mateObject instanceof LivingEntity parentB)
                || !(eggObject instanceof Entity egg)
                || parentA.level().isClientSide) {
            return;
        }
        HatchGeneticPayload payload = HatchGeneticsService.createPayload(parentA, parentB, parentA.getRandom().nextLong());
        if (payload != null) {
            GeneticEntityCarrier.set(egg, payload);
        }
    }
}
