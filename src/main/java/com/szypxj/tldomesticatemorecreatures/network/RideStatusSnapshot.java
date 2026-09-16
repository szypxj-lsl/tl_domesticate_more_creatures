package com.szypxj.tldomesticatemorecreatures.network;

import net.minecraft.network.FriendlyByteBuf;

public record RideStatusSnapshot(
        boolean available,
        String statusKey,
        String movementKey
) {
    public static final RideStatusSnapshot NONE = new RideStatusSnapshot(false, "", "");

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(available);
        buffer.writeUtf(statusKey == null ? "" : statusKey, 256);
        buffer.writeUtf(movementKey == null ? "" : movementKey, 256);
    }

    public static RideStatusSnapshot decode(FriendlyByteBuf buffer) {
        return new RideStatusSnapshot(
                buffer.readBoolean(),
                buffer.readUtf(256),
                buffer.readUtf(256)
        );
    }
}
