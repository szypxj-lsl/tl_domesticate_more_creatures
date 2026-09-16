package com.szypxj.tldomesticatemorecreatures.api.level;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

public final class PlayerLevelChangedEvent extends Event {
    private final ServerPlayer player;
    private final int oldLevel;
    private final int newLevel;

    public PlayerLevelChangedEvent(ServerPlayer player, int oldLevel, int newLevel) {
        this.player = player;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public ServerPlayer player() {
        return player;
    }

    public int oldLevel() {
        return oldLevel;
    }

    public int newLevel() {
        return newLevel;
    }
}
