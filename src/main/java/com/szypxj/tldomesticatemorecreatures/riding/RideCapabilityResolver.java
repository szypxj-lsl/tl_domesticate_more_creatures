package com.szypxj.tldomesticatemorecreatures.riding;

import com.szypxj.tldomesticatemorecreatures.riding.capability.RideCapability;
import com.szypxj.tldomesticatemorecreatures.riding.capability.RideCapabilityProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import com.szypxj.tldomesticatemorecreatures.riding.control.FlightRideController;
import com.szypxj.tldomesticatemorecreatures.riding.control.GroundRideController;
import com.szypxj.tldomesticatemorecreatures.riding.control.SwimRideController;
import com.szypxj.tldomesticatemorecreatures.riding.provider.RideCompatibilityApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;

import java.util.concurrent.ConcurrentHashMap;

public final class RideCapabilityResolver {
    private static final GroundRideController GROUND = new GroundRideController();
    private static final FlightRideController FLIGHT = new FlightRideController();
    private static final SwimRideController SWIM = new SwimRideController();
    private static final ConcurrentHashMap<Class<?>, RideCapabilityProfile> CACHE = new ConcurrentHashMap<>();

    private RideCapabilityResolver() {
    }

    public static RideEnvironment environment(Mob mount, EntityRideProfile profile) {
        boolean swim = canSwim(mount, profile);
        boolean flight = canFlight(mount, profile);
        if (mount.isInWaterOrBubble() && swim) {
            return RideEnvironment.WATER;
        }
        RideMovementMode mode = profile.movementMode();
        if (mode == RideMovementMode.FLIGHT || mode == RideMovementMode.FLIGHT_SWIM) {
            return RideEnvironment.AIR;
        }
        if (flight && !mount.onGround()) {
            return RideEnvironment.AIR;
        }
        return RideEnvironment.GROUND;
    }

    public static RideCapability resolve(Mob mount, EntityRideProfile profile, RideEnvironment environment) {
        return switch (environment) {
            case GROUND -> ground(mount, profile);
            case AIR -> flight(mount, profile);
            case WATER -> swim(mount, profile);
        };
    }

    public static RideCapabilityProfile profile(Mob mount, EntityRideProfile profile) {
        RideMovementMode mode = profile.movementMode();
        if (mode != RideMovementMode.AUTO) {
            return new RideCapabilityProfile(
                    mode == RideMovementMode.GROUND || mode == RideMovementMode.GROUND_SWIM,
                    mode == RideMovementMode.FLIGHT || mode == RideMovementMode.FLIGHT_SWIM,
                    mode == RideMovementMode.SWIM || mode == RideMovementMode.GROUND_SWIM || mode == RideMovementMode.FLIGHT_SWIM
            );
        }
        return CACHE.computeIfAbsent(mount.getClass(), ignored -> new RideCapabilityProfile(
                true,
                mount instanceof FlyingAnimal || mount.getNavigation() instanceof FlyingPathNavigation,
                mount instanceof WaterAnimal || mount.getNavigation() instanceof WaterBoundPathNavigation
        ));
    }

    public static RideCapabilityProfile profile(Mob mount) {
        return CACHE.computeIfAbsent(mount.getClass(), ignored -> new RideCapabilityProfile(
                true,
                mount instanceof FlyingAnimal || mount.getNavigation() instanceof FlyingPathNavigation,
                mount instanceof WaterAnimal || mount.getNavigation() instanceof WaterBoundPathNavigation
        ));
    }

    public static void invalidate() {
        CACHE.clear();
    }

    private static boolean canFlight(Mob mount, EntityRideProfile profile) {
        RideMovementMode mode = profile.movementMode();
        if (mode == RideMovementMode.FLIGHT || mode == RideMovementMode.FLIGHT_SWIM) return true;
        if (mode != RideMovementMode.AUTO) return false;
        if (!RideCompatibilityApi.flightRideProviders().isEmpty()) {
            for (RideCompatibilityApi.FlightRideProvider provider : RideCompatibilityApi.flightRideProviders()) {
                if (RideCompatibilityApi.enabled(provider)) {
                    try {
                        if (provider.supports(mount)) return true;
                    } catch (RuntimeException ignored) {
                        RideCompatibilityApi.disableProvider(provider);
                    }
                }
            }
        }
        return mount instanceof FlyingAnimal || mount.getNavigation() instanceof FlyingPathNavigation;
    }

