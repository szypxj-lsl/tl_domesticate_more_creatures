package com.szypxj.tldomesticatemorecreatures.inventory;

import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public final class PlayerExtraInventoryManager {
    private static final Map<Player, PlayerExtraInventory> CACHE = Collections.synchronizedMap(new IdentityHashMap<>());

    private PlayerExtraInventoryManager() {
    }

    public static PlayerExtraInventory get(Player player) {
        return CACHE.computeIfAbsent(player, PlayerExtraInventory::new);
    }

    public static void reload(Player player) {
        if (player == null) {
            return;
        }
        CACHE.put(player, new PlayerExtraInventory(player));
    }

    public static void invalidate(Player player) {
        if (player != null) {
            CACHE.remove(player);
        }
    }
}
