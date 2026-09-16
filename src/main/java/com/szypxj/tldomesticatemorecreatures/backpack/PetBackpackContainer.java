package com.szypxj.tldomesticatemorecreatures.backpack;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PetBackpackContainer implements Container {
    private final LivingEntity entity;
    private final boolean editable;
    private final boolean serverBacked;
    private final NonNullList<ItemStack> clientShadow = NonNullList.withSize(PetBackpackService.BACKPACK_SIZE, ItemStack.EMPTY);

    public PetBackpackContainer(LivingEntity entity, boolean editable) {
        this.entity = entity;
        this.editable = editable;
        this.serverBacked = entity != null && !entity.level().isClientSide;
    }

    public boolean editable() {
        return editable;
    }

    @Override
    public int getContainerSize() {
        return PetBackpackService.BACKPACK_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (!getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return serverBacked ? PetBackpackService.getItem(entity, slot) : clientShadow.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack existing = getItem(slot);
        if (existing.isEmpty() || amount <= 0 || !editable) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = existing.copy();
        removed.setCount(Math.min(amount, existing.getCount()));
        ItemStack remainder = existing.copy();
        remainder.shrink(removed.getCount());
        setItem(slot, remainder);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!editable) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = getItem(slot).copy();
        setItem(slot, ItemStack.EMPTY);
        return existing;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= getContainerSize()) {
            return;
        }
        ItemStack value = stack == null ? ItemStack.EMPTY : stack.copy();
        if (serverBacked) {
            if (editable) {
                PetBackpackService.setItem(entity, slot, value);
            }
        } else {
            clientShadow.set(slot, value);
        }
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        if (entity == null || entity.level().isClientSide) {
            return true;
        }
        boolean reachable = player.getVehicle() == entity || player.distanceToSqr(entity) <= 64.0D;
        return entity.isAlive()
                && !entity.isRemoved()
                && entity.level() == player.level()
                && reachable
                && (!editable || PetBackpackService.editable(player, entity));
    }

    @Override
    public void clearContent() {
        if (!editable) {
            return;
        }
        for (int i = 0; i < getContainerSize(); i++) {
            setItem(i, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return editable && slot >= 0 && slot < getContainerSize();
    }
}
