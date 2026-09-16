package com.szypxj.tldomesticatemorecreatures.command.pet;

import com.szypxj.tldomesticatemorecreatures.command.pet.capability.PetCommandCapability;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.WeakHashMap;

public final class PetCommandRuntimeState {
    private static final Map<LivingEntity, PetCommandRuntimeState> STATES = new WeakHashMap<>();

    private PetCommandCapability activeCapability;
    private int activeCandidateIndex = -1;
    private Boolean previousOrderedToSit;
    private final Map<String, Integer> previousNativeStates = new HashMap<>();
    private final Set<String> enteredNativeStates = new HashSet<>();
    private final Set<String> selfManagedNativeStates = new HashSet<>();
    private Vec3 landingTarget;
    private Vec3 lastMovementTarget;
    private String executionPhase;
    private boolean nativeStateHooksEvaluated;

    private PetCommandRuntimeState() {
    }

    public static PetCommandRuntimeState of(LivingEntity entity) {
        synchronized (STATES) {
            return STATES.computeIfAbsent(entity, ignored -> new PetCommandRuntimeState());
        }
    }

    public static PetCommandRuntimeState peek(LivingEntity entity) {
        synchronized (STATES) {
            return STATES.get(entity);
        }
    }

    public static void remove(LivingEntity entity) {
        synchronized (STATES) {
            STATES.remove(entity);
        }
    }

    public PetCommandCapability activeCapability() {
        return activeCapability;
    }

    public void setActiveCapability(PetCommandCapability capability) {
        this.activeCapability = capability;
    }

    public int activeCandidateIndex() {
        return activeCandidateIndex;
    }

    public void setActiveCandidateIndex(int index) {
        this.activeCandidateIndex = index;
    }

    public void clearActiveCapability() {
        this.activeCapability = null;
        this.activeCandidateIndex = -1;
    }

    public void rememberOrderedToSit(boolean value) {
        if (previousOrderedToSit == null) {
            previousOrderedToSit = value;
        }
    }

    public Optional<Boolean> previousOrderedToSit() {
        return Optional.ofNullable(previousOrderedToSit);
    }

    public void rememberNativeState(String adapterId, int value) {
        previousNativeStates.putIfAbsent(adapterId, value);
    }

    public OptionalInt previousNativeState(String adapterId) {
        Integer value = previousNativeStates.get(adapterId);
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    public boolean markNativeStateEntered(String providerId) {
        return enteredNativeStates.add(providerId);
    }

    public boolean nativeStateHooksEvaluated() {
        return nativeStateHooksEvaluated;
    }

    public void markNativeStateHooksEvaluated() {
        nativeStateHooksEvaluated = true;
    }

    public boolean nativeStateEntered(String providerId) {
        return enteredNativeStates.contains(providerId);
    }

    public void markNativeStateSelfManaged(String providerId) {
        if (providerId != null) {
            selfManagedNativeStates.add(providerId);
        }
    }

    public boolean nativeStateSelfManaged(String providerId) {
        return selfManagedNativeStates.contains(providerId);
    }

    public void unmarkNativeStateEntered(String providerId) {
        enteredNativeStates.remove(providerId);
        selfManagedNativeStates.remove(providerId);
    }

    public Set<String> enteredNativeStateIds() {
        return Set.copyOf(enteredNativeStates);
    }

    public Optional<Vec3> landingTarget() {
        return Optional.ofNullable(landingTarget);
    }

    public void setLandingTarget(Vec3 landingTarget) {
        this.landingTarget = landingTarget;
    }

    public Optional<Vec3> lastMovementTarget() {
        return Optional.ofNullable(lastMovementTarget);
    }

    public void setLastMovementTarget(Vec3 target) {
        this.lastMovementTarget = target;
    }

    public String executionPhase() {
        return executionPhase;
    }

    public void setExecutionPhase(String executionPhase) {
        this.executionPhase = executionPhase;
    }

    public void clearExecution() {
        activeCapability = null;
        activeCandidateIndex = -1;
        previousOrderedToSit = null;
        previousNativeStates.clear();
        enteredNativeStates.clear();
        selfManagedNativeStates.clear();
        landingTarget = null;
        lastMovementTarget = null;
        executionPhase = null;
        nativeStateHooksEvaluated = false;
    }
}
