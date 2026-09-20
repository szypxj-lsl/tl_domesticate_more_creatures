package com.szypxj.tldomesticatemorecreatures.api.creature.compat;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.api.creature.TamingFoodInfo;
import com.szypxj.tldomesticatemorecreatures.api.creature.TamingInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Shared fallback diet inference from explicitly configured taming foods. */
public final class CreatureDietResolver {
    public static final TagKey<Item> MEAT = foodTag("meat");
    public static final TagKey<Item> PLANT = foodTag("plant");
    public static final TagKey<Item> CARRION = foodTag("carrion");

    private CreatureDietResolver() {
    }

    public static CreatureDiet resolveFromConfiguredFoods(TamingInfo info) {
        if (info == null || !info.tameable() || info.foods().isEmpty()) {
            return CreatureDiet.UNKNOWN;
        }
        int meat = 0;
        int plant = 0;
        int carrion = 0;
        for (TamingFoodInfo food : info.foods()) {
            if (food == null || !food.configured() || food.amount() <= 0) {
                continue;
            }
            Item item = ForgeRegistries.ITEMS.getValue(food.itemId());
            if (item == null) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            if (stack.is(CARRION)) {
                carrion++;
            } else if (stack.is(MEAT)) {
                meat++;
            } else if (stack.is(PLANT)) {
                plant++;
            }
        }
        if (carrion > 0 && meat == 0 && plant == 0) {
            return CreatureDiet.SCAVENGER;
        }
        if (meat > 0 && plant > 0) {
            return CreatureDiet.OMNIVORE;
        }
        if (meat > 0) {
            return CreatureDiet.CARNIVORE;
        }
        if (plant > 0) {
            return CreatureDiet.HERBIVORE;
        }
        return CreatureDiet.UNKNOWN;
    }

    private static TagKey<Item> foodTag(String path) {
        return TagKey.create(
                Registries.ITEM,
                ResourceLocation.tryBuild(TlDomesticateMoreCreatures.MOD_ID, "foods/" + path)
        );
    }
}
