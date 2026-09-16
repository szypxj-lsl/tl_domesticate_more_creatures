package com.szypxj.tldomesticatemorecreatures.mixin.compat.ers;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "cn.aurorian.ers.block.be.ArtificialNestBlockEntity", remap = false)
public interface ErsArtificialNestAccessor {
    @Accessor(value = "egg", remap = false)
    ItemStack tdmc$getEgg();
}
