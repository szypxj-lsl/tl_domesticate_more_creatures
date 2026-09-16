package com.szypxj.tldomesticatemorecreatures.mixin.compat.ers;

import com.szypxj.tldomesticatemorecreatures.game.genetics.CompatEntityPredicates;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticItemCarrier;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticItemStackCarrierView;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchGeneticPayload;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchScope;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "cn.aurorian.ers.block.be.ArtificialNestBlockEntity", remap = false)
public abstract class ErsArtificialNestMixin implements GeneticItemStackCarrierView {
    @Unique
    private static final ThreadLocal<Integer> TDMC_HATCH_SCOPES = ThreadLocal.withInitial(() -> 0);

    @Override
    public ItemStack tdmc$getGeneticItemStack() {
        return ((ErsArtificialNestAccessor) (Object) this).tdmc$getEgg();
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false, require = 0)
    private static void tdmc$beginHatch(Level level, BlockPos pos, BlockState state, @Coerce Object blockEntityObject, CallbackInfo ci) {
        if (level.isClientSide || !(blockEntityObject instanceof ErsArtificialNestAccessor accessor)) {
            return;
        }
        ItemStack egg = accessor.tdmc$getEgg();
        HatchGeneticPayload payload = GeneticItemCarrier.get(egg).orElse(null);
        if (payload == null) {
            return;
        }
        HatchScope.begin(
                payload,
                child -> CompatEntityPredicates.anyNamespace(child, "ers", "oasis"),
                () -> GeneticItemCarrier.clear(egg)
        );
        TDMC_HATCH_SCOPES.set(TDMC_HATCH_SCOPES.get() + 1);
    }

    @Inject(method = "tick", at = @At("RETURN"), remap = false, require = 0)
    private static void tdmc$finishHatch(Level level, BlockPos pos, BlockState state, @Coerce Object blockEntityObject, CallbackInfo ci) {
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
