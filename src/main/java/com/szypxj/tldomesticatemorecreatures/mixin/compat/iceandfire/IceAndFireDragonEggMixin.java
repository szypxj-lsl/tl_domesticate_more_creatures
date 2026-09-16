package com.szypxj.tldomesticatemorecreatures.mixin.compat.iceandfire;

import com.szypxj.tldomesticatemorecreatures.game.genetics.CompatEntityPredicates;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticBlockCarrierData;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticCarrierEntityMarker;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticEntityCarrier;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticItemEntityTransferScope;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchGeneticPayload;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchScope;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexthe666.iceandfire.entity.EntityDragonEgg", remap = false)
public abstract class IceAndFireDragonEggMixin implements GeneticCarrierEntityMarker {
    @Unique
    private boolean tdmc$hatchScopeActive;

    @Unique
    private boolean tdmc$itemDropScopeActive;

    @Inject(method = "updateEggCondition", at = @At("HEAD"), remap = false, require = 0)
    private void tdmc$beginHatchScope(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide) {
            return;
        }
        HatchGeneticPayload payload = GeneticEntityCarrier.get(self).orElse(null);
        if (payload == null) {
            return;
        }
        HatchScope.begin(
                payload,
                child -> CompatEntityPredicates.namespace(child, "iceandfire") && child != self,
                () -> GeneticEntityCarrier.clear(self)
        );
        tdmc$hatchScopeActive = true;
    }

    @Inject(method = "updateEggCondition", at = @At("RETURN"), remap = false, require = 0)
    private void tdmc$finishHatchScope(CallbackInfo ci) {
        if (!tdmc$hatchScopeActive) {
            return;
        }
        Entity self = (Entity) (Object) this;
        HatchScope.end();
        tdmc$hatchScopeActive = false;

        HatchGeneticPayload payload = GeneticEntityCarrier.get(self).orElse(null);
        if (payload == null || !self.isRemoved()) {
            return;
        }
        BlockEntity blockEntity = self.level().getBlockEntity(self.blockPosition());
        if (blockEntity != null) {
            GeneticBlockCarrierData.set(blockEntity, payload);
            GeneticEntityCarrier.clear(self);
        }
    }

    @Inject(method = "m_6469_", at = @At("HEAD"), remap = false, require = 0)
    private void tdmc$beginDroppedEggTransfer(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide) {
            return;
        }
        HatchGeneticPayload payload = GeneticEntityCarrier.get(self).orElse(null);
        if (payload == null) {
            return;
        }
        GeneticItemEntityTransferScope.begin(
                payload,
                itemEntity -> itemEntity.getItem().getItem().getClass().getName().equals("com.github.alexthe666.iceandfire.item.ItemDragonEgg"),
                () -> GeneticEntityCarrier.clear(self)
        );
        tdmc$itemDropScopeActive = true;
    }

    @Inject(method = "m_6469_", at = @At("RETURN"), remap = false, require = 0)
    private void tdmc$finishDroppedEggTransfer(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!tdmc$itemDropScopeActive) {
            return;
        }
        GeneticItemEntityTransferScope.end();
        tdmc$itemDropScopeActive = false;
    }
}
