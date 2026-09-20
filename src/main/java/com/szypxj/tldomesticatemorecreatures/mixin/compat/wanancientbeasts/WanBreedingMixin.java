package com.szypxj.tldomesticatemorecreatures.mixin.compat.wanancientbeasts;

import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticItemEntityTransferScope;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchGeneticPayload;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchGeneticsService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.BlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
        "net.wanmine.wab.entity.Charger", "net.wanmine.wab.entity.Crusher", "net.wanmine.wab.entity.Eater",
        "net.wanmine.wab.entity.Glider", "net.wanmine.wab.entity.Raider", "net.wanmine.wab.entity.Soarer",
        "net.wanmine.wab.entity.Surfer", "net.wanmine.wab.entity.Walker"
}, remap = false)
public abstract class WanBreedingMixin {
    @Unique private static final ThreadLocal<Integer> TDMC_SCOPE_DEPTH = ThreadLocal.withInitial(() -> 0);

    @Inject(method = "m_27563_", at = @At("HEAD"), remap = false, require = 0)
    private void tdmc$beginEggGenetics(ServerLevel level, Animal mate, CallbackInfo ci) {
        if (!((Object) this instanceof LivingEntity parent) || mate == null || level.isClientSide) return;
        HatchGeneticPayload payload = HatchGeneticsService.createPayload(parent, mate, parent.getRandom().nextLong());
        if (payload == null) return;
        GeneticItemEntityTransferScope.begin(payload, item -> item.getItem().getItem() instanceof BlockItem blockItem
                && "net.wanmine.wab.block.BeastEggBlock".equals(blockItem.getBlock().getClass().getName()), () -> {});
        TDMC_SCOPE_DEPTH.set(TDMC_SCOPE_DEPTH.get() + 1);
    }

    @Inject(method = "m_27563_", at = @At("RETURN"), remap = false, require = 0)
    private void tdmc$endEggGenetics(ServerLevel level, Animal mate, CallbackInfo ci) {
        int depth = TDMC_SCOPE_DEPTH.get();
        if (depth <= 0) return;
        GeneticItemEntityTransferScope.end();
        if (depth == 1) TDMC_SCOPE_DEPTH.remove(); else TDMC_SCOPE_DEPTH.set(depth - 1);
    }
}
