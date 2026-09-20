package com.szypxj.tldomesticatemorecreatures.compat.fossil;

import com.szypxj.tldomesticatemorecreatures.api.creature.compat.CreatureCompatProfileRegistry;
import com.szypxj.tldomesticatemorecreatures.riding.provider.RideCompatibilityApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;

/** Optional Fossils & Archeology integration. */
public final class FossilCompat {
    private static boolean registered;
    private FossilCompat() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        FossilThreatProvider.register();
        FossilRideControlProvider.register();
        FossilTamingInfoProvider.register();
        CreatureCompatProfileRegistry.register(new FossilCompatProfileProvider());
        RideCompatibilityApi.registerNativeRideProvider(new RideCompatibilityApi.NativeRideProvider() {
            @Override public boolean supports(LivingEntity entity) { return isPrehistoric(entity); }
            @Override public boolean hasNativePlayerControl(LivingEntity entity) { return isNativeRideable(entity); }
        });
    }

    private static boolean isPrehistoric(LivingEntity entity) {
        ResourceLocation id = entity == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null || !"fossil".equals(id.getNamespace())) return false;
        Class<?> type = entity.getClass();
        while (type != null) {
            if ("com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.Prehistoric".equals(type.getName())) return true;
            type = type.getSuperclass();
        }
        return false;
    }

    static boolean isNativeRideable(LivingEntity entity) {
        if (!isPrehistoric(entity)) return false;
        try {
            Method infoMethod = entity.getClass().getMethod("info");
            Object info = infoMethod.invoke(entity);
            Method dataMethod = info.getClass().getMethod("data");
            Object data = dataMethod.invoke(info);
            Object value = data.getClass().getMethod("canBeRidden").invoke(data);
            return value instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }
}
