package com.szypxj.tldomesticatemorecreatures.api.riding;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RideActionApi {
    private static final Map<ResourceLocation, RideActionGuard> GUARDS = new ConcurrentHashMap<>();

    private RideActionApi() {
    }

    public static void register(ResourceLocation id, RideActionGuard guard) {
        if (id == null || guard == null) {
            throw new IllegalArgumentException("Ride action guard id and guard must not be null");
        }
        RideActionGuard previous = GUARDS.putIfAbsent(id, guard);
        if (previous != null) {
            throw new IllegalArgumentException("Ride action guard already registered: " + id);
        }
    }

    public static boolean allows(Player rider, LivingEntity mount, RideAction action) {
        if (rider == null || mount == null || action == null) {
            return false;
        }
        for (RideActionGuard guard : GUARDS.values()) {
            try {
                if (!guard.allows(rider, mount, action)) {
                    return false;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return true;
    }

    public static void clearForTests() {
        GUARDS.clear();
    }
}
