package com.szypxj.tldomesticatemorecreatures.mixin.compat.saintsdragons;

import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticEggBlockMarker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "com.leon.saintsdragons.common.block.AbstractDragonEggBlockEntity", remap = false)
public abstract class SaintsDragonEggBlockEntityMixin implements GeneticEggBlockMarker {
}
