package com.szypxj.tldomesticatemorecreatures.mixin.compat.fossil;

import com.szypxj.tldomesticatemorecreatures.game.genetics.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.Prehistoric", remap = false)
public abstract class FossilPrehistoricBreedingMixin {
    @Unique private static final ThreadLocal<Integer> TDMC_SCOPE_DEPTH = ThreadLocal.withInitial(() -> 0);

    @Inject(method = "procreate", at = @At("HEAD"), remap = false, require = 0)
    private void tdmc$beginProcreate(@Coerce Object mateObject, CallbackInfo ci) {
        if (!((Object) this instanceof LivingEntity parentA) || !(mateObject instanceof LivingEntity parentB) || parentA.level().isClientSide) return;
        HatchGeneticPayload payload = HatchGeneticsService.createPayload(parentA, parentB, parentA.getRandom().nextLong());
        if (payload == null) return;
        GeneticItemEntityTransferScope.begin(payload, FossilPrehistoricBreedingMixin::isFossilEggItemEntity, () -> {});
        GeneticEntityTransferScope.begin(payload, entity -> {
            var id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            return id != null && "fossil:dinosaur_egg".equals(id.toString());
        }, () -> {});
        HatchScope.begin(payload, child -> {
            var id = ForgeRegistries.ENTITY_TYPES.getKey(child.getType());
            return id != null && "fossil".equals(id.getNamespace()) && !"dinosaur_egg".equals(id.getPath());
        }, () -> {});
        TDMC_SCOPE_DEPTH.set(TDMC_SCOPE_DEPTH.get() + 1);
    }

    @Inject(method = "procreate", at = @At("RETURN"), remap = false, require = 0)
    private void tdmc$endProcreate(@Coerce Object mateObject, CallbackInfo ci) {
        int depth = TDMC_SCOPE_DEPTH.get();
        if (depth <= 0) return;
        HatchScope.end();
        GeneticEntityTransferScope.end();
        GeneticItemEntityTransferScope.end();
        if (depth == 1) TDMC_SCOPE_DEPTH.remove(); else TDMC_SCOPE_DEPTH.set(depth - 1);
    }

    @Unique
    private static boolean isFossilEggItemEntity(ItemEntity item) {
        var id = ForgeRegistries.ITEMS.getKey(item.getItem().getItem());
        return id != null && "fossil".equals(id.getNamespace()) && id.getPath().contains("egg");
    }
}
