package com.szypxj.tldomesticatemorecreatures.network;

import net.minecraft.network.FriendlyByteBuf;

public record ImprintSnapshot(
        boolean available,
        boolean active,
        boolean finished,
        int percent,
        long remainingTicks,
        String needType,
        String foodNameKey,
        long nextNeedTicks,
        int levelCapBonus,
        boolean bonded
) {
    public static final ImprintSnapshot NONE = new ImprintSnapshot(false, false, false, 0, 0L, "", "", 0L, 0, false);

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(available);
        buffer.writeBoolean(active);
        buffer.writeBoolean(finished);
        buffer.writeVarInt(percent);
        buffer.writeVarLong(remainingTicks);
        buffer.writeUtf(needType == null ? "" : needType);
        buffer.writeUtf(foodNameKey == null ? "" : foodNameKey);
        buffer.writeVarLong(nextNeedTicks);
        buffer.writeVarInt(levelCapBonus);
        buffer.writeBoolean(bonded);
    }

    public static ImprintSnapshot decode(FriendlyByteBuf buffer) {
        return new ImprintSnapshot(
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarLong(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readVarLong(),
                buffer.readVarInt(),
                buffer.readBoolean()
        );
    }
}
