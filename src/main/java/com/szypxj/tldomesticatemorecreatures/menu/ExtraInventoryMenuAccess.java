package com.szypxj.tldomesticatemorecreatures.menu;

import net.minecraft.world.inventory.Slot;

public interface ExtraInventoryMenuAccess {
    Slot tdmc$addSlot(Slot slot);

    boolean tdmc$extraInventoryAttached();

    void tdmc$markExtraInventoryAttached(int start, int end);

    int tdmc$extraInventoryStart();

    int tdmc$extraInventoryEnd();
}
