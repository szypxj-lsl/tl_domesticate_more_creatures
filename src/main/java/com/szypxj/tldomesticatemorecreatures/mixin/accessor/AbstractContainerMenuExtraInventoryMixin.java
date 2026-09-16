package com.szypxj.tldomesticatemorecreatures.mixin.accessor;

import com.szypxj.tldomesticatemorecreatures.menu.ExtraInventoryMenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuExtraInventoryMixin implements ExtraInventoryMenuAccess {
    @Shadow
    protected abstract Slot addSlot(Slot slot);

    @Unique
    private boolean tdmc$extraInventoryAttached;
    @Unique
    private int tdmc$extraInventoryStart = -1;
    @Unique
    private int tdmc$extraInventoryEnd = -1;

    @Override
    public Slot tdmc$addSlot(Slot slot) {
        return addSlot(slot);
    }

    @Override
    public boolean tdmc$extraInventoryAttached() {
        return tdmc$extraInventoryAttached;
    }

    @Override
    public void tdmc$markExtraInventoryAttached(int start, int end) {
        tdmc$extraInventoryAttached = true;
        tdmc$extraInventoryStart = start;
        tdmc$extraInventoryEnd = end;
    }

    @Override
    public int tdmc$extraInventoryStart() {
        return tdmc$extraInventoryStart;
    }

    @Override
    public int tdmc$extraInventoryEnd() {
        return tdmc$extraInventoryEnd;
    }
}
