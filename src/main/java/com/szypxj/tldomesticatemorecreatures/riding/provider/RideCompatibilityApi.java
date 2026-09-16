package com.szypxj.tldomesticatemorecreatures.riding.provider;

import com.szypxj.tldomesticatemorecreatures.riding.NativeRideCapabilityResolver;
import com.szypxj.tldomesticatemorecreatures.riding.RideCapabilityResolver;
import com.szypxj.tldomesticatemorecreatures.riding.RideInputState;
import com.szypxj.tldomesticatemorecreatures.riding.RideRuntimeState;
import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.RiderVisualProfile;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class RideCompatibilityApi {
    private static final CopyOnWriteArrayList<RideEligibilityProvider> ELIGIBILITY = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<NativeRideProvider> NATIVE = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<GroundRideProvider> GROUND = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<FlightRideProvider> FLIGHT = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<SwimRideProvider> SWIM = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<JumpRideProvider> JUMP = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<RideAbilityProvider> ABILITY = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<SeatProfileProvider> SEAT = new CopyOnWriteArrayList<>();
    private static final Set<Class<?>> DISABLED_PROVIDER_CLASSES = ConcurrentHashMap.newKeySet();
    private static final AtomicLong GENERATION = new AtomicLong();

    private RideCompatibilityApi() {
    }

    public static void registerEligibilityProvider(RideEligibilityProvider provider) {
        ELIGIBILITY.add(Objects.requireNonNull(provider));
        changed();
    }

    public static void registerNativeRideProvider(NativeRideProvider provider) {
        NATIVE.add(Objects.requireNonNull(provider));
        changed();
    }

    public static void registerGroundRideProvider(GroundRideProvider provider) {
        GROUND.add(Objects.requireNonNull(provider));
        changed();
    }

    public static void registerFlightRideProvider(FlightRideProvider provider) {
        FLIGHT.add(Objects.requireNonNull(provider));
        changed();
    }

    public static void registerSwimRideProvider(SwimRideProvider provider) {
        SWIM.add(Objects.requireNonNull(provider));
        changed();
    }

    public static void registerJumpRideProvider(JumpRideProvider provider) {
        JUMP.add(Objects.requireNonNull(provider));
        changed();
    }

    public static void registerRideAbilityProvider(RideAbilityProvider provider) {
        ABILITY.add(Objects.requireNonNull(provider));
        changed();
    }

    public static void registerSeatProfileProvider(SeatProfileProvider provider) {
        SEAT.add(Objects.requireNonNull(provider));
        changed();
    }

    public static List<RideEligibilityProvider> eligibilityProviders() {
        return List.copyOf(ELIGIBILITY);
    }

    public static List<NativeRideProvider> nativeRideProviders() {
        return List.copyOf(NATIVE);
    }

    public static List<GroundRideProvider> groundRideProviders() {
        return List.copyOf(GROUND);
    }

    public static List<FlightRideProvider> flightRideProviders() {
        return List.copyOf(FLIGHT);
    }

    public static List<SwimRideProvider> swimRideProviders() {
        return List.copyOf(SWIM);
    }

    public static List<JumpRideProvider> jumpRideProviders() {
        return List.copyOf(JUMP);
    }

    public static List<RideAbilityProvider> rideAbilityProviders() {
        return List.copyOf(ABILITY);
    }

    public static List<SeatProfileProvider> seatProfileProviders() {
        return List.copyOf(SEAT);
    }

    public static long generation() {
        return GENERATION.get();
    }

    public static boolean enabled(Object provider) {
        return provider != null && !DISABLED_PROVIDER_CLASSES.contains(provider.getClass());
    }

    public static boolean disableProvider(Object provider) {
        return provider != null && DISABLED_PROVIDER_CLASSES.add(provider.getClass());
    }

    private static void changed() {
        GENERATION.incrementAndGet();
        NativeRideCapabilityResolver.invalidate();
        RideCapabilityResolver.invalidate();
    }

    public enum Decision {
        ALLOW, DENY, PASS
    }

    public interface RideEligibilityProvider {
        boolean supports(LivingEntity entity);
        Decision evaluate(LivingEntity entity, Player rider);
    }

    public interface NativeRideProvider {
        boolean supports(LivingEntity entity);
        boolean hasNativePlayerControl(LivingEntity entity);
    }

    public interface GroundRideProvider {
        boolean supports(Mob mount);
        boolean tick(ServerPlayer rider, Mob mount, RideInputState input, EntityRideProfile profile, RideRuntimeState runtime);
        void stop(Mob mount, RideRuntimeState runtime);
    }

    public interface FlightRideProvider {
        boolean supports(Mob mount);
        boolean tick(ServerPlayer rider, Mob mount, RideInputState input, EntityRideProfile profile, RideRuntimeState runtime);
        void stop(Mob mount, RideRuntimeState runtime);
    }

    public interface SwimRideProvider {
        boolean supports(Mob mount);
        boolean tick(ServerPlayer rider, Mob mount, RideInputState input, EntityRideProfile profile, RideRuntimeState runtime);
        void stop(Mob mount, RideRuntimeState runtime);
    }

    public interface JumpRideProvider {
        boolean supports(Mob mount);
        boolean jump(Mob mount, double strength);
    }

    public interface RideAbilityProvider {
        boolean supports(LivingEntity mount);
    }

    public interface SeatProfileProvider {
        boolean supports(LivingEntity mount);
        RiderVisualProfile profile(LivingEntity mount);
    }
}
