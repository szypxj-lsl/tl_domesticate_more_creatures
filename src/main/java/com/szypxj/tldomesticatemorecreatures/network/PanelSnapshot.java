package com.szypxj.tldomesticatemorecreatures.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public record PanelSnapshot(
        int entityId,
        Component name,
        boolean player,
        boolean tamed,
        boolean owner,
        boolean elite,
        int initialLevel,
        int level,
        int maxLevel,
        long experience,
        long experienceRequired,
        int unspentPoints,
        RideStatusSnapshot riding,
        boolean allowOtherRiders,
        PetEquipmentSnapshot equipment,
        boolean backpackAvailable,
        boolean backpackEditable,
        ImprintSnapshot imprint,
        List<TalentSnapshot> talents,
        FurySnapshot fury,
        List<StatSnapshot> stats
) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeComponent(name);
        buffer.writeBoolean(player);
        buffer.writeBoolean(tamed);
        buffer.writeBoolean(owner);
        buffer.writeBoolean(elite);
        buffer.writeVarInt(initialLevel);
        buffer.writeVarInt(level);
        buffer.writeVarInt(maxLevel);
        buffer.writeLong(experience);
        buffer.writeLong(experienceRequired);
        buffer.writeVarInt(unspentPoints);
        riding.encode(buffer);
        buffer.writeBoolean(allowOtherRiders);
        equipment.encode(buffer);
        buffer.writeBoolean(backpackAvailable);
        buffer.writeBoolean(backpackEditable);
        imprint.encode(buffer);
        buffer.writeVarInt(talents.size());
        for (TalentSnapshot talent : talents) {
            talent.encode(buffer);
        }
        fury.encode(buffer);
        buffer.writeVarInt(stats.size());
        for (StatSnapshot stat : stats) {
            stat.encode(buffer);
        }
    }

    public static PanelSnapshot decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        Component name = buffer.readComponent();
        boolean player = buffer.readBoolean();
        boolean tamed = buffer.readBoolean();
        boolean owner = buffer.readBoolean();
        boolean elite = buffer.readBoolean();
        int initialLevel = buffer.readVarInt();
        int level = buffer.readVarInt();
        int maxLevel = buffer.readVarInt();
        long experience = buffer.readLong();
        long experienceRequired = buffer.readLong();
        int unspentPoints = buffer.readVarInt();
        RideStatusSnapshot riding = RideStatusSnapshot.decode(buffer);
        boolean allowOtherRiders = buffer.readBoolean();
        PetEquipmentSnapshot equipment = PetEquipmentSnapshot.decode(buffer);
        boolean backpackAvailable = buffer.readBoolean();
        boolean backpackEditable = buffer.readBoolean();
        ImprintSnapshot imprint = ImprintSnapshot.decode(buffer);
        int talentSize = buffer.readVarInt();
        List<TalentSnapshot> talents = new ArrayList<>(talentSize);
        for (int i = 0; i < talentSize; i++) {
            talents.add(TalentSnapshot.decode(buffer));
        }
        FurySnapshot fury = FurySnapshot.decode(buffer);
        int statSize = buffer.readVarInt();
        List<StatSnapshot> stats = new ArrayList<>(statSize);
        for (int i = 0; i < statSize; i++) {
            stats.add(StatSnapshot.decode(buffer));
        }
        return new PanelSnapshot(
                entityId,
                name,
                player,
                tamed,
                owner,
                elite,
                initialLevel,
                level,
                maxLevel,
                experience,
                experienceRequired,
                unspentPoints,
                riding,
                allowOtherRiders,
                equipment,
                backpackAvailable,
                backpackEditable,
                imprint,
                List.copyOf(talents),
                fury,
                List.copyOf(stats)
        );
    }
}
