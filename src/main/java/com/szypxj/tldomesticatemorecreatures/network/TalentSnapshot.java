package com.szypxj.tldomesticatemorecreatures.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record TalentSnapshot(String id, String nameKey, int level, boolean special, List<Effect> effects) {
    public TalentSnapshot(String id, String nameKey, int level, List<Effect> effects) {
        this(id, nameKey, level, false, effects);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(id);
        buffer.writeUtf(nameKey);
        buffer.writeVarInt(level);
        buffer.writeBoolean(special);
        buffer.writeVarInt(effects.size());
        for (Effect effect : effects) {
            buffer.writeUtf(effect.statNameKey());
            buffer.writeVarInt(effect.points());
        }
    }

    public static TalentSnapshot decode(FriendlyByteBuf buffer) {
        String id = buffer.readUtf();
        String nameKey = buffer.readUtf();
        int level = buffer.readVarInt();
        boolean special = buffer.readBoolean();
        int size = buffer.readVarInt();
        List<Effect> effects = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            effects.add(new Effect(buffer.readUtf(), buffer.readVarInt()));
        }
        return new TalentSnapshot(id, nameKey, level, special, List.copyOf(effects));
    }

    public record Effect(String statNameKey, int points) {
    }
}
