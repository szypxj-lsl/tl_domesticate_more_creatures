package com.szypxj.tldomesticatemorecreatures.network;

import com.szypxj.tldomesticatemorecreatures.petmanagement.PetRecord;
import com.szypxj.tldomesticatemorecreatures.petmanagement.PetRecordState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record PetManagementSummary(
        UUID petUuid,
        ResourceLocation entityTypeId,
        String displayName,
        int level,
        PetRecordState state,
        int shortcutSlot,
        boolean rideable
) {
    public static PetManagementSummary from(PetRecord record) {
        return new PetManagementSummary(
                record.petUuid(),
                record.entityTypeId(),
                record.displayName(),
                record.level(),
                record.state(),
                record.shortcutSlot(),
                record.rideable()
        );
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(petUuid);
        buffer.writeUtf(entityTypeId == null ? "minecraft:pig" : entityTypeId.toString());
        buffer.writeUtf(displayName == null ? "" : displayName, 256);
        buffer.writeVarInt(Math.max(1, level));
        buffer.writeEnum(state == null ? PetRecordState.UNLOCATED : state);
        buffer.writeByte(shortcutSlot);
        buffer.writeBoolean(rideable);
    }

    public static PetManagementSummary decode(FriendlyByteBuf buffer) {
        UUID uuid = buffer.readUUID();
        ResourceLocation typeId = ResourceLocation.tryParse(buffer.readUtf(32767));
        String displayName = buffer.readUtf(256);
        int level = buffer.readVarInt();
        PetRecordState state = buffer.readEnum(PetRecordState.class);
        int shortcut = buffer.readByte();
        boolean rideable = buffer.readBoolean();
        return new PetManagementSummary(uuid, typeId, displayName, level, state, shortcut, rideable);
    }
}
