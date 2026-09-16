package com.szypxj.tldomesticatemorecreatures.command.pet.capability;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommand;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandData;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandRuntimeState;
import com.szypxj.tldomesticatemorecreatures.command.pet.provider.PetCommandCompatibilityApi;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ReflectiveNativeCapabilities {
    private static final ConcurrentHashMap<Class<?>, PetCommandCapability> MOVEMENT = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, PetCommandCompatibilityApi.FlightProvider> FLIGHT = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, PetCommandCapability> LANDING = new ConcurrentHashMap<>();
    private static final Set<Class<?>> NO_MOVEMENT = ConcurrentHashMap.newKeySet();
    private static final Set<Class<?>> NO_FLIGHT = ConcurrentHashMap.newKeySet();
    private static final Set<Class<?>> NO_LANDING = ConcurrentHashMap.newKeySet();

    private ReflectiveNativeCapabilities() {
    }

    public static Optional<PetCommandCapability> nativeMovement(Mob mob) {
        if (mob == null) {
            return Optional.empty();
        }
        Class<?> type = mob.getClass();
        if (NO_MOVEMENT.contains(type)) {
            return Optional.empty();
        }
        PetCommandCapability cached = MOVEMENT.get(type);
        if (cached != null) {
            return Optional.of(cached);
        }
        PetCommandCapability discovered = discoverMovement(mob);
        if (discovered == null) {
            NO_MOVEMENT.add(type);
            return Optional.empty();
        }
        MOVEMENT.put(type, discovered);
        return Optional.of(discovered);
    }

    public static Optional<PetCommandCompatibilityApi.FlightProvider> nativeFlight(Mob mob) {
        if (mob == null) {
            return Optional.empty();
        }
        Class<?> type = mob.getClass();
        if (NO_FLIGHT.contains(type)) {
            return Optional.empty();
        }
        PetCommandCompatibilityApi.FlightProvider cached = FLIGHT.get(type);
        if (cached != null) {
            return Optional.of(cached);
        }
        PetCommandCompatibilityApi.FlightProvider discovered = discoverFlight(type);
        if (discovered == null) {
            NO_FLIGHT.add(type);
            return Optional.empty();
        }
        FLIGHT.put(type, discovered);
        return Optional.of(discovered);
    }

    public static Optional<PetCommandCapability> nativeLanding(Mob mob) {
        if (mob == null) {
            return Optional.empty();
        }
        Class<?> type = mob.getClass();
        if (NO_LANDING.contains(type)) {
            return Optional.empty();
        }
        PetCommandCapability cached = LANDING.get(type);
        if (cached != null) {
            return Optional.of(cached);
        }
        PetCommandCapability discovered = discoverLanding(type);
        if (discovered == null) {
            NO_LANDING.add(type);
            return Optional.empty();
        }
        LANDING.put(type, discovered);
        return Optional.of(discovered);
    }

    public static void disableMovement(Class<?> type) {
        if (type != null) {
            MOVEMENT.remove(type);
            NO_MOVEMENT.add(type);
            PetCapabilityResolver.invalidate(type);
        }
    }

    public static void disableFlight(Class<?> type) {
        if (type != null) {
            FLIGHT.remove(type);
            NO_FLIGHT.add(type);
            PetCapabilityResolver.invalidate(type);
        }
    }

    public static void disableLanding(Class<?> type) {
        if (type != null) {
            LANDING.remove(type);
            NO_LANDING.add(type);
            PetCapabilityResolver.invalidate(type);
        }
    }

    private static PetCommandCapability discoverMovement(Mob mob) {
        Class<?> type = mob.getClass();
        PetCommandCapability entityMovement = discoverEntityMovement(type);
        if (entityMovement != null) {
            return entityMovement;
        }
        return discoverNavigationMovement(type, mob.getNavigation().getClass());
    }

    private static PetCommandCapability discoverEntityMovement(Class<?> type) {
        try {
            Method getAiMovement = type.getMethod("getAIMovement");
            Class<?> controller = getAiMovement.getReturnType();
            Method setWaypoint = controller.getMethod("setWaypoint", Vec3.class, double.class);
            Method stop = controller.getMethod("stop");
            Class<?> waypointReturn = setWaypoint.getReturnType();
            if ((waypointReturn != void.class && waypointReturn != boolean.class)
                    || stop.getReturnType() != void.class) {
                return null;
            }
            return new NativeMovementCapability(type, getAiMovement, setWaypoint, stop);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static PetCommandCapability discoverNavigationMovement(Class<?> ownerType, Class<?> navigationType) {
        try {
            Method tryMoveToBlockPos = navigationType.getMethod("tryMoveToBlockPos", BlockPos.class, double.class);
            if (tryMoveToBlockPos.getReturnType() != boolean.class) {
                return null;
            }
            return new NavigationMovementCapability(ownerType, navigationType, tryMoveToBlockPos);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static PetCommandCompatibilityApi.FlightProvider discoverFlight(Class<?> type) {
        List<Method> airborneMethods = new ArrayList<>();
        for (String name : List.of("isFlying", "isTakeoff", "isHovering")) {
            Method method = booleanMethod(type, name);
            if (method != null) {
                airborneMethods.add(method);
            }
        }
        if (airborneMethods.isEmpty()) {
            return null;
        }
        Method isLanding = booleanMethod(type, "isLanding");
        if (isLanding != null) {
            airborneMethods.add(isLanding);
        }
        return new ReflectiveFlightProvider(type, airborneMethods);
    }

    private static PetCommandCapability discoverLanding(Class<?> type) {
        PetCommandCapability groundTransition = discoverGroundTransitionLanding(type);
        if (groundTransition != null) {
            return groundTransition;
        }
        try {
            Method beginAiLanding = type.getMethod("beginAiLanding");
            Method isLanding = type.getMethod("isLanding");
            PetCommandCompatibilityApi.FlightProvider flight = discoverFlight(type);
            if ((beginAiLanding.getReturnType() != void.class && beginAiLanding.getReturnType() != boolean.class)
                    || isLanding.getReturnType() != boolean.class
                    || flight == null) {
                return null;
            }
            return new NativeLandingCapability(type, beginAiLanding, isLanding, flight);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static PetCommandCapability discoverGroundTransitionLanding(Class<?> type) {
        try {
            Method getAiMovement = type.getMethod("getAIMovement");
            Class<?> controller = getAiMovement.getReturnType();
            Method groundTransition = groundTransitionMethod(controller);
            Method stop = controller.getMethod("stop");
            PetCommandCompatibilityApi.FlightProvider flight = discoverFlight(type);
            if (groundTransition == null
                    || stop.getReturnType() != void.class
                    || flight == null) {
                return null;
            }
            return new GroundTransitionLandingCapability(
                    type,
                    getAiMovement,
                    groundTransition,
                    stop,
                    flight
            );
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Method groundTransitionMethod(Class<?> controller) {
        for (String name : List.of("requestGroundTransition", "setGroundTransitionWaypoint")) {
            try {
                Method method = controller.getMethod(name, Vec3.class, double.class);
                Class<?> returnType = method.getReturnType();
                if (returnType == void.class || returnType == boolean.class) {
                    return method;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static Method booleanMethod(Class<?> type, String name) {
        try {
            Method method = type.getMethod(name);
            return method.getReturnType() == boolean.class ? method : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static final class ReflectiveFlightProvider implements PetCommandCompatibilityApi.FlightProvider {
        private final Class<?> ownerType;
        private final List<Method> airborneMethods;

        private ReflectiveFlightProvider(Class<?> ownerType, List<Method> airborneMethods) {
            this.ownerType = ownerType;
            this.airborneMethods = List.copyOf(airborneMethods);
        }

        @Override
        public boolean supports(Mob mob) {
            return mob != null && ownerType.isInstance(mob);
        }

        @Override
        public boolean isAirborne(Mob mob) {
            if (!supports(mob)) {
                return false;
            }
            try {
                for (Method method : airborneMethods) {
                    if (Boolean.TRUE.equals(method.invoke(mob))) {
                        return true;
                    }
                }
                return false;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                disableFlight(ownerType);
                throw new IllegalStateException("Native flight-state probe failed for " + ownerType.getName(), exception);
            }
        }
    }

    private static final class NativeMovementCapability implements PetCommandCapability {
        private final Class<?> ownerType;
        private final Method getAiMovement;
        private final Method setWaypoint;
        private final Method stop;

        private NativeMovementCapability(Class<?> ownerType, Method getAiMovement, Method setWaypoint, Method stop) {
            this.ownerType = ownerType;
            this.getAiMovement = getAiMovement;
            this.setWaypoint = setWaypoint;
            this.stop = stop;
        }

        @Override
        public String id() {
            return "native:waypoint";
        }

        @Override
        public boolean start(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            return issue(mob, data.position(), 1.0D, runtime);
        }

        @Override
        public TickResult tick(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (data.command() == PetCommand.MOVE && mob.distanceToSqr(data.position()) <= 4.0D) {
                stop(mob);
                return TickResult.COMPLETE;
            }
            Vec3 last = runtime.lastMovementTarget().orElse(null);
            Vec3 current = data.position();
            if (last == null || last.distanceToSqr(current) > 1.0D) {
                return issue(mob, current, 1.0D, runtime) ? TickResult.ACTIVE : TickResult.FAILED;
            }
            return TickResult.ACTIVE;
        }

        @Override
        public void cancel(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            stop(mob);
        }

        private boolean issue(Mob mob, Vec3 target, double speed, PetCommandRuntimeState runtime) {
            if (target == null) {
                return false;
            }
            try {
                Object movement = getAiMovement.invoke(mob);
                if (movement == null) {
                    return false;
                }
                Object result = setWaypoint.invoke(movement, target, speed);
                if (result instanceof Boolean value && !value) {
                    return false;
                }
                runtime.setLastMovementTarget(target);
                return true;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                disableMovement(ownerType);
                return false;
            }
        }

        private void stop(Mob mob) {
            try {
                Object movement = getAiMovement.invoke(mob);
                if (movement != null) {
                    stop.invoke(movement);
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                disableMovement(ownerType);
            }
        }
    }

    private static final class NavigationMovementCapability implements PetCommandCapability {
        private final Class<?> ownerType;
        private final Class<?> navigationType;
        private final Method tryMoveToBlockPos;

        private NavigationMovementCapability(Class<?> ownerType, Class<?> navigationType, Method tryMoveToBlockPos) {
            this.ownerType = ownerType;
            this.navigationType = navigationType;
            this.tryMoveToBlockPos = tryMoveToBlockPos;
        }

        @Override
        public String id() {
            return "native:navigation_move";
        }

        @Override
        public boolean start(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            return issue(mob, data.position(), 1.0D, runtime);
        }

        @Override
        public TickResult tick(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            Vec3 target = data.position();
            if (data.command() == PetCommand.MOVE && mob.distanceToSqr(target) <= 4.0D) {
                mob.getNavigation().stop();
                return TickResult.COMPLETE;
            }
            Vec3 last = runtime.lastMovementTarget().orElse(null);
            if (last == null || last.distanceToSqr(target) > 1.0D || mob.getNavigation().isDone()) {
                return issue(mob, target, 1.0D, runtime) ? TickResult.ACTIVE : TickResult.FAILED;
            }
            return TickResult.ACTIVE;
        }

        @Override
        public void cancel(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            mob.getNavigation().stop();
        }

        private boolean issue(Mob mob, Vec3 target, double speed, PetCommandRuntimeState runtime) {
            if (target == null || !ownerType.isInstance(mob) || !navigationType.isInstance(mob.getNavigation())) {
                return false;
            }
            try {
                Object accepted = tryMoveToBlockPos.invoke(mob.getNavigation(), BlockPos.containing(target), speed);
                if (!Boolean.TRUE.equals(accepted)) {
                    return false;
                }
                runtime.setLastMovementTarget(target);
                return true;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                disableMovement(ownerType);
                return false;
            }
        }
    }

    private static final class GroundTransitionLandingCapability implements PetCommandCapability {
        private final Class<?> ownerType;
        private final Method getAiMovement;
        private final Method groundTransition;
        private final Method stop;
        private final PetCommandCompatibilityApi.FlightProvider flight;

        private GroundTransitionLandingCapability(
                Class<?> ownerType,
                Method getAiMovement,
                Method groundTransition,
                Method stop,
                PetCommandCompatibilityApi.FlightProvider flight
        ) {
            this.ownerType = ownerType;
            this.getAiMovement = getAiMovement;
            this.groundTransition = groundTransition;
            this.stop = stop;
            this.flight = flight;
        }

        @Override
        public String id() {
            return "native:ground_transition_landing";
        }

        @Override
        public boolean start(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (mob.onGround() || mob.isInWaterOrBubble()) {
                return true;
            }
            if (!flight.isAirborne(mob)) {
                return false;
            }
            Vec3 landing = StandardPetCapabilities.findSafeLandingPosition(mob);
            if (landing == null) {
                return false;
            }
            runtime.setLandingTarget(landing);
            data.updatePosition(landing);
            return issueGroundTransition(mob, landing);
        }

        @Override
        public TickResult tick(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (mob.onGround() || mob.isInWaterOrBubble()) {
                return TickResult.COMPLETE;
            }
            try {
                return flight.isAirborne(mob) ? TickResult.ACTIVE : TickResult.COMPLETE;
            } catch (RuntimeException exception) {
                disableLanding(ownerType);
                return TickResult.FAILED;
            }
        }

        @Override
        public void cancel(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            try {
                Object movement = getAiMovement.invoke(mob);
                if (movement != null) {
                    stop.invoke(movement);
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                disableLanding(ownerType);
            }
            runtime.setLandingTarget(null);
        }

        private boolean issueGroundTransition(Mob mob, Vec3 landing) {
            try {
                Object movement = getAiMovement.invoke(mob);
                if (movement == null) {
                    return false;
                }
                Object result = groundTransition.invoke(movement, landing, 1.0D);
                return !(result instanceof Boolean value) || value;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                disableLanding(ownerType);
                return false;
            }
        }
    }

    private static final class NativeLandingCapability implements PetCommandCapability {
        private final Class<?> ownerType;
        private final Method beginAiLanding;
        private final Method isLanding;
        private final PetCommandCompatibilityApi.FlightProvider flight;

        private NativeLandingCapability(
                Class<?> ownerType,
                Method beginAiLanding,
                Method isLanding,
                PetCommandCompatibilityApi.FlightProvider flight
        ) {
            this.ownerType = ownerType;
            this.beginAiLanding = beginAiLanding;
            this.isLanding = isLanding;
            this.flight = flight;
        }

        @Override
        public String id() {
            return "native:landing";
        }

        @Override
        public boolean start(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (mob.onGround() || mob.isInWaterOrBubble()) {
                return true;
            }
            return invokeLanding(mob);
        }

        @Override
        public TickResult tick(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (mob.onGround() || mob.isInWaterOrBubble()) {
                return TickResult.COMPLETE;
            }
            try {
                boolean landing = Boolean.TRUE.equals(isLanding.invoke(mob));
                boolean airborne = flight.isAirborne(mob);
                if (landing) {
                    return TickResult.ACTIVE;
                }
                if (airborne) {
                    return invokeLanding(mob) ? TickResult.ACTIVE : TickResult.FAILED;
                }
                return TickResult.ACTIVE;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                disableLanding(ownerType);
                return TickResult.FAILED;
            }
        }

        @Override
        public void cancel(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
        }

        private boolean invokeLanding(Mob mob) {
            try {
                Object result = beginAiLanding.invoke(mob);
                return !(result instanceof Boolean value) || value;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                disableLanding(ownerType);
                return false;
            }
        }
    }
}
