package com.szypxj.tldomesticatemorecreatures.menu;

import com.szypxj.tldomesticatemorecreatures.inventory.PlayerExtraInventory;
import com.szypxj.tldomesticatemorecreatures.inventory.PlayerExtraInventoryManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public final class ExtraInventoryMenuBridge {
    private ExtraInventoryMenuBridge() {
    }

    public static void attachServer(AbstractContainerMenu menu, ServerPlayer player) {
        if (!shouldAttach(menu, player)) {
            return;
        }
        attach(menu, player, 0, 0, 0, 0);
    }

    public static void attachClient(AbstractContainerMenu menu, Player player, int startX, int startY, int columnSpacing, int rowSpacing) {
        if (!shouldAttach(menu, player)) {
            return;
        }
        attach(menu, player, startX, startY, columnSpacing, rowSpacing);
    }

    public static void positionClient(AbstractContainerMenu menu, int startX, int startY, int columnSpacing, int rowSpacing) {
        if (!(menu instanceof ExtraInventoryMenuAccess access) || !access.tdmc$extraInventoryAttached()) {
            return;
        }
        int start = access.tdmc$extraInventoryStart();
        int end = access.tdmc$extraInventoryEnd();
        if (start < 0 || end <= start || end > menu.slots.size()) {
            return;
        }
        for (int menuIndex = start; menuIndex < end; menuIndex++) {
            int slotIndex = menuIndex - start;
            int column = slotIndex % 3;
            int row = slotIndex / 3;
            Slot slot = menu.slots.get(menuIndex);
            ((MutableSlotPosition) (Object) slot).tdmc$setPosition(
                    startX + column * columnSpacing,
                    startY + row * rowSpacing
            );
        }
    }

    private static boolean shouldAttach(AbstractContainerMenu menu, Player player) {
        if (menu.containerId == 0 || menu instanceof AttributePanelMenu || menu == player.inventoryMenu) {
            return false;
        }
        return menu instanceof ExtraInventoryMenuAccess access && !access.tdmc$extraInventoryAttached();
    }

    private static void attach(AbstractContainerMenu menu, Player player, int startX, int startY, int columnSpacing, int rowSpacing) {
        ExtraInventoryMenuAccess access = (ExtraInventoryMenuAccess) menu;
        PlayerExtraInventory extra = PlayerExtraInventoryManager.get(player);
        int start = menu.slots.size();
        for (int slot = 0; slot < PlayerExtraInventory.SIZE; slot++) {
            int column = slot % 3;
            int row = slot / 3;
            int x = startX + column * columnSpacing;
            int y = startY + row * rowSpacing;
            access.tdmc$addSlot(new Slot(extra, slot, x, y));
        }
        access.tdmc$markExtraInventoryAttached(start, menu.slots.size());
    }
}
