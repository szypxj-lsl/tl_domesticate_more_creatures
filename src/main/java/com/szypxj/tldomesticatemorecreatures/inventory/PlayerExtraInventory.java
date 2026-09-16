package com.szypxj.tldomesticatemorecreatures.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PlayerExtraInventory implements Container {
    public static final int SIZE = 27;
    private static final String ROOT_KEY = "tdmcExtraInventory";

    private final Player player;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public PlayerExtraInventory(Player player) {
        this.player = player;
        load();
    }

    public static void copy(Player from, Player to) {
        CompoundTag fromPersistent = from.getPersistentData();
        CompoundTag toPersistent = to.getPersistentData();
        if (fromPersistent.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            toPersistent.put(ROOT_KEY, fromPersistent.getCompound(ROOT_KEY).copy());
        } else {
            toPersistent.remove(ROOT_KEY);
        }
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < SIZE ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SIZE) {
            return;
        }
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        if (!player.level().isClientSide) {
            save();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.player == player;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    private void load() {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return;
        }
        ContainerHelper.loadAllItems(persistent.getCompound(ROOT_KEY), items);
    }

    private void save() {
        CompoundTag persistent = player.getPersistentData();
        if (isEmpty()) {
            persistent.remove(ROOT_KEY);
            return;
        }
        CompoundTag root = new CompoundTag();
        ContainerHelper.saveAllItems(root, items);
        persistent.put(ROOT_KEY, root);
    }
}
