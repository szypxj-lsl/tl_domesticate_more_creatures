package com.szypxj.tldomesticatemorecreatures.brewing;

import com.szypxj.tldomesticatemorecreatures.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;

public final class NarcoticBrewingRecipes {
    private static boolean registered;

    private NarcoticBrewingRecipes() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        BrewingRecipeRegistry.addRecipe(new StandardPoisonNarcoticRecipe());
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(ModItems.NARCOTIC.get()),
                Ingredient.of(Items.POISONOUS_POTATO),
                new ItemStack(ModItems.STRONG_NARCOTIC.get())
        );
        BrewingRecipeRegistry.addRecipe(
                Ingredient.of(ModItems.STRONG_NARCOTIC.get()),
                Ingredient.of(Items.FERMENTED_SPIDER_EYE),
                new ItemStack(ModItems.CONCENTRATED_NARCOTIC.get())
        );
        registered = true;
    }

    private static final class StandardPoisonNarcoticRecipe implements IBrewingRecipe {
        @Override
        public boolean isInput(ItemStack input) {
            return input.is(Items.POTION) && PotionUtils.getPotion(input) == Potions.POISON;
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.is(Items.SPIDER_EYE);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (!isInput(input) || !isIngredient(ingredient)) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(ModItems.NARCOTIC.get());
        }
    }
}
