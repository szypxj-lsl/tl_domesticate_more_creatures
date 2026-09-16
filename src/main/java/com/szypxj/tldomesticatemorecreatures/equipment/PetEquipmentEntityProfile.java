package com.szypxj.tldomesticatemorecreatures.equipment;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record PetEquipmentEntityProfile(ResourceLocation entityId, List<PetEquipmentSlotDefinition> slots) {
    public static final int MAX_SLOTS = 12;
    public PetEquipmentEntityProfile {
        slots = slots == null ? List.of() : List.copyOf(slots);
    }

    public static PetEquipmentEntityProfile defaults(ResourceLocation entityId) {
        return new PetEquipmentEntityProfile(entityId, List.of(
                PetEquipmentSlotDefinition.head(),
                PetEquipmentSlotDefinition.chest(),
                PetEquipmentSlotDefinition.legs(),
                PetEquipmentSlotDefinition.feet()
        ));
    }

    public PetEquipmentEntityProfile validated() {
        List<PetEquipmentSlotDefinition> clean = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        for (PetEquipmentSlotDefinition slot : slots) {
            if (slot == null || !ids.add(slot.id())) {
                continue;
            }
            clean.add(slot);
            if (clean.size() >= MAX_SLOTS) {
                break;
            }
        }
        if (clean.isEmpty()) {
            return defaults(entityId);
        }
        return new PetEquipmentEntityProfile(entityId, clean);
    }
}
