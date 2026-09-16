package com.szypxj.tldomesticatemorecreatures.item;

import com.szypxj.tldomesticatemorecreatures.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SpyglassItemHelper {
    private SpyglassItemHelper() {
    }

    public static boolean isAnySpyglass(ItemStack stack) {
        return stack != null && (stack.is(Items.SPYGLASS) || stack.is(ModItems.SUPER_SPYGLASS.get()));
    }

    public static boolean isSuperSpyglass(ItemStack stack) {
        return stack != null && stack.is(ModItems.SUPER_SPYGLASS.get());
    }

    public static boolean isUsingSpyglass(Player player) {
        return player != null && player.isUsingItem() && isAnySpyglass(player.getUseItem());
    }

    public static boolean isHoldingSpyglass(Player player) {
        return player != null && (isAnySpyglass(player.getMainHandItem()) || isAnySpyglass(player.getOffhandItem()));
    }

    public static boolean isUsingSuperSpyglass(Player player) {
        return player != null && player.isUsingItem() && isSuperSpyglass(player.getUseItem());
    }

    public static boolean isHoldingSuperSpyglass(Player player) {
        return player != null && (isSuperSpyglass(player.getMainHandItem()) || isSuperSpyglass(player.getOffhandItem()));
    }
}
