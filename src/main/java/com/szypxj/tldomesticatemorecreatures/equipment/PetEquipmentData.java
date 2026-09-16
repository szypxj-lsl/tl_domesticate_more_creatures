package com.szypxj.tldomesticatemorecreatures.equipment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class PetEquipmentData {
    private static final String ROOT_KEY = "tdmcPetEquipment";
    private final CompoundTag root;

    private PetEquipmentData(CompoundTag root) {
        this.root = root;
    }

    public static PetEquipmentData of(LivingEntity entity) {
        CompoundTag persistent = entity.getPersistentData();
        if (!persistent.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT_KEY, new CompoundTag());
        }
        return new PetEquipmentData(persistent.getCompound(ROOT_KEY));
    }

    public ItemStack get(String slotId) {
        String key = PetEquipmentSlotDefinition.normalizeId(slotId);
        if (!root.contains(key, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.of(root.getCompound(key));
    }

    public void set(String slotId, ItemStack stack) {
        String key = PetEquipmentSlotDefinition.normalizeId(slotId);
        if (stack == null || stack.isEmpty()) {
            root.remove(key);
            return;
        }
        CompoundTag tag = new CompoundTag();
        stack.copy().save(tag);
        root.put(key, tag);
    }

    public void clear(String slotId) {
        root.remove(PetEquipmentSlotDefinition.normalizeId(slotId));
    }
}
