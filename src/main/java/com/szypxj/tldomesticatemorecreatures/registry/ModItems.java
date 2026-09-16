package com.szypxj.tldomesticatemorecreatures.registry;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.item.AttributeResetCrystalItem;
import com.szypxj.tldomesticatemorecreatures.item.ClubItem;
import com.szypxj.tldomesticatemorecreatures.item.PetExperiencePotionItem;
import com.szypxj.tldomesticatemorecreatures.item.SuperSpyglassItem;
import com.szypxj.tldomesticatemorecreatures.item.EmptyPetExperienceBottleItem;
import com.szypxj.tldomesticatemorecreatures.item.PetExperienceBottleItem;
import com.szypxj.tldomesticatemorecreatures.item.NarcoticArrowItem;
import com.szypxj.tldomesticatemorecreatures.item.NarcoticItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TlDomesticateMoreCreatures.MOD_ID);

    public static final RegistryObject<Item> ATTRIBUTE_RESET_CRYSTAL =
            ITEMS.register("attribute_reset_crystal",
                    () -> new AttributeResetCrystalItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> PET_EXPERIENCE_POTION =
            ITEMS.register("pet_experience_potion",
                    () -> new PetExperiencePotionItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> EMPTY_PET_EXPERIENCE_BOTTLE =
            ITEMS.register("empty_pet_experience_bottle",
                    () -> new EmptyPetExperienceBottleItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> PET_EXPERIENCE_BOTTLE =
            ITEMS.register("pet_experience_bottle",
                    () -> new PetExperienceBottleItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> NARCOTIC =
            ITEMS.register("narcotic",
                    () -> new NarcoticItem(new Item.Properties().stacksTo(1), 1,
                            "tip.tl_domesticate_more_creatures.narcotic"));

    public static final RegistryObject<Item> STRONG_NARCOTIC =
            ITEMS.register("strong_narcotic",
                    () -> new NarcoticItem(new Item.Properties().stacksTo(1), 2,
                            "tip.tl_domesticate_more_creatures.strong_narcotic"));

    public static final RegistryObject<Item> CONCENTRATED_NARCOTIC =
            ITEMS.register("concentrated_narcotic",
                    () -> new NarcoticItem(new Item.Properties().stacksTo(1), 3,
                            "tip.tl_domesticate_more_creatures.concentrated_narcotic"));

    public static final RegistryObject<Item> NARCOTIC_ARROW =
            ITEMS.register("narcotic_arrow",
                    () -> new NarcoticArrowItem(new Item.Properties(), 1));

    public static final RegistryObject<Item> STRONG_NARCOTIC_ARROW =
            ITEMS.register("strong_narcotic_arrow",
                    () -> new NarcoticArrowItem(new Item.Properties(), 2));

    public static final RegistryObject<Item> CONCENTRATED_NARCOTIC_ARROW =
            ITEMS.register("concentrated_narcotic_arrow",
                    () -> new NarcoticArrowItem(new Item.Properties(), 3));

    public static final RegistryObject<Item> CLUB =
            ITEMS.register("club",
                    () -> new ClubItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SUPER_SPYGLASS =
            ITEMS.register("super_spyglass",
                    () -> new SuperSpyglassItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}