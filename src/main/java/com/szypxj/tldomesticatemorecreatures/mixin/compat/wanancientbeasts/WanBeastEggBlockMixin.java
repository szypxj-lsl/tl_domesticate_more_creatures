package com.szypxj.tldomesticatemorecreatures.mixin.compat.wanancientbeasts;

import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticBlockPosStore;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchGeneticPayload;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchScope;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.wanmine.wab.block.BeastEggBlock", remap = false)
public abstract class WanBeastEggBlockMixin {
    @Unique private static final ThreadLocal<Integer> TDMC_SCOPE_DEPTH = ThreadLocal.withInitial(() -> 0);

    @Inject(method = "m_213897_", at = @At("HEAD"), remap = false, require = 0)
    private void tdmc$beginHatch(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        HatchGeneticPayload payload = GeneticBlockPosStore.get(level).get(pos).orElse(null);
        if (payload == null) return;
        HatchScope.begin(payload, child -> {
            var id = ForgeRegistries.ENTITY_TYPES.getKey(child.getType());
            return id != null && payload.childEntityId().equals(id.toString());
        }, () -> GeneticBlockPosStore.get(level).remove(pos));
        TDMC_SCOPE_DEPTH.set(TDMC_SCOPE_DEPTH.get() + 1);
    }

    @Inject(method = "m_213897_", at = @At("RETURN"), remap = false, require = 0)
    private void tdmc$endHatch(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        int depth = TDMC_SCOPE_DEPTH.get();
        if (depth <= 0) return;
        HatchScope.end();
        if (depth == 1) TDMC_SCOPE_DEPTH.remove(); else TDMC_SCOPE_DEPTH.set(depth - 1);
    }
}
