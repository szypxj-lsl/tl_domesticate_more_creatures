package com.szypxj.tldomesticatemorecreatures.riding;

import com.mojang.logging.LogUtils;
import com.szypxj.tldomesticatemorecreatures.riding.provider.RideCompatibilityApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NativeRideCapabilityResolver {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ConcurrentHashMap<Class<?>, NativeRideStatus> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> ROR_NATIVE_RIDEABLES = Set.of(
            "giganotosaurus",
            "spinosaurus",
            "tyrannosaurus"
    );

    private NativeRideCapabilityResolver() {
    }

    public static NativeRideStatus status(LivingEntity entity) {
        if (entity == null) {
            return NativeRideStatus.UNKNOWN;
        }
        if (entity instanceof NativeRideMarker || isKnownNativeRideType(entity)) {
            return NativeRideStatus.NATIVE;
        }
        for (RideCompatibilityApi.NativeRideProvider provider : RideCompatibilityApi.nativeRideProviders()) {
            if (!RideCompatibilityApi.enabled(provider)) {
                continue;
            }
            try {
                if (provider.supports(entity) && provider.hasNativePlayerControl(entity)) {
                    return NativeRideStatus.NATIVE;
                }
            } catch (RuntimeException exception) {
                if (RideCompatibilityApi.disableProvider(provider)) {
                    LOGGER.warn("Native ride provider {} failed and was disabled.", provider.getClass().getName(), exception);
                }
            }
        }
        return CACHE.computeIfAbsent(entity.getClass(), NativeRideCapabilityResolver::discover);
    }

    public static boolean hasNativePlayerControl(LivingEntity entity) {
        return status(entity) == NativeRideStatus.NATIVE;
    }

    public static void invalidate() {
        CACHE.clear();
    }

    private static boolean isKnownNativeRideType(LivingEntity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null
                && "ror".equals(id.getNamespace())
                && ROR_NATIVE_RIDEABLES.contains(id.getPath());
    }

    private static NativeRideStatus discover(Class<?> type) {
        int vanillaRideHooks = 0;
        if (declaresBelowMob(type, "getRiddenInput", Player.class, Vec3.class)) {
            vanillaRideHooks++;
        }
        if (declaresBelowMob(type, "getRiddenSpeed", Player.class)) {
            vanillaRideHooks++;
        }
        if (declaresBelowMob(type, "tickRidden", Player.class, Vec3.class)) {
            vanillaRideHooks++;
        }
        if (vanillaRideHooks >= 2) {
            return NativeRideStatus.NATIVE;
        }

        int explicitRiderControlHooks = 0;
        if (declaresBelowMob(type, "applyRiderMovementInput", Player.class, float.class, float.class, boolean.class)) {
            explicitRiderControlHooks++;
        }
        if (declaresBelowMob(type, "onRiderTakeoffRequest", Player.class)) {
            explicitRiderControlHooks++;
        }
        if (declaresBelowMob(type, "onRiderAbilityUse", Player.class, String.class)) {
            explicitRiderControlHooks++;
        }
        if (explicitRiderControlHooks >= 2) {
            return NativeRideStatus.NATIVE;
        }

        return Mob.class.isAssignableFrom(type) ? NativeRideStatus.GENERIC_CANDIDATE : NativeRideStatus.UNKNOWN;
    }

    private static boolean declaresBelowMob(Class<?> type, String name, Class<?>... parameters) {
        Class<?> current = type;
        while (current != null && current != Mob.class && current != LivingEntity.class && current != Object.class) {
            try {
                Method ignored = current.getDeclaredMethod(name, parameters);
                return true;
            } catch (NoSuchMethodException exception) {
                current = current.getSuperclass();
            }
        }
        return false;
    }

    public enum NativeRideStatus {
        NATIVE, GENERIC_CANDIDATE, UNKNOWN
    }
}
