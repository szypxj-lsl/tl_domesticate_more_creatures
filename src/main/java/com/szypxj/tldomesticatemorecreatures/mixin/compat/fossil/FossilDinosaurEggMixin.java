package com.szypxj.tldomesticatemorecreatures.mixin.compat.fossil;

import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticEntityCarrier;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchGeneticPayload;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchScope;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.DinosaurEgg", remap = false)
public abstract class FossilDinosaurEggMixin {
    @Unique private static final ThreadLocal<Integer> TDMC_SCOPE_DEPTH = ThreadLocal.withInitial(() -> 0);

    @Inject(method = "m_8119_", at = @At("HEAD"), remap = false, require = 0)
    private void tdmc$beginDinosaurEggTick(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        HatchGeneticPayload payload = GeneticEntityCarrier.get(self).orElse(null);
        if (payload == null || self.level().isClientSide) return;
        HatchScope.begin(payload, child -> {
            var id = ForgeRegistries.ENTITY_TYPES.getKey(child.getType());
            return id != null && "fossil".equals(id.getNamespace()) && !"dinosaur_egg".equals(id.getPath());
        }, () -> GeneticEntityCarrier.clear(self));
        TDMC_SCOPE_DEPTH.set(TDMC_SCOPE_DEPTH.get() + 1);
    }

    @Inject(method = "m_8119_", at = @At("RETURN"), remap = false, require = 0)
    private void tdmc$endDinosaurEggTick(CallbackInfo ci) {
        int depth = TDMC_SCOPE_DEPTH.get();
        if (depth <= 0) return;
        HatchScope.end();
        if (depth == 1) TDMC_SCOPE_DEPTH.remove(); else TDMC_SCOPE_DEPTH.set(depth - 1);
    }
}
