package com.szypxj.tldomesticatemorecreatures.mixin.compat.fossil;

import com.szypxj.tldomesticatemorecreatures.game.genetics.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.teamfossilsarcheology.fossil.item.EggItem", remap = false)
public abstract class FossilEggItemMixin {
    @Unique private static final ThreadLocal<Integer> TDMC_SCOPE_DEPTH = ThreadLocal.withInitial(() -> 0);
    @Unique private static final ThreadLocal<ItemStack> TDMC_SOURCE_STACK = new ThreadLocal<>();

    @Inject(method = "m_6225_", at = @At("HEAD"), remap = false, require = 0)
    private void tdmc$beginUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) { tdmc$begin(context.getItemInHand()); }
    @Inject(method = "m_6225_", at = @At("RETURN"), remap = false, require = 0)
    private void tdmc$endUseOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) { tdmc$end(); }

    @Inject(method = "m_7203_", at = @At("HEAD"), remap = false, require = 0)
    private void tdmc$beginUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) { tdmc$begin(player.getItemInHand(hand)); }
    @Inject(method = "m_7203_", at = @At("RETURN"), remap = false, require = 0)
    private void tdmc$endUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) { tdmc$end(); }

    @Unique private static void tdmc$begin(ItemStack stack) {
        HatchGeneticPayload payload = GeneticItemCarrier.get(stack).orElse(null);
        if (payload == null) return;
        TDMC_SOURCE_STACK.set(stack);
        Runnable consume = () -> GeneticItemCarrier.clear(stack);
        GeneticEntityTransferScope.begin(payload, entity -> {
            var id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            return id != null && "fossil:dinosaur_egg".equals(id.toString());
        }, consume);
        HatchScope.begin(payload, child -> {
            var id = ForgeRegistries.ENTITY_TYPES.getKey(child.getType());
            return id != null && "fossil".equals(id.getNamespace()) && !"dinosaur_egg".equals(id.getPath());
        }, consume);
        TDMC_SCOPE_DEPTH.set(TDMC_SCOPE_DEPTH.get() + 1);
    }

    @Unique private static void tdmc$end() {
        int depth = TDMC_SCOPE_DEPTH.get();
        if (depth <= 0) return;
        HatchScope.end();
        GeneticEntityTransferScope.end();
        TDMC_SOURCE_STACK.remove();
        if (depth == 1) TDMC_SCOPE_DEPTH.remove(); else TDMC_SCOPE_DEPTH.set(depth - 1);
    }
}
