package com.szypxj.tldomesticatemorecreatures.equipment;

import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PetEquipmentService {
    private PetEquipmentService() {
    }

    public static List<PetEquipmentSlotDefinition> slots(LivingEntity pet) {
        if (pet == null) {
            return List.of();
        }
        LinkedHashMap<String, PetEquipmentSlotDefinition> result = new LinkedHashMap<>();
        for (PetEquipmentSlotDefinition slot : PetEquipmentConfigManager.profile(pet.getType()).slots()) {
            result.putIfAbsent(slot.id(), slot);
        }
        for (PetEquipmentCompatibilityApi.Provider provider : supportedProviders(pet)) {
            try {
                for (PetEquipmentSlotDefinition slot : provider.additionalSlots(pet)) {
                    if (slot != null) {
                        result.putIfAbsent(slot.id(), slot);
                    }
                }
            } catch (RuntimeException ignored) {
            }
        }
        return List.copyOf(result.values());
    }

    public static ItemStack getItem(LivingEntity pet, PetEquipmentSlotDefinition slot) {
        if (pet == null || slot == null) {
            return ItemStack.EMPTY;
        }
        PetEquipmentCompatibilityApi.Provider provider = ownerProvider(pet, slot.id());
        if (provider != null) {
            try {
                ItemStack stack = provider.getStack(pet, slot.id());
                return stack == null ? ItemStack.EMPTY : stack;
            } catch (RuntimeException ignored) {
                return ItemStack.EMPTY;
            }
        }
        if (slot.vanillaSlot() != null) {
            return pet.getItemBySlot(slot.vanillaSlot());
        }
        return PetEquipmentData.of(pet).get(slot.id());
    }

    public static boolean setItem(LivingEntity pet, PetEquipmentSlotDefinition slot, ItemStack stack) {
        if (pet == null || slot == null) {
            return false;
        }
        ItemStack value = stack == null ? ItemStack.EMPTY : stack.copy();
        if (!value.isEmpty()) {
            value.setCount(1);
            if (!canEquip(pet, slot, value)) {
                return false;
            }
        }
        ItemStack previous = getItem(pet, slot).copy();
        PetEquipmentCompatibilityApi.Provider owner = ownerProvider(pet, slot.id());
        if (owner != null) {
            try {
                if (!owner.setStack(pet, slot.id(), value)) {
                    return false;
                }
            } catch (RuntimeException ignored) {
                return false;
            }
        } else if (slot.vanillaSlot() != null) {
            pet.setItemSlot(slot.vanillaSlot(), value);
            if (pet instanceof Mob mob && !value.isEmpty()) {
                mob.setGuaranteedDrop(slot.vanillaSlot());
            }
        } else {
            PetEquipmentData.of(pet).set(slot.id(), value);
        }
        refreshAttributeBonuses(pet);
        notifyProviders(pet, slot.id(), previous, value);
        return true;
    }


    public static void dropCustomEquipment(LivingEntity pet) {
        if (pet == null || pet.level().isClientSide) {
            return;
        }
        for (PetEquipmentSlotDefinition slot : slots(pet)) {
            if (slot.vanillaSlot() != null || ownerProvider(pet, slot.id()) != null) {
                continue;
            }
            ItemStack stack = PetEquipmentData.of(pet).get(slot.id());
            if (stack.isEmpty()) {
                continue;
            }
            pet.spawnAtLocation(stack.copy());
            PetEquipmentData.of(pet).clear(slot.id());
        }
    }

    public static boolean canEquip(LivingEntity pet, PetEquipmentSlotDefinition slot, ItemStack stack) {
        if (pet == null || slot == null || stack == null || stack.isEmpty()) {
            return true;
        }
        PetEquipmentCompatibilityApi.Provider owner = ownerProvider(pet, slot.id());
        if (owner != null) {
            try {
                return owner.canEquip(pet, slot.id(), stack);
            } catch (RuntimeException ignored) {
                return false;
            }
        }
        ResourceLocation entityId = net.minecraft.world.entity.EntityType.getKey(pet.getType());
        PetEquipmentItemRule rule = PetEquipmentConfigManager.rule(stack);
        if (rule != null && rule.allows(entityId, slot.id())) {
            return true;
        }
        if (slot.allowAnyItem()) {
            return true;
        }
        EquipmentSlot vanillaSlot = slot.vanillaSlot();
        if (vanillaSlot == null) {
            return false;
        }
        return stack.canEquip(vanillaSlot, pet);
    }

    public static void refreshAttributeBonuses(LivingEntity pet) {
        if (pet == null || pet.level().isClientSide || !PetOwnershipService.isTamed(pet)) {
            return;
        }
        List<PetEquipmentSlotDefinition> slots = slots(pet);
        for (PetEquipmentSlotDefinition slot : slots) {
            for (ResourceLocation attributeId : PetEquipmentConfigManager.referencedAttributes()) {
                Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
                if (attribute == null) {
                    continue;
                }
                AttributeInstance instance = pet.getAttribute(attribute);
                if (instance != null) {
                    instance.removeModifier(modifierId(slot.id(), attributeId));
                }
            }
        }

        ResourceLocation entityId = net.minecraft.world.entity.EntityType.getKey(pet.getType());
        for (PetEquipmentSlotDefinition slot : slots) {
            ItemStack stack = getItem(pet, slot);
            PetEquipmentItemRule rule = PetEquipmentConfigManager.rule(stack);
            if (rule == null || !rule.allows(entityId, slot.id())) {
                continue;
            }
            for (PetEquipmentItemRule.AttributeBonus bonus : rule.attributeBonuses()) {
                Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(bonus.attributeId());
                if (attribute == null) {
                    continue;
                }
                AttributeInstance instance = pet.getAttribute(attribute);
                if (instance == null) {
                    continue;
                }
                UUID id = modifierId(slot.id(), bonus.attributeId());
                instance.removeModifier(id);
                instance.addTransientModifier(new AttributeModifier(
                        id,
                        "tdmc_pet_equipment_" + slot.id(),
                        bonus.amount(),
                        bonus.operation().vanilla()
                ));
            }
        }
        if (pet.getHealth() > pet.getMaxHealth()) {
            pet.setHealth(pet.getMaxHealth());
        }
    }

    private static UUID modifierId(String slotId, ResourceLocation attributeId) {
        return UUID.nameUUIDFromBytes(("tdmc:pet_equipment:" + slotId + ":" + attributeId)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static List<PetEquipmentCompatibilityApi.Provider> supportedProviders(LivingEntity pet) {
        List<PetEquipmentCompatibilityApi.Provider> result = new ArrayList<>();
        for (PetEquipmentCompatibilityApi.Provider provider : PetEquipmentCompatibilityApi.providers()) {
            try {
                if (provider.supports(pet)) {
                    result.add(provider);
                }
            } catch (RuntimeException ignored) {
            }
        }
        return result;
    }

    private static PetEquipmentCompatibilityApi.Provider ownerProvider(LivingEntity pet, String slotId) {
        for (PetEquipmentCompatibilityApi.Provider provider : supportedProviders(pet)) {
            try {
                if (provider.ownsSlot(pet, slotId)) {
                    return provider;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    private static void notifyProviders(LivingEntity pet, String slotId, ItemStack previous, ItemStack current) {
        for (PetEquipmentCompatibilityApi.Provider provider : supportedProviders(pet)) {
            try {
                provider.onEquipmentChanged(pet, slotId, previous.copy(), current.copy());
            } catch (RuntimeException ignored) {
            }
        }
    }
}
