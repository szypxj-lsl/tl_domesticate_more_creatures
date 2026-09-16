package com.szypxj.tldomesticatemorecreatures.command.pet.provider;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommand;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandRuntimeState;
import com.szypxj.tldomesticatemorecreatures.command.pet.capability.PetCommandCapability;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class PetCommandCompatibilityApi {
    private static final CopyOnWriteArrayList<OwnershipProvider> OWNERSHIP = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<MovementProvider> MOVEMENT = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<FollowProvider> FOLLOW = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<CombatProvider> COMBAT = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<DefenseProvider> DEFENSE = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<FlightProvider> FLIGHT = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<LandingProvider> LANDING = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<NativeStateProvider> NATIVE_STATE = new CopyOnWriteArrayList<>();
    private static final AtomicLong GENERATION = new AtomicLong();

    private PetCommandCompatibilityApi() {
    }

    public static void registerOwnershipProvider(OwnershipProvider provider) {
        OWNERSHIP.add(Objects.requireNonNull(provider, "provider"));
        GENERATION.incrementAndGet();
    }

    public static void registerMovementProvider(MovementProvider provider) {
        MOVEMENT.add(Objects.requireNonNull(provider, "provider"));
        GENERATION.incrementAndGet();
    }

    public static void registerFollowProvider(FollowProvider provider) {
        FOLLOW.add(Objects.requireNonNull(provider, "provider"));
        GENERATION.incrementAndGet();
    }

    public static void registerCombatProvider(CombatProvider provider) {
        COMBAT.add(Objects.requireNonNull(provider, "provider"));
        GENERATION.incrementAndGet();
    }

    public static void registerDefenseProvider(DefenseProvider provider) {
        DEFENSE.add(Objects.requireNonNull(provider, "provider"));
        GENERATION.incrementAndGet();
    }

    public static void registerFlightProvider(FlightProvider provider) {
        FLIGHT.add(Objects.requireNonNull(provider, "provider"));
        GENERATION.incrementAndGet();
    }

    public static void registerLandingProvider(LandingProvider provider) {
        LANDING.add(Objects.requireNonNull(provider, "provider"));
        GENERATION.incrementAndGet();
    }

    public static void registerNativeStateProvider(NativeStateProvider provider) {
        NATIVE_STATE.add(Objects.requireNonNull(provider, "provider"));
        GENERATION.incrementAndGet();
    }

    public static List<OwnershipProvider> ownershipProviders() {
        return List.copyOf(OWNERSHIP);
    }

    public static List<MovementProvider> movementProviders() {
        return List.copyOf(MOVEMENT);
    }

    public static List<FollowProvider> followProviders() {
        return List.copyOf(FOLLOW);
    }

    public static List<CombatProvider> combatProviders() {
        return List.copyOf(COMBAT);
    }

    public static List<DefenseProvider> defenseProviders() {
        return List.copyOf(DEFENSE);
    }

    public static List<FlightProvider> flightProviders() {
        return List.copyOf(FLIGHT);
    }

    public static List<LandingProvider> landingProviders() {
        return List.copyOf(LANDING);
    }

    public static List<NativeStateProvider> nativeStateProviders() {
        return List.copyOf(NATIVE_STATE);
    }

    public static long generation() {
        return GENERATION.get();
    }

    public interface OwnershipProvider {
        boolean supports(LivingEntity entity);

        boolean isOwnedBy(LivingEntity entity, Player player);
    }

    public interface MovementProvider extends PetCommandCapability {
        boolean supports(Mob mob);
    }

    public interface FollowProvider extends PetCommandCapability {
        boolean supports(Mob mob);
    }

    public interface CombatProvider extends PetCommandCapability {
        boolean supports(Mob mob);
    }

    public interface DefenseProvider extends PetCommandCapability {
        boolean supports(Mob mob);
    }

    public interface FlightProvider {
        boolean supports(Mob mob);

        boolean isAirborne(Mob mob);
    }

    public interface LandingProvider extends PetCommandCapability {
        boolean supports(Mob mob);
    }

    public interface NativeStateProvider {
        String id();

        boolean supports(Mob mob, PetCommand command);

        boolean enter(Mob mob, PetCommand command, PetCommandRuntimeState runtime);

        boolean maintain(Mob mob, PetCommand command, PetCommandRuntimeState runtime);

        void restore(Mob mob, PetCommand command, PetCommandRuntimeState runtime);
    }
}
