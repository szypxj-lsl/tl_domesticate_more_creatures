package com.szypxj.tldomesticatemorecreatures.equipment;

import net.minecraft.world.entity.EquipmentSlot;

import java.util.Locale;

public record PetEquipmentSlotDefinition(
        String id,
        String nameKey,
        EquipmentSlot vanillaSlot,
        boolean allowAnyItem
) {
    public PetEquipmentSlotDefinition {
        id = normalizeId(id);
        nameKey = nameKey == null || nameKey.isBlank()
                ? "gui.tl_domesticate_more_creatures.pet_equipment.slot." + id
                : nameKey;
        if (vanillaSlot != null && vanillaSlot.getType() != EquipmentSlot.Type.ARMOR) {
            vanillaSlot = null;
        }
    }

    public static PetEquipmentSlotDefinition head() {
        return new PetEquipmentSlotDefinition(
                "head",
                "gui.tl_domesticate_more_creatures.pet_equipment.slot.head",
                EquipmentSlot.HEAD,
                false
        );
    }

    public static PetEquipmentSlotDefinition chest() {
        return new PetEquipmentSlotDefinition(
                "chest",
                "gui.tl_domesticate_more_creatures.pet_equipment.slot.chest",
                EquipmentSlot.CHEST,
                false
        );
    }

    public static PetEquipmentSlotDefinition legs() {
        return new PetEquipmentSlotDefinition(
                "legs",
                "gui.tl_domesticate_more_creatures.pet_equipment.slot.legs",
                EquipmentSlot.LEGS,
                false
        );
    }

    public static PetEquipmentSlotDefinition feet() {
        return new PetEquipmentSlotDefinition(
                "feet",
                "gui.tl_domesticate_more_creatures.pet_equipment.slot.feet",
                EquipmentSlot.FEET,
                false
        );
    }

    public static String normalizeId(String value) {
        if (value == null) {
            return "equipment";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        return normalized.isBlank() ? "equipment" : normalized;
    }
}
