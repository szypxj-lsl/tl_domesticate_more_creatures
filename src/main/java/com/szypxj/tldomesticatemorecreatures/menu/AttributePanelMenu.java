package com.szypxj.tldomesticatemorecreatures.menu;

import com.szypxj.tldomesticatemorecreatures.equipment.PetEquipmentSlotDefinition;
import com.szypxj.tldomesticatemorecreatures.backpack.PetBackpackContainer;
import com.szypxj.tldomesticatemorecreatures.backpack.PetBackpackService;
import com.szypxj.tldomesticatemorecreatures.inventory.PlayerExtraInventory;
import com.szypxj.tldomesticatemorecreatures.inventory.PlayerExtraInventoryManager;
import com.szypxj.tldomesticatemorecreatures.network.PanelSnapshot;
import com.szypxj.tldomesticatemorecreatures.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.List;

public final class AttributePanelMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 520;
    public static final int IMAGE_HEIGHT = 300;
    public static final int INVENTORY_X = 18;
    public static final int INVENTORY_Y = 76;
    public static final int INVENTORY_COLUMNS = 6;
    public static final int VANILLA_INVENTORY_COLUMNS = 3;
    public static final int EXTRA_INVENTORY_COLUMNS = 3;
    public static final int INVENTORY_ROWS = 9;
    public static final int INVENTORY_COLUMN_SPACING = 22;
    public static final int INVENTORY_ROW_SPACING = 22;
    public static final int HOTBAR_X = 160;
    public static final int HOTBAR_Y = 76;
    public static final int EQUIPMENT_START_X = 188;
    public static final int EQUIPMENT_TOP_Y = 76;
    public static final int EQUIPMENT_COLUMNS = 5;
    public static final int EQUIPMENT_SLOT_SPACING = 23;
    public static final int BACKPACK_COLUMNS = 6;
    public static final int BACKPACK_ROWS = 3;
    public static final int BACKPACK_X = 390;
    public static final int BACKPACK_Y = 236;
    public static final int BACKPACK_SLOT_SPACING = 18;

    private final Inventory playerInventory;
    private final PlayerExtraInventory extraInventory;
    private final PanelSnapshot initialSnapshot;
    private final LivingEntity target;
    private final PetEquipmentContainer petEquipmentContainer;
    private final PetBackpackContainer petBackpackContainer;
    private final List<PetEquipmentSlotDefinition> petDefinitions;
    private final List<Integer> equipmentMenuSlotIndices = new ArrayList<>();
    private final List<Integer> backpackMenuSlotIndices = new ArrayList<>();
    private int mainInventoryStart;
    private int mainInventoryEnd;
    private int hotbarStart;
    private int hotbarEnd;

    public AttributePanelMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, PanelSnapshot.decode(extraData), null);
    }

    private AttributePanelMenu(int containerId, Inventory playerInventory, PanelSnapshot snapshot, LivingEntity clientTarget) {
        super(ModMenus.ATTRIBUTE_PANEL.get(), containerId);
        this.playerInventory = playerInventory;
        this.extraInventory = PlayerExtraInventoryManager.get(playerInventory.player);
        this.initialSnapshot = snapshot;
        this.target = clientTarget;
        this.petDefinitions = snapshot.equipment().slots().stream().map(value -> value.definition()).toList();
        this.petEquipmentContainer = snapshot.player()
                ? null
                : new PetEquipmentContainer(clientTarget, petDefinitions, snapshot.equipment().editable());
        this.petBackpackContainer = snapshot.backpackAvailable()
                ? new PetBackpackContainer(clientTarget, snapshot.backpackEditable())
                : null;
        buildSlots();
    }

    public AttributePanelMenu(int containerId, Inventory playerInventory, LivingEntity target, PanelSnapshot snapshot) {
        super(ModMenus.ATTRIBUTE_PANEL.get(), containerId);
        this.playerInventory = playerInventory;
        this.extraInventory = PlayerExtraInventoryManager.get(playerInventory.player);
        this.initialSnapshot = snapshot;
        this.target = target;
        this.petDefinitions = snapshot.equipment().slots().stream().map(value -> value.definition()).toList();
        this.petEquipmentContainer = snapshot.player()
                ? null
                : new PetEquipmentContainer(target, petDefinitions, snapshot.equipment().editable());
        this.petBackpackContainer = snapshot.backpackAvailable()
                ? new PetBackpackContainer(target, snapshot.backpackEditable())
                : null;
        buildSlots();
    }

    private void buildSlots() {
        if (initialSnapshot.player()) {
            addPlayerEquipmentSlots();
        } else {
            addPetEquipmentSlots();
            addPetBackpackSlots();
        }
        addPlayerInventorySlots();
    }

    private void addPlayerEquipmentSlots() {
        addPlayerArmorSlot(EquipmentSlot.HEAD, 39, equipmentX(0), EQUIPMENT_TOP_Y);
        addPlayerArmorSlot(EquipmentSlot.CHEST, 38, equipmentX(1), EQUIPMENT_TOP_Y);
        addPlayerArmorSlot(EquipmentSlot.LEGS, 37, equipmentX(2), EQUIPMENT_TOP_Y);
        addPlayerArmorSlot(EquipmentSlot.FEET, 36, equipmentX(3), EQUIPMENT_TOP_Y);
        Slot offhand = new Slot(playerInventory, 40, equipmentX(4), EQUIPMENT_TOP_Y);
        equipmentMenuSlotIndices.add(addSlot(offhand).index);
    }

    private void addPlayerArmorSlot(EquipmentSlot equipmentSlot, int inventoryIndex, int x, int y) {
        Slot slot = new Slot(playerInventory, inventoryIndex, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.canEquip(equipmentSlot, playerInventory.player);
            }

            @Override
            public boolean mayPickup(Player player) {
                ItemStack stack = getItem();
                return stack.isEmpty() || player.isCreative() || !EnchantmentHelper.hasBindingCurse(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        };
        equipmentMenuSlotIndices.add(addSlot(slot).index);
    }

    private void addPetEquipmentSlots() {
        if (petEquipmentContainer == null) {
            return;
        }
        for (int i = 0; i < petDefinitions.size(); i++) {
            int column = i % EQUIPMENT_COLUMNS;
            int row = i / EQUIPMENT_COLUMNS;
            int x = equipmentX(column);
            int y = EQUIPMENT_TOP_Y + row * EQUIPMENT_SLOT_SPACING;
            final int petSlot = i;
            Slot slot = new Slot(petEquipmentContainer, i, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return petEquipmentContainer.canPlaceItem(petSlot, stack);
                }

                @Override
                public boolean mayPickup(Player player) {
                    return petEquipmentContainer.editable();
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            };
            equipmentMenuSlotIndices.add(addSlot(slot).index);
        }
    }

    private void addPetBackpackSlots() {
        if (petBackpackContainer == null) {
            return;
        }
        for (int row = 0; row < BACKPACK_ROWS; row++) {
            for (int column = 0; column < BACKPACK_COLUMNS; column++) {
                int slotIndex = row * BACKPACK_COLUMNS + column;
                Slot slot = new Slot(
                        petBackpackContainer,
                        slotIndex,
                        BACKPACK_X + column * BACKPACK_SLOT_SPACING,
                        BACKPACK_Y + row * BACKPACK_SLOT_SPACING
                ) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return petBackpackContainer.editable();
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return petBackpackContainer.editable();
                    }
                };
                backpackMenuSlotIndices.add(addSlot(slot).index);
            }
        }
    }

    private void addPlayerInventorySlots() {
        mainInventoryStart = slots.size();
        for (int row = 0; row < INVENTORY_ROWS; row++) {
            for (int column = 0; column < INVENTORY_COLUMNS; column++) {
                addMainInventorySlot(row, column);
            }
        }
        mainInventoryEnd = slots.size();
        hotbarStart = slots.size();
        for (int hotbarIndex = 0; hotbarIndex < 9; hotbarIndex++) {
            addSlot(new Slot(playerInventory, hotbarIndex,
                    HOTBAR_X,
                    HOTBAR_Y + hotbarIndex * INVENTORY_ROW_SPACING));
        }
        hotbarEnd = slots.size();
    }

    private void addMainInventorySlot(int row, int column) {
        int x = INVENTORY_X + column * INVENTORY_COLUMN_SPACING;
        int y = INVENTORY_Y + row * INVENTORY_ROW_SPACING;
        if (column < VANILLA_INVENTORY_COLUMNS) {
            int originalRow = 2 - column;
            int originalColumn = row;
            int inventoryIndex = originalColumn + originalRow * 9 + 9;
            addSlot(new Slot(playerInventory, inventoryIndex, x, y));
            return;
        }
        int extraColumn = column - VANILLA_INVENTORY_COLUMNS;
        int extraIndex = extraColumn * INVENTORY_ROWS + row;
        addSlot(new Slot(extraInventory, extraIndex, x, y));
    }

    public PanelSnapshot initialSnapshot() {
        return initialSnapshot;
    }

    public List<PetEquipmentSlotDefinition> petDefinitions() {
        return petDefinitions;
    }

    public List<Integer> equipmentMenuSlotIndices() {
        return List.copyOf(equipmentMenuSlotIndices);
    }

    public List<Integer> backpackMenuSlotIndices() {
        return List.copyOf(backpackMenuSlotIndices);
    }

    public int equipmentRowCount() {
        if (initialSnapshot.player()) {
            return 1;
        }
        return (petDefinitions.size() + EQUIPMENT_COLUMNS - 1) / EQUIPMENT_COLUMNS;
    }

    private static int equipmentX(int column) {
        return EQUIPMENT_START_X + column * EQUIPMENT_SLOT_SPACING;
    }

    public boolean petView() {
        return !initialSnapshot.player();
    }

    @Override
    public boolean stillValid(Player player) {
        if (initialSnapshot.player()) {
            return true;
        }
        boolean equipmentValid = petEquipmentContainer == null || petEquipmentContainer.stillValid(player);
        boolean backpackValid = petBackpackContainer == null || petBackpackContainer.stillValid(player);
        return equipmentValid && backpackValid;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot source = slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack sourceStack = source.getItem();
        ItemStack copy = sourceStack.copy();
        boolean moved;
        if (index < mainInventoryStart) {
            moved = moveItemStackTo(sourceStack, mainInventoryStart, mainInventoryEnd, false);
            if (!moved) {
                moved = moveItemStackTo(sourceStack, hotbarStart, hotbarEnd, false);
            }
        } else {
            moved = moveIntoEquipment(sourceStack);
            if (!moved) {
                moved = moveIntoBackpack(sourceStack);
            }
            if (!moved) {
                if (index >= mainInventoryStart && index < mainInventoryEnd) {
                    moved = moveItemStackTo(sourceStack, hotbarStart, hotbarEnd, false);
                } else {
                    moved = moveItemStackTo(sourceStack, mainInventoryStart, mainInventoryEnd, false);
                }
            }
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (sourceStack.isEmpty()) {
            source.set(ItemStack.EMPTY);
        } else {
            source.setChanged();
        }
        source.onTake(player, sourceStack);
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        extraInventory.setChanged();
        if (petBackpackContainer != null) {
            petBackpackContainer.setChanged();
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (button == 1
                && clickType == ClickType.PICKUP
                && getCarried().isEmpty()
                && target != null
                && backpackMenuSlotIndices.contains(slotId)) {
            int backpackSlot = backpackMenuSlotIndices.indexOf(slotId);
            if (backpackSlot >= 0 && PetBackpackService.forceUse(player, target, backpackSlot)) {
                broadcastChanges();
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    private boolean moveIntoBackpack(ItemStack stack) {
        if (petBackpackContainer == null || !petBackpackContainer.editable() || backpackMenuSlotIndices.isEmpty()) {
            return false;
        }
        int start = backpackMenuSlotIndices.get(0);
        int end = backpackMenuSlotIndices.get(backpackMenuSlotIndices.size() - 1) + 1;
        return moveItemStackTo(stack, start, end, false);
    }

    private boolean moveIntoEquipment(ItemStack stack) {
        for (int menuIndex : equipmentMenuSlotIndices) {
            Slot targetSlot = slots.get(menuIndex);
            if (!targetSlot.hasItem() && targetSlot.mayPlace(stack)) {
                if (moveItemStackTo(stack, menuIndex, menuIndex + 1, false)) {
                    return true;
                }
            }
        }
        return false;
    }
}
