package com.szypxj.tldomesticatemorecreatures.backpack;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class PetBackpackData {
    private static final String ROOT_KEY = "tdmcPetBackpack";
    private static final String ITEMS_KEY = "Items";

    private PetBackpackData() {
    }

    public static NonNullList<ItemStack> load(LivingEntity entity) {
        NonNullList<ItemStack> items = NonNullList.withSize(PetBackpackService.BACKPACK_SIZE, ItemStack.EMPTY);
        if (entity == null) {
            return items;
        }
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return items;
        }
        CompoundTag root = persistent.getCompound(ROOT_KEY);
        if (root.contains(ITEMS_KEY, Tag.TAG_LIST)) {
            ContainerHelper.loadAllItems(root, items);
        }
        return items;
    }

    public static void save(LivingEntity entity, NonNullList<ItemStack> items) {
        if (entity == null || items == null) {
            return;
        }
        CompoundTag root = new CompoundTag();
        ContainerHelper.saveAllItems(root, items);
        entity.getPersistentData().put(ROOT_KEY, root);
    }

    public static void clear(LivingEntity entity) {
        if (entity != null) {
            entity.getPersistentData().remove(ROOT_KEY);
        }
    }
}
