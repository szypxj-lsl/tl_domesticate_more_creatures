package com.szypxj.tldomesticatemorecreatures.network;

import net.minecraft.network.FriendlyByteBuf;

public record StatSnapshot(
        String id,
        String nameKey,
        String icon,
        int totalPoints,
        int wildPoints,
        int trainedPoints,
        int talentPoints,
        double displayedValue,
        String displayFormat,
        boolean available,
        boolean canAllocate,
        int maxPoints,
        boolean dynamicDisplay,
        double currentValue,
        double maxValue
) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(id);
        buffer.writeUtf(nameKey);
        buffer.writeUtf(icon);
        buffer.writeVarInt(totalPoints);
        buffer.writeVarInt(wildPoints);
        buffer.writeVarInt(trainedPoints);
        buffer.writeVarInt(talentPoints);
        buffer.writeDouble(displayedValue);
        buffer.writeUtf(displayFormat);
        buffer.writeBoolean(available);
        buffer.writeBoolean(canAllocate);
        buffer.writeInt(maxPoints);
        buffer.writeBoolean(dynamicDisplay);
        buffer.writeDouble(currentValue);
        buffer.writeDouble(maxValue);
    }

    public static StatSnapshot decode(FriendlyByteBuf buffer) {
        return new StatSnapshot(
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readDouble(),
                buffer.readUtf(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readInt(),
                buffer.readBoolean(),
                buffer.readDouble(),
                buffer.readDouble()
        );
    }
}
