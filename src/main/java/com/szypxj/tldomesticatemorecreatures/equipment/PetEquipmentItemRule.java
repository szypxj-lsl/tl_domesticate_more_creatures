package com.szypxj.tldomesticatemorecreatures.equipment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record PetEquipmentItemRule(
        ResourceLocation itemId,
        Set<ResourceLocation> allowedEntities,
        Set<String> allowedSlots,
        List<AttributeBonus> attributeBonuses
) {
    public PetEquipmentItemRule {
        allowedEntities = allowedEntities == null ? Set.of() : Set.copyOf(allowedEntities);
        allowedSlots = allowedSlots == null ? Set.of() : Set.copyOf(allowedSlots);
        attributeBonuses = attributeBonuses == null ? List.of() : List.copyOf(attributeBonuses);
    }

    public PetEquipmentItemRule validated() {
        LinkedHashSet<ResourceLocation> entities = new LinkedHashSet<>();
        for (ResourceLocation id : allowedEntities) {
            if (id != null) {
                entities.add(id);
            }
        }
        LinkedHashSet<String> slots = new LinkedHashSet<>();
        for (String id : allowedSlots) {
            if (id != null && !id.isBlank()) {
                slots.add(PetEquipmentSlotDefinition.normalizeId(id));
            }
        }
        List<AttributeBonus> bonuses = attributeBonuses.stream()
                .filter(java.util.Objects::nonNull)
                .map(AttributeBonus::validated)
                .filter(bonus -> bonus.attributeId() != null && Double.isFinite(bonus.amount()))
                .toList();
        return new PetEquipmentItemRule(itemId, entities, slots, bonuses);
    }

    public boolean allows(ResourceLocation entityId, String slotId) {
        if (entityId == null || slotId == null) {
            return false;
        }
        return (allowedEntities.isEmpty() || allowedEntities.contains(entityId))
                && allowedSlots.contains(PetEquipmentSlotDefinition.normalizeId(slotId));
    }

    public record AttributeBonus(ResourceLocation attributeId, double amount, Operation operation) {
        public AttributeBonus {
            operation = operation == null ? Operation.ADDITION : operation;
        }

        public AttributeBonus validated() {
            double clean = Double.isFinite(amount) ? Math.max(-1024.0D, Math.min(1024.0D, amount)) : 0.0D;
            return new AttributeBonus(attributeId, clean, operation == null ? Operation.ADDITION : operation);
        }
    }

    public enum Operation {
        ADDITION,
        MULTIPLY_BASE,
        MULTIPLY_TOTAL;

        public static Operation parse(String value) {
            if (value == null || value.isBlank()) {
                return ADDITION;
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return ADDITION;
            }
        }

        public AttributeModifier.Operation vanilla() {
            return switch (this) {
                case ADDITION -> AttributeModifier.Operation.ADDITION;
                case MULTIPLY_BASE -> AttributeModifier.Operation.MULTIPLY_BASE;
                case MULTIPLY_TOTAL -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            };
        }
    }
}
