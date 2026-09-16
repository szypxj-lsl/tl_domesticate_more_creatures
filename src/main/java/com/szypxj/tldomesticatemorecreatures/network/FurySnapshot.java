package com.szypxj.tldomesticatemorecreatures.network;

import net.minecraft.network.FriendlyByteBuf;

public record FurySnapshot(boolean available, double anger, double maxAnger, boolean berserk) {
    public static final FurySnapshot NONE = new FurySnapshot(false, 0.0D, 0.0D, false);

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(available);
        if (!available) {
            return;
        }
        buffer.writeDouble(anger);
        buffer.writeDouble(maxAnger);
        buffer.writeBoolean(berserk);
    }

    public static FurySnapshot decode(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return NONE;
        }
        return new FurySnapshot(true, buffer.readDouble(), buffer.readDouble(), buffer.readBoolean());
    }
}
