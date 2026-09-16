package com.szypxj.tldomesticatemorecreatures.network;

import net.minecraft.network.FriendlyByteBuf;

public record TargetHudSnapshot(
        int entityId,
        boolean tamed,
        float health,
        float maxHealth,
        boolean torporAvailable,
        double torpor,
        double maxTorpor,
        int requiredTamingLevel
) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeBoolean(tamed);
        buffer.writeFloat(health);
        buffer.writeFloat(maxHealth);
        buffer.writeBoolean(torporAvailable);
        buffer.writeDouble(torpor);
        buffer.writeDouble(maxTorpor);
        buffer.writeVarInt(requiredTamingLevel);
    }

    public static TargetHudSnapshot decode(FriendlyByteBuf buffer) {
        return new TargetHudSnapshot(
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readVarInt()
        );
    }
}