    private static boolean canSwim(Mob mount, EntityRideProfile profile) {
        RideMovementMode mode = profile.movementMode();
        if (mode == RideMovementMode.SWIM || mode == RideMovementMode.GROUND_SWIM || mode == RideMovementMode.FLIGHT_SWIM) return true;
        if (mode != RideMovementMode.AUTO) return false;
        for (RideCompatibilityApi.SwimRideProvider provider : RideCompatibilityApi.swimRideProviders()) {
            if (RideCompatibilityApi.enabled(provider)) {
                try {
                    if (provider.supports(mount)) return true;
                } catch (RuntimeException ignored) {
                    RideCompatibilityApi.disableProvider(provider);
                }
            }
        }
        return mount instanceof WaterAnimal || mount.getNavigation() instanceof WaterBoundPathNavigation;
    }

    private static RideCapability ground(Mob mount, EntityRideProfile profile) {
        for (RideCompatibilityApi.GroundRideProvider provider : RideCompatibilityApi.groundRideProviders()) {
            if (!RideCompatibilityApi.enabled(provider)) continue;
            try {
                if (provider.supports(mount)) return new GroundProviderCapability(provider);
            } catch (RuntimeException ignored) {
                RideCompatibilityApi.disableProvider(provider);
            }
        }
        return GROUND.supports(mount, profile) ? GROUND : null;
    }

    private static RideCapability flight(Mob mount, EntityRideProfile profile) {
        for (RideCompatibilityApi.FlightRideProvider provider : RideCompatibilityApi.flightRideProviders()) {
            if (!RideCompatibilityApi.enabled(provider)) continue;
            try {
                if (provider.supports(mount)) return new FlightProviderCapability(provider);
            } catch (RuntimeException ignored) {
                RideCompatibilityApi.disableProvider(provider);
            }
        }
        return canFlight(mount, profile) && FLIGHT.supports(mount, profile) ? FLIGHT : null;
    }

    private static RideCapability swim(Mob mount, EntityRideProfile profile) {
        for (RideCompatibilityApi.SwimRideProvider provider : RideCompatibilityApi.swimRideProviders()) {
            if (!RideCompatibilityApi.enabled(provider)) continue;
            try {
                if (provider.supports(mount)) return new SwimProviderCapability(provider);
            } catch (RuntimeException ignored) {
                RideCompatibilityApi.disableProvider(provider);
            }
        }
        return canSwim(mount, profile) && SWIM.supports(mount, profile) ? SWIM : null;
    }

    private record GroundProviderCapability(RideCompatibilityApi.GroundRideProvider provider) implements RideCapability {
        @Override public String id() { return "provider_ground:" + provider.getClass().getName(); }
        @Override public boolean supports(Mob mount, EntityRideProfile profile) { return provider.supports(mount); }
        @Override public RideEnvironment environment() { return RideEnvironment.GROUND; }
        @Override public boolean tick(ServerPlayer rider, Mob mount, RideInputState input, EntityRideProfile profile, RideRuntimeState runtime) { return provider.tick(rider, mount, input, profile, runtime); }
        @Override public void stop(Mob mount, RideRuntimeState runtime) { provider.stop(mount, runtime); }
    }

    private record FlightProviderCapability(RideCompatibilityApi.FlightRideProvider provider) implements RideCapability {
        @Override public String id() { return "provider_flight:" + provider.getClass().getName(); }
        @Override public boolean supports(Mob mount, EntityRideProfile profile) { return provider.supports(mount); }
        @Override public RideEnvironment environment() { return RideEnvironment.AIR; }
        @Override public boolean tick(ServerPlayer rider, Mob mount, RideInputState input, EntityRideProfile profile, RideRuntimeState runtime) { return provider.tick(rider, mount, input, profile, runtime); }
        @Override public void stop(Mob mount, RideRuntimeState runtime) { provider.stop(mount, runtime); }
    }

    private record SwimProviderCapability(RideCompatibilityApi.SwimRideProvider provider) implements RideCapability {
        @Override public String id() { return "provider_swim:" + provider.getClass().getName(); }
        @Override public boolean supports(Mob mount, EntityRideProfile profile) { return provider.supports(mount); }
        @Override public RideEnvironment environment() { return RideEnvironment.WATER; }
        @Override public boolean tick(ServerPlayer rider, Mob mount, RideInputState input, EntityRideProfile profile, RideRuntimeState runtime) { return provider.tick(rider, mount, input, profile, runtime); }
        @Override public void stop(Mob mount, RideRuntimeState runtime) { provider.stop(mount, runtime); }
    }
}
