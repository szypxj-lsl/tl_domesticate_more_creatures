package com.szypxj.tldomesticatemorecreatures.equipment;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PetEquipmentCompatibilityApi {
    private static final CopyOnWriteArrayList<Provider> PROVIDERS = new CopyOnWriteArrayList<>();

    private PetEquipmentCompatibilityApi() {
    }

    public static void registerProvider(Provider provider) {
        if (provider != null && !PROVIDERS.contains(provider)) {
            PROVIDERS.add(provider);
        }
    }

    public static List<Provider> providers() {
        return List.copyOf(PROVIDERS);
    }

    public interface Provider {
        boolean supports(LivingEntity pet);

        default List<PetEquipmentSlotDefinition> additionalSlots(LivingEntity pet) {
            return List.of();
        }

        default boolean ownsSlot(LivingEntity pet, String slotId) {
            return false;
        }

        default ItemStack getStack(LivingEntity pet, String slotId) {
            return ItemStack.EMPTY;
        }

        default boolean setStack(LivingEntity pet, String slotId, ItemStack stack) {
            return false;
        }

        default boolean canEquip(LivingEntity pet, String slotId, ItemStack stack) {
            return true;
        }

        default void onEquipmentChanged(LivingEntity pet, String slotId, ItemStack previous, ItemStack current) {
        }
    }
}
