package com.szypxj.tldomesticatemorecreatures.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;

public final class CombinedPlayerItemHandler implements IItemHandler {
    private final IItemHandler primary;
    private final Player player;
    private PlayerExtraInventory cachedExtra;
    private InvWrapper cachedExtraHandler;

    public CombinedPlayerItemHandler(IItemHandler primary, Player player) {
        this.primary = primary;
        this.player = player;
    }

    @Override
    public int getSlots() {
        return primary.getSlots() + PlayerExtraInventory.SIZE;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        if (slot < primary.getSlots()) {
            return primary.getStackInSlot(slot);
        }
        return extraHandler().getStackInSlot(slot - primary.getSlots());
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (slot < primary.getSlots()) {
            return primary.insertItem(slot, stack, simulate);
        }
        return extraHandler().insertItem(slot - primary.getSlots(), stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < primary.getSlots()) {
            return primary.extractItem(slot, amount, simulate);
        }
        return extraHandler().extractItem(slot - primary.getSlots(), amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        if (slot < primary.getSlots()) {
            return primary.getSlotLimit(slot);
        }
        return extraHandler().getSlotLimit(slot - primary.getSlots());
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        if (slot < primary.getSlots()) {
            return primary.isItemValid(slot, stack);
        }
        return extraHandler().isItemValid(slot - primary.getSlots(), stack);
    }

    private InvWrapper extraHandler() {
        PlayerExtraInventory current = PlayerExtraInventoryManager.get(player);
        if (current != cachedExtra || cachedExtraHandler == null) {
            cachedExtra = current;
            cachedExtraHandler = new InvWrapper(current);
        }
        return cachedExtraHandler;
    }
}
