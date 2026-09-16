package com.szypxj.tldomesticatemorecreatures.brewing;

import java.nio.file.Files;
import java.nio.file.Path;

public final class NarcoticBrewingSourceInvariantTest {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        Path recipeSource = root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/brewing/NarcoticBrewingRecipes.java");
        Path modSource = root.resolve("src/main/java/com/szypxj/tldomesticatemorecreatures/TlDomesticateMoreCreatures.java");

        assertContains(recipeSource, "BrewingRecipeRegistry.addRecipe(new StandardPoisonNarcoticRecipe())");
        assertContains(recipeSource, "Items.POTION");
        assertContains(recipeSource, "Potions.POISON");
        assertContains(recipeSource, "Items.SPIDER_EYE");
        assertContains(recipeSource, "ModItems.NARCOTIC");
        assertContains(recipeSource, "Items.POISONOUS_POTATO");
        assertContains(recipeSource, "ModItems.STRONG_NARCOTIC");
        assertContains(recipeSource, "Items.FERMENTED_SPIDER_EYE");
        assertContains(recipeSource, "ModItems.CONCENTRATED_NARCOTIC");
        assertContains(recipeSource, "PotionUtils.getPotion(input) == Potions.POISON");
        assertContains(modSource, "NarcoticBrewingRecipes.register();");

        System.out.println("NARCOTIC_BREWING_SOURCE_PASS");
    }

    private static void assertContains(Path path, String needle) throws Exception {
        if (!Files.exists(path)) {
            throw new AssertionError("Missing required file: " + path);
        }
        String text = Files.readString(path);
        if (!text.contains(needle)) {
            throw new AssertionError("Missing source invariant: " + needle + " in " + path);
        }
    }
}
