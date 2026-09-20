package com.szypxj.tldomesticatemorecreatures.mixin.compat.fossil;

import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticEntityTransferScope;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticItemCarrier;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchGeneticPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.teamfossilsarcheology.fossil.item.BirdEggItem", remap = false)
public abstract class FossilBirdEggItemMixin {
    @Unique private static final ThreadLocal<Integer> TDMC_SCOPE_DEPTH = ThreadLocal.withInitial(() -> 0);

    @Inject(method = "m_7203_", at = @At("HEAD"), remap = false, require = 0)
    private void tdmc$beginThrow(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = player.getItemInHand(hand);
        HatchGeneticPayload payload = GeneticItemCarrier.get(stack).orElse(null);
        if (payload == null) return;
        GeneticEntityTransferScope.begin(payload,
                entity -> "com.github.teamfossilsarcheology.fossil.entity.ThrownBirdEgg".equals(entity.getClass().getName()),
                () -> GeneticItemCarrier.clear(stack));
        TDMC_SCOPE_DEPTH.set(TDMC_SCOPE_DEPTH.get() + 1);
    }

    @Inject(method = "m_7203_", at = @At("RETURN"), remap = false, require = 0)
    private void tdmc$endThrow(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        int depth = TDMC_SCOPE_DEPTH.get();
        if (depth <= 0) return;
        GeneticEntityTransferScope.end();
        if (depth == 1) TDMC_SCOPE_DEPTH.remove(); else TDMC_SCOPE_DEPTH.set(depth - 1);
    }
}
