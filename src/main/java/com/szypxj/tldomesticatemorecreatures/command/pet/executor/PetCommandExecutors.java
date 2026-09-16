package com.szypxj.tldomesticatemorecreatures.command.pet.executor;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommand;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandData;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandRuntimeState;
import com.szypxj.tldomesticatemorecreatures.command.pet.capability.PetCapabilityProfile;
import com.szypxj.tldomesticatemorecreatures.command.pet.capability.PetCapabilityResolver;
import com.szypxj.tldomesticatemorecreatures.command.pet.capability.PetCommandCapability;
import com.szypxj.tldomesticatemorecreatures.command.pet.provider.PetCommandCompatibilityApi;
import net.minecraft.world.entity.Mob;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PetCommandExecutors {
    private static final Map<PetCommand, PetCommandExecutor> EXECUTORS = new EnumMap<>(PetCommand.class);

    static {
        EXECUTORS.put(PetCommand.ATTACK, new AttackCommandExecutor());
        EXECUTORS.put(PetCommand.MOVE, new MoveCommandExecutor());
        EXECUTORS.put(PetCommand.RETREAT, new RetreatCommandExecutor());
        EXECUTORS.put(PetCommand.FOLLOW, new FollowCommandExecutor());
        EXECUTORS.put(PetCommand.DEFEND, new DefendCommandExecutor());
        EXECUTORS.put(PetCommand.LAND, new LandCommandExecutor());
    }

    private PetCommandExecutors() {
    }

    public static PetCommandExecutor get(PetCommand command) {
        return EXECUTORS.get(command);
    }

    static boolean startFirst(
            Mob mob,
            PetCommandData data,
            PetCommandRuntimeState runtime,
            List<PetCommandCapability> candidates,
            int startIndex
    ) {
        for (int index = Math.max(0, startIndex); index < candidates.size(); index++) {
            PetCommandCapability candidate = candidates.get(index);
            if (!isExplicitProvider(candidate) && !runtime.nativeStateHooksEvaluated()) {
                enterNativeStates(mob, data, runtime, PetCapabilityResolver.resolve(mob));
                runtime.markNativeStateHooksEvaluated();
            }
            boolean started = false;
            try {
                started = candidate.start(mob, data, runtime);
            } catch (RuntimeException ignored) {
            }
            if (started) {
                runtime.setActiveCapability(candidate);
                runtime.setActiveCandidateIndex(index);
                return true;
            }
            try {
                candidate.cancel(mob, data, runtime);
            } catch (RuntimeException ignored) {
            }
        }
        if (!runtime.nativeStateHooksEvaluated()) {
            enterNativeStates(mob, data, runtime, PetCapabilityResolver.resolve(mob));
            runtime.markNativeStateHooksEvaluated();
        }
        return false;
    }

    static boolean tickActiveOrFallback(
            Mob mob,
            PetCommandData data,
            PetCommandRuntimeState runtime,
            List<PetCommandCapability> candidates
    ) {
        maintainNativeStates(mob, data, runtime);
        PetCommandCapability active = runtime.activeCapability();
        if (active == null) {
            return startFirst(mob, data, runtime, candidates, 0);
        }
        PetCommandCapability.TickResult result;
        try {
            result = active.tick(mob, data, runtime);
        } catch (RuntimeException ignored) {
            result = PetCommandCapability.TickResult.FAILED;
        }
        if (result == PetCommandCapability.TickResult.ACTIVE) {
            return true;
        }
        if (result == PetCommandCapability.TickResult.COMPLETE) {
            return false;
        }
        int nextIndex = runtime.activeCandidateIndex() + 1;
        try {
            active.cancel(mob, data, runtime);
        } catch (RuntimeException ignored) {
        }
        runtime.clearActiveCapability();
        return startFirst(mob, data, runtime, candidates, nextIndex);
    }

    static void cancelActiveOnly(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
        PetCommandCapability active = runtime.activeCapability();
        if (active != null) {
            try {
                active.cancel(mob, data, runtime);
            } catch (RuntimeException ignored) {
            }
        }
        runtime.clearActiveCapability();
        runtime.setExecutionPhase(null);
    }

    static void cancelAndRestore(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
        PetCommandCapability active = runtime.activeCapability();
        if (active != null) {
            try {
                active.cancel(mob, data, runtime);
            } catch (RuntimeException ignored) {
            }
        }
        List<PetCommandCompatibilityApi.NativeStateProvider> providers = PetCommandCompatibilityApi.nativeStateProviders();
        for (int index = providers.size() - 1; index >= 0; index--) {
            PetCommandCompatibilityApi.NativeStateProvider provider = providers.get(index);
            if (!runtime.nativeStateEntered(provider.id())) {
                continue;
            }
            try {
                provider.restore(mob, data.command(), runtime);
            } catch (RuntimeException ignored) {
            }
        }
        runtime.clearExecution();
    }

    private static boolean isExplicitProvider(PetCommandCapability capability) {
        return capability instanceof PetCommandCompatibilityApi.MovementProvider
                || capability instanceof PetCommandCompatibilityApi.FollowProvider
                || capability instanceof PetCommandCompatibilityApi.CombatProvider
                || capability instanceof PetCommandCompatibilityApi.DefenseProvider
                || capability instanceof PetCommandCompatibilityApi.LandingProvider;
    }

    private static void enterNativeStates(
            Mob mob,
            PetCommandData data,
            PetCommandRuntimeState runtime,
            PetCapabilityProfile profile
    ) {
        if (data.command() == PetCommand.FOLLOW) {
            return;
        }
        for (PetCommandCompatibilityApi.NativeStateProvider provider : profile.nativeStates()) {
            if (runtime.nativeStateEntered(provider.id())) {
                continue;
            }
            try {
                if (provider.supports(mob, data.command())
                        && provider.enter(mob, data.command(), runtime)) {
                    runtime.markNativeStateEntered(provider.id());
                }
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static void maintainNativeStates(
            Mob mob,
            PetCommandData data,
            PetCommandRuntimeState runtime
    ) {
        for (PetCommandCompatibilityApi.NativeStateProvider provider : PetCommandCompatibilityApi.nativeStateProviders()) {
            if (!runtime.nativeStateEntered(provider.id()) || runtime.nativeStateSelfManaged(provider.id())) {
                continue;
            }
            boolean maintained = false;
            try {
                maintained = provider.maintain(mob, data.command(), runtime);
            } catch (RuntimeException ignored) {
            }
            if (maintained) {
                continue;
            }
            try {
                provider.restore(mob, data.command(), runtime);
            } catch (RuntimeException ignored) {
            }
            runtime.unmarkNativeStateEntered(provider.id());
        }
    }
}
