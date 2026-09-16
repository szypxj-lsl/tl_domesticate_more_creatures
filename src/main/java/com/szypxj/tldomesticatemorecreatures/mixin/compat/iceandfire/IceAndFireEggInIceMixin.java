package com.szypxj.tldomesticatemorecreatures.mixin.compat.iceandfire;

import com.szypxj.tldomesticatemorecreatures.game.genetics.CompatEntityPredicates;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticBlockCarrierData;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticEntityTransferScope;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticEggBlockMarker;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchGeneticPayload;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchScope;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.iceandfire.entity.tile.TileEntityEggInIce", remap = false)
public abstract class IceAndFireEggInIceMixin implements GeneticEggBlockMarker {
    @Unique
    private static final ThreadLocal<Integer> TDMC_HATCH_SCOPES = ThreadLocal.withInitial(() -> 0);

    @Unique
    private boolean tdmc$transferScopeActive;

    @Inject(method = "tickEgg", at = @At("HEAD"), remap = false, require = 0)
    private static void tdmc$beginFrozenEggHatch(Level level, BlockPos pos, BlockState state, @Coerce Object blockEntityObject, CallbackInfo ci) {
        if (level.isClientSide || !(blockEntityObject instanceof BlockEntity blockEntity)) {
            return;
        }
        HatchGeneticPayload payload = GeneticBlockCarrierData.get(blockEntity).orElse(null);
        if (payload == null) {
            return;
        }
        HatchScope.begin(
                payload,
                child -> CompatEntityPredicates.namespace(child, "iceandfire"),
                () -> GeneticBlockCarrierData.clear(blockEntity)
        );
        TDMC_HATCH_SCOPES.set(TDMC_HATCH_SCOPES.get() + 1);
    }

    @Inject(method = "tickEgg", at = @At("RETURN"), remap = false, require = 0)
    private static void tdmc$finishFrozenEggHatch(Level level, BlockPos pos, BlockState state, @Coerce Object blockEntityObject, CallbackInfo ci) {
        int depth = TDMC_HATCH_SCOPES.get();
        if (depth <= 0) {
            return;
        }
        HatchScope.end();
        if (depth == 1) {
            TDMC_HATCH_SCOPES.remove();
        } else {
            TDMC_HATCH_SCOPES.set(depth - 1);
        }
    }

    @Inject(method = "spawnEgg", at = @At("HEAD"), remap = false, require = 0)
    private void tdmc$beginFrozenEggTransfer(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        HatchGeneticPayload payload = GeneticBlockCarrierData.get(self).orElse(null);
        if (payload == null) {
            return;
        }
        GeneticEntityTransferScope.begin(
                payload,
                entity -> CompatEntityPredicates.exact(entity, "iceandfire:dragon_egg"),
                () -> GeneticBlockCarrierData.clear(self)
        );
        tdmc$transferScopeActive = true;
    }

    @Inject(method = "spawnEgg", at = @At("RETURN"), remap = false, require = 0)
    private void tdmc$finishFrozenEggTransfer(CallbackInfo ci) {
        if (!tdmc$transferScopeActive) {
            return;
        }
        GeneticEntityTransferScope.end();
        tdmc$transferScopeActive = false;
    }
}
