package com.szypxj.tldomesticatemorecreatures.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpyglassItem;

public class SuperSpyglassItem extends SpyglassItem {
    public SuperSpyglassItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}
