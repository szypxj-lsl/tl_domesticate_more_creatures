package com.szypxj.tldomesticatemorecreatures.menu;

import com.szypxj.tldomesticatemorecreatures.equipment.PetEquipmentService;
import com.szypxj.tldomesticatemorecreatures.equipment.PetEquipmentSlotDefinition;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class PetEquipmentContainer implements Container {
    private final LivingEntity pet;
    private final List<PetEquipmentSlotDefinition> definitions;
    private final List<ItemStack> clientShadow;
    private final boolean editable;
    private final boolean serverBacked;

    public PetEquipmentContainer(LivingEntity pet, List<PetEquipmentSlotDefinition> definitions, boolean editable) {
        this.pet = pet;
        this.definitions = definitions == null ? List.of() : List.copyOf(definitions);
        this.editable = editable;
        this.serverBacked = pet != null && !pet.level().isClientSide;
        this.clientShadow = new ArrayList<>(this.definitions.size());
        for (int i = 0; i < this.definitions.size(); i++) {
            clientShadow.add(ItemStack.EMPTY);
        }
    }

    public PetEquipmentSlotDefinition definition(int slot) {
        return definitions.get(slot);
    }

    public boolean editable() {
        return editable;
    }

    @Override
    public int getContainerSize() {
        return definitions.size();
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
        if (slot < 0 || slot >= definitions.size()) {
            return ItemStack.EMPTY;
        }
        if (serverBacked) {
            return PetEquipmentService.getItem(pet, definitions.get(slot));
        }
        return clientShadow.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack existing = getItem(slot);
        if (existing.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = existing.copy();
        removed.setCount(Math.min(amount, existing.getCount()));
        if (removed.getCount() >= existing.getCount()) {
            setItem(slot, ItemStack.EMPTY);
        } else {
            ItemStack remainder = existing.copy();
            remainder.shrink(removed.getCount());
            setItem(slot, remainder);
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack existing = getItem(slot).copy();
        setItem(slot, ItemStack.EMPTY);
        return existing;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= definitions.size()) {
            return;
        }
        ItemStack value = stack == null ? ItemStack.EMPTY : stack.copy();
        if (!value.isEmpty()) {
            value.setCount(1);
        }
        if (serverBacked) {
            if (editable) {
                PetEquipmentService.setItem(pet, definitions.get(slot), value);
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
        if (pet == null || pet.level().isClientSide) {
            return true;
        }
        boolean reachable = player.getVehicle() == pet || player.distanceToSqr(pet) <= 64.0D;
        return pet.isAlive() && !pet.isRemoved() && pet.level() == player.level() && reachable;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < getContainerSize(); i++) {
            setItem(i, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (!editable || slot < 0 || slot >= definitions.size()) {
            return false;
        }
        return !serverBacked || PetEquipmentService.canEquip(pet, definitions.get(slot), stack);
    }
}
