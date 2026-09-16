package com.szypxj.tldomesticatemorecreatures.mixin.compat.saintsdragons;

import com.szypxj.tldomesticatemorecreatures.game.genetics.CompatEntityPredicates;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticBlockCarrierData;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchGeneticPayload;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchScope;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.leon.saintsdragons.common.block.RaevyxEggBlock", remap = false)
public abstract class SaintsRaevyxEggBlockMixin {
    @Unique
    private static final ThreadLocal<Integer> TDMC_HATCH_SCOPES = ThreadLocal.withInitial(() -> 0);

    @Inject(method = "hatchEgg", at = @At("HEAD"), remap = false, require = 0)
    private void tdmc$beginRaevyxHatch(ServerLevel level, BlockPos pos, BlockState state, CallbackInfo ci) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        HatchGeneticPayload payload = GeneticBlockCarrierData.get(blockEntity).orElse(null);
        if (payload == null) {
            return;
        }

        HatchScope.begin(
                payload,
                child -> CompatEntityPredicates.exact(child, "saintsdragons:raevyx"),
                () -> GeneticBlockCarrierData.clear(blockEntity)
        );
        TDMC_HATCH_SCOPES.set(TDMC_HATCH_SCOPES.get() + 1);
    }

    @Inject(method = "hatchEgg", at = @At("RETURN"), remap = false, require = 0)
    private void tdmc$finishRaevyxHatch(ServerLevel level, BlockPos pos, BlockState state, CallbackInfo ci) {
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
}
