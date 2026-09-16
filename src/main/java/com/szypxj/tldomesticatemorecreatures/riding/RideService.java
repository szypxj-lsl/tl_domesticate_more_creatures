package com.szypxj.tldomesticatemorecreatures.riding;

import com.szypxj.tldomesticatemorecreatures.domestication.DomesticationData;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.riding.capability.RideCapability;
import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingConfigManager;
import com.szypxj.tldomesticatemorecreatures.talent.FuryService;
import com.szypxj.tldomesticatemorecreatures.talent.active.ActiveTalentService;
import com.szypxj.tldomesticatemorecreatures.torpor.TorporData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class RideService {
    private static final String GENERIC_RIDE_RIDER_TAG = "tdmcGenericRideRider";
    private static final String SIT_CAPTURED_TAG = "tdmcRideSitCaptured";
    private static final String PREVIOUS_ORDERED_TO_SIT_TAG = "tdmcRidePreviousOrderedToSit";
    private static final Map<LivingEntity, RideRuntimeState> ACTIVE = Collections.synchronizedMap(new WeakHashMap<>());

    private RideService() {
    }

    public static InteractionResult tryMount(ServerPlayer rider, LivingEntity mount) {
        if (rider == null || mount == null || mount.level() != rider.level()) {
            return InteractionResult.PASS;
        }
        RideEligibilityResult result = RideEligibilityResolver.evaluate(mount, rider);
        if (result == RideEligibilityResult.NATIVE_BYPASS) {
            if (!mount.getPassengers().isEmpty() || rider.isPassenger()) {
                return InteractionResult.FAIL;
            }
            return rider.startRiding(mount, true) ? InteractionResult.CONSUME : InteractionResult.FAIL;
        }
        if (!(mount instanceof Mob) || result != RideEligibilityResult.ALLOW_GENERIC || !mayRide(rider, mount)) {
            return InteractionResult.FAIL;
        }
        if (!mount.getPassengers().isEmpty() || rider.isPassenger()) {
            return InteractionResult.FAIL;
        }
        if (!rider.startRiding(mount, true)) {
            return InteractionResult.FAIL;
        }
        ACTIVE.put(mount, new RideRuntimeState(rider.getUUID(), RidingConfigManager.generation()));
        markGenericRide(mount, rider.getUUID());
        prepareMountedBehavior(mount);
        NetworkHandler.sendRideState(rider, mount, true, RideEnvironment.GROUND, RidingConfigManager.generation());
        return InteractionResult.CONSUME;
    }

    public static boolean trySummonMount(ServerPlayer rider, LivingEntity mount) {
        if (rider == null || mount == null || rider.level() != mount.level() || !mayRide(rider, mount)) return false;
        if (!mount.getPassengers().isEmpty()) return false;
        if (rider.isPassenger()) rider.stopRiding();
        RideEligibilityResult result = RideEligibilityResolver.evaluate(mount, rider);
        if (result == RideEligibilityResult.ALLOW_GENERIC) {
            return tryMount(rider, mount).consumesAction();
        }
        if (result == RideEligibilityResult.NATIVE_BYPASS) {
            return rider.startRiding(mount, true);
        }
        return false;
    }

    public static boolean isGenericControlled(LivingEntity mount) {
        RideRuntimeState runtime = ACTIVE.get(mount);
        return runtime != null;
    }

    public static boolean isGenericRider(Player rider) {
        if (rider == null || !(rider.getVehicle() instanceof LivingEntity mount)) {
            return false;
        }
        RideRuntimeState runtime = ACTIVE.get(mount);
        return runtime != null && runtime.riderUuid().equals(rider.getUUID());
    }

    public static boolean mayOtherPlayerRide(Player rider, LivingEntity mount) {
        return rider != null
                && mount != null
                && PetOwnershipService.isTamed(mount)
                && !PetOwnershipService.isOwnedBy(mount, rider)
                && DomesticationData.of(mount).allowOtherRiders();
    }

    public static boolean mayRide(Player rider, LivingEntity mount) {
        return !FuryService.isBerserk(mount)
                && (PetOwnershipService.isOwnedBy(mount, rider) || mayOtherPlayerRide(rider, mount));
    }

    public static void acceptInput(ServerPlayer rider, int mountId, RideInputState input) {
        if (rider == null || !(rider.getVehicle() instanceof LivingEntity mount) || mount.getId() != mountId) {
            return;
        }
        RideRuntimeState runtime = ACTIVE.get(mount);
        if (runtime == null || !runtime.riderUuid().equals(rider.getUUID()) || input == null) {
            return;
        }
        if (input.sequence() <= runtime.lastAcceptedSequence()) {
            return;
        }
        runtime.lastAcceptedSequence(input.sequence());
        runtime.input(new RideInputState(
                Mth.clamp(input.forward(), -1.0F, 1.0F),
                Mth.clamp(input.strafe(), -1.0F, 1.0F),
                input.jump(),
                input.descend(),
                input.sequence()
        ));
    }

    public static RideRuntimeState runtime(LivingEntity mount) {
        return ACTIVE.get(mount);
    }

    public static void stopRide(LivingEntity mount, StopReason reason) {
        if (mount == null) {
            return;
        }
        RideRuntimeState runtime = ACTIVE.remove(mount);
        if (runtime == null) {
            return;
        }
        runtime.input(RideInputState.ZERO);
        runtime.lastMovementVector(net.minecraft.world.phys.Vec3.ZERO);
        if (runtime.activeCapability() != null && mount instanceof Mob mob) {
            try {
                runtime.activeCapability().stop(mob, runtime);
            } catch (RuntimeException ignored) {
            }
            runtime.activeCapability(null);
        }
        if (runtime.gravityCaptured()) {
            mount.setNoGravity(runtime.previousNoGravity());
            runtime.gravityCaptured(false);
        }

        ServerPlayer rider = null;
        if (!mount.level().isClientSide && mount.level() instanceof net.minecraft.server.level.ServerLevel level) {
            rider = level.getServer().getPlayerList().getPlayer(runtime.riderUuid());
            if (rider != null) {
                NetworkHandler.sendRideState(rider, mount, false, runtime.environment(), RidingConfigManager.generation());
                if (rider.getVehicle() == mount) {
                    rider.stopRiding();
                }
            }
        }

        restoreMountedBehavior(mount);
        boolean preserveReconnectMarker = reason == StopReason.RIDER_INVALID && rider == null;
        if (!preserveReconnectMarker) {
            clearGenericRideMarker(mount);
        }
    }

    public static boolean blocksMovementCommand(LivingEntity pet) {
        return isGenericControlled(pet);
    }

    public static boolean allowsAutomaticTarget(LivingEntity pet) {
        return pet == null || !isGenericControlled(pet) || RidingConfigManager.profile(pet.getType()).autoAttackWhileRidden();
    }

    public static void serverTick(MinecraftServer server) {
        recoverPersistedRides(server);
        long generation = RidingConfigManager.generation();
        for (LivingEntity mount : activeMountsSnapshot()) {
            RideRuntimeState runtime = ACTIVE.get(mount);
            if (runtime == null) {
                continue;
            }
            ServerPlayer rider = server.getPlayerList().getPlayer(runtime.riderUuid());
            StopReason reason = invalidReason(rider, mount);
            if (reason != null) {
                stopRide(mount, reason);
                continue;
            }
            maintainMountedBehavior(mount);
            if (runtime.jumpCooldownTicks() > 0) {
                runtime.jumpCooldownTicks(runtime.jumpCooldownTicks() - 1);
            }
            if (runtime.configGeneration() != generation) {
                runtime.configGeneration(generation);
                if (RideEligibilityResolver.evaluate(mount, rider) != RideEligibilityResult.ALLOW_GENERIC) {
                    stopRide(mount, StopReason.CONFIG_CHANGED);
                    continue;
                }
            }
            if (ActiveTalentService.blocksRideInput(mount)) {
                runtime.input(RideInputState.ZERO);
                continue;
            }
            if (mount instanceof Mob mob) {
                EntityRideProfile profile = RidingConfigManager.profile(mount.getType());
                RideEnvironment nextEnvironment = RideCapabilityResolver.environment(mob, profile);
                RideCapability capability = RideCapabilityResolver.resolve(mob, profile, nextEnvironment);
                if (capability == null) {
                    stopRide(mount, StopReason.CONFIG_CHANGED);
                    continue;
                }
                if (runtime.activeCapability() == null || !java.util.Objects.equals(runtime.activeCapability().id(), capability.id())) {
                    if (runtime.activeCapability() != null) {
                        try {
                            runtime.activeCapability().stop(mob, runtime);
                        } catch (RuntimeException ignored) {
                        }
                    }
                    runtime.activeCapability(capability);
                }
                if (runtime.environment() != nextEnvironment) {
                    runtime.environment(nextEnvironment);
                    NetworkHandler.sendRideState(rider, mount, true, nextEnvironment, generation);
                }
                capability.tick(rider, mob, runtime.input(), profile, runtime);
            }
        }
    }

    public static void clearAll() {
        for (LivingEntity mount : activeMountsSnapshot()) {
            stopRide(mount, StopReason.SERVER_STOPPING);
        }
        ACTIVE.clear();
    }

    private static void recoverPersistedRides(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayer rider : server.getPlayerList().getPlayers()) {
            if (!(rider.getVehicle() instanceof LivingEntity mount) || ACTIVE.containsKey(mount)) {
                continue;
            }
            RideEligibilityResult result = RideEligibilityResolver.evaluate(mount, rider);
            if (result == RideEligibilityResult.ALLOW_GENERIC && mount instanceof Mob mob && mayRide(rider, mount)) {
                EntityRideProfile profile = RidingConfigManager.profile(mount.getType());
                RideEnvironment environment = RideCapabilityResolver.environment(mob, profile);
                RideCapability capability = RideCapabilityResolver.resolve(mob, profile, environment);
                if (capability == null) {
                    rider.stopRiding();
                    restoreMountedBehavior(mount);
                    clearGenericRideMarker(mount);
                    continue;
                }
                RideRuntimeState runtime = new RideRuntimeState(rider.getUUID(), RidingConfigManager.generation());
                runtime.environment(environment);
                runtime.activeCapability(capability);
                ACTIVE.put(mount, runtime);
                markGenericRide(mount, rider.getUUID());
                prepareMountedBehavior(mount);
                NetworkHandler.sendRideState(rider, mount, true, environment, RidingConfigManager.generation());
                continue;
            }
            if (isMarkedGenericRide(mount, rider.getUUID()) && result == RideEligibilityResult.DENY) {
                rider.stopRiding();
                restoreMountedBehavior(mount);
                clearGenericRideMarker(mount);
            }
        }
    }

    private static void prepareMountedBehavior(LivingEntity mount) {
        if (mount instanceof TamableAnimal tamable) {
            var persistent = mount.getPersistentData();
            if (!persistent.getBoolean(SIT_CAPTURED_TAG)) {
                persistent.putBoolean(SIT_CAPTURED_TAG, true);
                persistent.putBoolean(PREVIOUS_ORDERED_TO_SIT_TAG, tamable.isOrderedToSit());
            }
            if (tamable.isOrderedToSit()) {
                tamable.setOrderedToSit(false);
            }
        }
        if (mount instanceof Mob mob) {
            mob.getNavigation().stop();
        }
    }

    private static void maintainMountedBehavior(LivingEntity mount) {
        if (mount instanceof TamableAnimal tamable && tamable.isOrderedToSit()) {
            tamable.setOrderedToSit(false);
        }
    }

    private static void restoreMountedBehavior(LivingEntity mount) {
        if (!(mount instanceof TamableAnimal tamable)) {
            return;
        }
        var persistent = mount.getPersistentData();
        if (!persistent.getBoolean(SIT_CAPTURED_TAG)) {
            return;
        }
        boolean previous = persistent.getBoolean(PREVIOUS_ORDERED_TO_SIT_TAG);
        tamable.setOrderedToSit(previous);
        persistent.remove(SIT_CAPTURED_TAG);
        persistent.remove(PREVIOUS_ORDERED_TO_SIT_TAG);
    }

    private static void markGenericRide(LivingEntity mount, UUID riderUuid) {
        if (mount != null && riderUuid != null) {
            mount.getPersistentData().putString(GENERIC_RIDE_RIDER_TAG, riderUuid.toString());
        }
    }

    private static boolean isMarkedGenericRide(LivingEntity mount, UUID riderUuid) {
        if (mount == null || riderUuid == null) {
            return false;
        }
        return riderUuid.toString().equals(mount.getPersistentData().getString(GENERIC_RIDE_RIDER_TAG));
    }

    private static void clearGenericRideMarker(LivingEntity mount) {
        if (mount != null) {
            mount.getPersistentData().remove(GENERIC_RIDE_RIDER_TAG);
        }
    }

    private static List<LivingEntity> activeMountsSnapshot() {
        synchronized (ACTIVE) {
            return new ArrayList<>(ACTIVE.keySet());
        }
    }

    private static StopReason invalidReason(ServerPlayer rider, LivingEntity mount) {
        if (rider == null || !rider.isAlive() || rider.isSpectator()) return StopReason.RIDER_INVALID;
        if (!mount.isAlive() || mount.isRemoved()) return StopReason.MOUNT_INVALID;
        if (rider.getVehicle() != mount) return StopReason.DISMOUNTED;
        if (rider.level() != mount.level()) return StopReason.DIMENSION_CHANGED;
        if (TorporData.exists(mount) && TorporData.of(mount).unconscious()) return StopReason.TORPOR;
        if (FuryService.isBerserk(mount)) return StopReason.FURY;
        if (!mayRide(rider, mount)) return StopReason.PERMISSION_REVOKED;
        return null;
    }

    public enum StopReason {
        MANUAL,
        DISMOUNTED,
        RIDER_INVALID,
        MOUNT_INVALID,
        DIMENSION_CHANGED,
        PERMISSION_REVOKED,
        TORPOR,
        FURY,
        CONFIG_CHANGED,
        ENTITY_REMOVED,
        SERVER_STOPPING
    }
}
