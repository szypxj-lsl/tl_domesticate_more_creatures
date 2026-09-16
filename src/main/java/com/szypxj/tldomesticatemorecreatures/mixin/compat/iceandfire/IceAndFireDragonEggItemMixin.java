package com.szypxj.tldomesticatemorecreatures.mixin.compat.iceandfire;

import com.szypxj.tldomesticatemorecreatures.game.genetics.CompatEntityPredicates;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticEntityTransferScope;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticItemCarrier;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchGeneticPayload;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexthe666.iceandfire.item.ItemDragonEgg", remap = false)
public abstract class IceAndFireDragonEggItemMixin {
    @Unique
    private static final ThreadLocal<Integer> TDMC_TRANSFER_SCOPES = ThreadLocal.withInitial(() -> 0);

    @Inject(method = "m_6225_", at = @At("HEAD"), remap = false, require = 0)
    private void tdmc$beginPlacedEggTransfer(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel().isClientSide) {
            return;
        }
        ItemStack stack = context.getItemInHand();
        HatchGeneticPayload payload = GeneticItemCarrier.get(stack).orElse(null);
        if (payload == null) {
            return;
        }
        GeneticEntityTransferScope.begin(
                payload,
                entity -> CompatEntityPredicates.exact(entity, "iceandfire:dragon_egg"),
                () -> GeneticItemCarrier.clear(stack)
        );
        TDMC_TRANSFER_SCOPES.set(TDMC_TRANSFER_SCOPES.get() + 1);
    }

    @Inject(method = "m_6225_", at = @At("RETURN"), remap = false, require = 0)
    private void tdmc$finishPlacedEggTransfer(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        int depth = TDMC_TRANSFER_SCOPES.get();
        if (depth <= 0) {
            return;
        }
        GeneticEntityTransferScope.end();
        if (depth == 1) {
            TDMC_TRANSFER_SCOPES.remove();
        } else {
            TDMC_TRANSFER_SCOPES.set(depth - 1);
        }
    }
}
