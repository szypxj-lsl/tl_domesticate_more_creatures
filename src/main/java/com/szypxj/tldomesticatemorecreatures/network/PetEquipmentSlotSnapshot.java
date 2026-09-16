package com.szypxj.tldomesticatemorecreatures.network;

import com.szypxj.tldomesticatemorecreatures.equipment.PetEquipmentSlotDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;

public record PetEquipmentSlotSnapshot(
        String id,
        String nameKey,
        String vanillaSlot,
        boolean allowAnyItem
) {
    public static PetEquipmentSlotSnapshot of(PetEquipmentSlotDefinition slot) {
        return new PetEquipmentSlotSnapshot(
                slot.id(),
                slot.nameKey(),
                slot.vanillaSlot() == null ? "" : slot.vanillaSlot().name(),
                slot.allowAnyItem()
        );
    }

    public PetEquipmentSlotDefinition definition() {
        EquipmentSlot slot = null;
        if (vanillaSlot != null && !vanillaSlot.isBlank()) {
            try {
                slot = EquipmentSlot.valueOf(vanillaSlot);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return new PetEquipmentSlotDefinition(id, nameKey, slot, allowAnyItem);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(id);
        buffer.writeUtf(nameKey);
        buffer.writeUtf(vanillaSlot == null ? "" : vanillaSlot);
        buffer.writeBoolean(allowAnyItem);
    }

    public static PetEquipmentSlotSnapshot decode(FriendlyByteBuf buffer) {
        return new PetEquipmentSlotSnapshot(
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readBoolean()
        );
    }
}
