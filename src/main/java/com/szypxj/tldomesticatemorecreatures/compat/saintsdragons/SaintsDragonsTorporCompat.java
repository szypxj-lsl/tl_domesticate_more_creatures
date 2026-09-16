package com.szypxj.tldomesticatemorecreatures.compat.saintsdragons;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SaintsDragonsTorporCompat {
    private static final String MOD_ID = "saintsdragons";
    private static final Map<Class<?>, Access> ACCESS_CACHE = new ConcurrentHashMap<>();
    private static final Access UNSUPPORTED = new Access(null, null, null);

    private SaintsDragonsTorporCompat() {
    }

    public static boolean maintainNativeStun(LivingEntity entity) {
        if (entity == null || !ModList.get().isLoaded(MOD_ID)) {
            return false;
        }
        Access access = ACCESS_CACHE.computeIfAbsent(entity.getClass(), SaintsDragonsTorporCompat::resolve);
        if (!access.supported()) {
            return false;
        }
        try {
            access.setRecoveryTarget().invoke(entity, 0.0F);
            Object stunned = access.isTamingStunned().invoke(entity);
            return stunned instanceof Boolean value && value;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    public static void clearNativeStun(LivingEntity entity) {
        if (entity == null || !ModList.get().isLoaded(MOD_ID)) {
            return;
        }
        Access access = ACCESS_CACHE.computeIfAbsent(entity.getClass(), SaintsDragonsTorporCompat::resolve);
        if (!access.supported()) {
            return;
        }
        try {
            access.clearRecovery().invoke(entity);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static Access resolve(Class<?> type) {
        try {
            Method isTamingStunned = type.getMethod("isTamingStunned");
            Method setTamingRecoveryTarget = type.getMethod("setTamingRecoveryTarget", float.class);
            Method clearTamingRecovery = type.getMethod("clearTamingRecovery");
            return new Access(isTamingStunned, setTamingRecoveryTarget, clearTamingRecovery);
        } catch (NoSuchMethodException ignored) {
            return UNSUPPORTED;
        }
    }

    private record Access(Method isTamingStunned, Method setRecoveryTarget, Method clearRecovery) {
        private boolean supported() {
            return isTamingStunned != null && setRecoveryTarget != null && clearRecovery != null;
        }
    }
}
