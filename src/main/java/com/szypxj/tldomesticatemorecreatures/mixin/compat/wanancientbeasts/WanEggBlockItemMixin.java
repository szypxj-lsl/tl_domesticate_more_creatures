package com.szypxj.tldomesticatemorecreatures.mixin.compat.wanancientbeasts;

import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticBlockPosStore;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticItemCarrier;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchGeneticPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class WanEggBlockItemMixin {
    @Unique private static final ThreadLocal<Boolean> TDMC_WAN_PLACEMENT = new ThreadLocal<>();
    @Unique private static final ThreadLocal<HatchGeneticPayload> TDMC_WAN_PAYLOAD = new ThreadLocal<>();

    @Inject(method = "place", at = @At("HEAD"))
    private void tdmc$captureWanEggPayload(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        TDMC_WAN_PLACEMENT.remove();
        TDMC_WAN_PAYLOAD.remove();
        Block block = ((BlockItem) (Object) this).getBlock();
        if (!"net.wanmine.wab.block.BeastEggBlock".equals(block.getClass().getName())) return;
        TDMC_WAN_PLACEMENT.set(Boolean.TRUE);
        GeneticItemCarrier.get(context.getItemInHand()).ifPresent(TDMC_WAN_PAYLOAD::set);
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void tdmc$storeWanEggPayload(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        boolean wanPlacement = Boolean.TRUE.equals(TDMC_WAN_PLACEMENT.get());
        HatchGeneticPayload payload = TDMC_WAN_PAYLOAD.get();
        TDMC_WAN_PLACEMENT.remove();
        TDMC_WAN_PAYLOAD.remove();
        if (!wanPlacement || !cir.getReturnValue().consumesAction() || !(context.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = context.getClickedPos();
        if (!"net.wanmine.wab.block.BeastEggBlock".equals(level.getBlockState(pos).getBlock().getClass().getName())) return;
        GeneticBlockPosStore store = GeneticBlockPosStore.get(level);
        store.remove(pos);
        if (payload != null) {
            store.put(pos, payload);
            GeneticItemCarrier.clear(context.getItemInHand());
        }
    }
}
