package com.szypxj.tldomesticatemorecreatures.api.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;

public final class ErsCompatApi {
    public static final double MAX_HUNGER = 100.0D;

    private ErsCompatApi() {
    }

    public static boolean isErsEntity(LivingEntity entity) {
        return entity instanceof ErsTamableBridge;
    }

    public static boolean usesNativeHunger(LivingEntity entity) {
        return entity instanceof ErsTamableBridge bridge && bridge.tdmc$usesErsHunger();
    }

    public static double currentHunger(LivingEntity entity) {
        if (!(entity instanceof ErsTamableBridge bridge) || !bridge.tdmc$usesErsHunger()) {
            return 0.0D;
        }
        return clampHunger(bridge.tdmc$getErsHunger());
    }

    public static double maxHunger(LivingEntity entity) {
        return usesNativeHunger(entity) ? MAX_HUNGER : 0.0D;
    }

    public static boolean setHunger(LivingEntity entity, double value) {
        if (!(entity instanceof ErsTamableBridge bridge) || !bridge.tdmc$usesErsHunger() || !Double.isFinite(value)) {
            return false;
        }
        bridge.tdmc$setErsHunger((float) clampHunger(value));
        return true;
    }

    public static boolean addHunger(LivingEntity entity, double amount) {
        if (!Double.isFinite(amount) || !usesNativeHunger(entity)) {
            return false;
        }
        return setHunger(entity, currentHunger(entity) + amount);
    }

    public static boolean hasNativeInventory(LivingEntity entity) {
        return isErsEntity(entity) && entity instanceof HasCustomInventoryScreen;
    }

    public static boolean openNativeInventory(ServerPlayer player, LivingEntity entity) {
        if (player == null || !hasNativeInventory(entity) || !(entity instanceof HasCustomInventoryScreen inventoryScreen)) {
            return false;
        }
        inventoryScreen.openCustomInventoryScreen(player);
        return true;
    }

    public static boolean isExternalElite(LivingEntity entity) {
        return entity instanceof ErsEliteBridge bridge && bridge.tdmc$isErsElite();
    }

    private static double clampHunger(double value) {
        return Math.max(0.0D, Math.min(MAX_HUNGER, value));
    }
}
