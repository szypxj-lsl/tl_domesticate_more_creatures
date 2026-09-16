package com.szypxj.tldomesticatemorecreatures.mixin.accessor;

import com.szypxj.tldomesticatemorecreatures.menu.MutableSlotPosition;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Slot.class)
public abstract class SlotPositionMixin implements MutableSlotPosition {
    @Shadow
    @Final
    @Mutable
    public int x;

    @Shadow
    @Final
    @Mutable
    public int y;

    @Override
    public void tdmc$setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
