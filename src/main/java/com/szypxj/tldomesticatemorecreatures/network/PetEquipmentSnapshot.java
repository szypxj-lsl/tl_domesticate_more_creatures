package com.szypxj.tldomesticatemorecreatures.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record PetEquipmentSnapshot(boolean available, boolean editable, List<PetEquipmentSlotSnapshot> slots) {
    public static final PetEquipmentSnapshot NONE = new PetEquipmentSnapshot(false, false, List.of());

    public PetEquipmentSnapshot {
        slots = slots == null ? List.of() : List.copyOf(slots);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(available);
        buffer.writeBoolean(editable);
        buffer.writeVarInt(slots.size());
        for (PetEquipmentSlotSnapshot slot : slots) {
            slot.encode(buffer);
        }
    }

    public static PetEquipmentSnapshot decode(FriendlyByteBuf buffer) {
        boolean available = buffer.readBoolean();
        boolean editable = buffer.readBoolean();
        int count = buffer.readVarInt();
        List<PetEquipmentSlotSnapshot> slots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            slots.add(PetEquipmentSlotSnapshot.decode(buffer));
        }
        return new PetEquipmentSnapshot(available, editable, slots);
    }
}
