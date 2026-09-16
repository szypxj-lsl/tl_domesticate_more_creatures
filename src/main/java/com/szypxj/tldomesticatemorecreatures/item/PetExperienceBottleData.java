package com.szypxj.tldomesticatemorecreatures.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class PetExperienceBottleData {
    public static final String STORED_EXPERIENCE_KEY = "StoredExperience";

    private PetExperienceBottleData() {
    }

    public static long get(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return 0L;
        }
        CompoundTag tag = stack.getTag();
        return tag == null ? 0L : Math.max(0L, tag.getLong(STORED_EXPERIENCE_KEY));
    }

    public static void set(ItemStack stack, long experience) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        long safe = Math.max(0L, experience);
        if (safe <= 0L) {
            if (stack.hasTag() && stack.getTag() != null) {
                stack.getTag().remove(STORED_EXPERIENCE_KEY);
            }
            return;
        }
        stack.getOrCreateTag().putLong(STORED_EXPERIENCE_KEY, safe);
    }
}
