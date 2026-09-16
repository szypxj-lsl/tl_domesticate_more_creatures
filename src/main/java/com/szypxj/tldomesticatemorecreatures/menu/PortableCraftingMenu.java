package com.szypxj.tldomesticatemorecreatures.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;

public final class PortableCraftingMenu extends CraftingMenu {
    public PortableCraftingMenu(int containerId, Inventory inventory) {
        super(containerId, inventory, ContainerLevelAccess.create(inventory.player.level(), inventory.player.blockPosition()));
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
