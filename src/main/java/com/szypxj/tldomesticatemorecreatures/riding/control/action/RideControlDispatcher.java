package com.szypxj.tldomesticatemorecreatures.riding.control.action;

import com.mojang.logging.LogUtils;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionApi;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionContext;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionInfo;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionResult;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionStatus;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideCapabilities;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideControlApi;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideControlProvider;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideInputPhase;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.riding.RideService;
import com.szypxj.tldomesticatemorecreatures.talent.active.ActiveTalentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative router for unified mounted actions. */
public final class RideControlDispatcher {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, Integer> LAST_SEQUENCE = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LEGACY_LAST_SEQUENCE = new ConcurrentHashMap<>();
    private static final RideAction[] PROFILE_ACTION_ORDER = {
            RideAction.PRIMARY_ATTACK,
            RideAction.SECONDARY_ATTACK,
            RideAction.ROAR,
            RideAction.SKILL_1,
            RideAction.SKILL_2,
            RideAction.SKILL_3,
            RideAction.MOVEMENT_SPECIAL,
            RideAction.UTILITY
    };

    private RideControlDispatcher() {
    }

    public static void acceptAction(ServerPlayer rider, int mountEntityId, RideAction action, RideInputPhase phase, int sequence) {
        if (!hasMatchingMountedEntity(rider, mountEntityId) || action == null || phase == null || !action.unifiedControlAction()) {
            return;
        }
        if (!acceptSequence(LAST_SEQUENCE, rider, sequence)) {
            return;
        }
        acceptActionInternal(rider, mountEntityId, action, phase, sequence);
    }

    /** Compatibility bridge for the old attack packet. */
    public static void acceptLegacyPrimaryAttack(ServerPlayer rider, int mountEntityId, int sequence) {
        if (!hasMatchingMountedEntity(rider, mountEntityId)) {
            return;
        }
        if (!acceptSequence(LEGACY_LAST_SEQUENCE, rider, sequence)) {
            return;
        }
        acceptActionInternal(rider, mountEntityId, RideAction.PRIMARY_ATTACK, RideInputPhase.PRESS, sequence);
    }

    private static void acceptActionInternal(ServerPlayer rider, int mountEntityId, RideAction action, RideInputPhase phase, int sequence) {
        if (rider == null || action == null || phase == null || !action.unifiedControlAction()) {
            return;
        }
        if (!(rider.getVehicle() instanceof LivingEntity mount) || mount.getId() != mountEntityId) {
            return;
        }
        if (!RideService.mayRide(rider, mount)) {
            sendState(rider, mount, action, RideActionStatus.BLOCKED);
            return;
        }
        if (ActiveTalentService.blocksRideInput(mount)
                || !RideActionApi.allows(rider, mount, action.legacyGuardAction())) {
            sendState(rider, mount, action, RideActionStatus.BLOCKED);
            return;
        }

        List<RideControlProvider> candidates = candidateProviders(rider, mount, action);
        if (candidates.isEmpty()) {
            sendState(rider, mount, action, RideActionStatus.UNSUPPORTED);
            return;
        }

        RideActionContext actionContext = new RideActionContext(rider, mount, phase, sequence);
        for (RideControlProvider provider : candidates) {
            RideActionInfo info = safeInfo(provider, mount, action);
            if (phase == RideInputPhase.HOLD && info != null && !info.holdable()) {
                continue;
            }
            try {
                RideActionResult result = provider.execute(actionContext, action);
                if (result == null || result == RideActionResult.PASS) {
                    continue;
                }
                sendState(rider, mount, action, safeStatus(provider, rider, mount, action));
                return;
            } catch (RuntimeException exception) {
                providerFailed(provider, exception);
            }
        }
        sendState(rider, mount, action, RideActionStatus.UNSUPPORTED);
    }

    public static void syncProfile(ServerPlayer rider, LivingEntity mount, boolean active) {
        if (rider == null || mount == null) {
            return;
        }
        if (!active) {
            NetworkHandler.sendRideControlProfile(rider, mount.getId(), false, RideCapabilities.NONE, List.of());
            return;
        }

        List<RideControlProvider> providers = profileProviders(rider, mount);
        if (providers.isEmpty()) {
            NetworkHandler.sendRideControlProfile(rider, mount.getId(), false, RideCapabilities.NONE, List.of());
            return;
        }

        boolean ground = false;
        boolean flight = false;
        boolean swim = false;
        boolean jump = false;
        Map<RideAction, RideActionInfo> infos = new EnumMap<>(RideAction.class);
        for (RideControlProvider provider : providers) {
            RideCapabilities capabilities = safeCapabilities(provider, mount);
            if (capabilities == null) {
                continue;
            }
            ground |= capabilities.groundMovement();
            flight |= capabilities.flightMovement();
            swim |= capabilities.swimMovement();
            jump |= capabilities.jump();
            for (RideAction action : PROFILE_ACTION_ORDER) {
                if (!capabilities.supports(action) || infos.containsKey(action)) {
                    continue;
                }
                RideActionInfo info = safeInfo(provider, mount, action);
                if (info != null) {
                    infos.put(action, info);
                }
            }
        }

        List<RideActionInfo> ordered = new ArrayList<>();
        for (RideAction action : PROFILE_ACTION_ORDER) {
            RideActionInfo info = infos.get(action);
            if (info != null) {
                ordered.add(info);
            }
        }
        if (ordered.isEmpty()) {
            NetworkHandler.sendRideControlProfile(rider, mount.getId(), false, RideCapabilities.NONE, List.of());
            return;
        }
        RideCapabilities merged = new RideCapabilities(ground, flight, swim, jump, infos.keySet());
        NetworkHandler.sendRideControlProfile(rider, mount.getId(), true, merged, ordered);
        for (RideActionInfo info : ordered) {
            RideControlProvider provider = firstProviderForAction(providers, mount, info.action());
            if (provider != null) {
                sendState(rider, mount, info.action(), safeStatus(provider, rider, mount, info.action()));
            }
        }
    }

    public static void clearPlayer(ServerPlayer rider) {
        if (rider == null) return;
        LAST_SEQUENCE.remove(rider.getUUID());
        LEGACY_LAST_SEQUENCE.remove(rider.getUUID());
    }

    public static void clearAll() {
        LAST_SEQUENCE.clear();
        LEGACY_LAST_SEQUENCE.clear();
    }

    private static List<RideControlProvider> profileProviders(ServerPlayer rider, LivingEntity mount) {
        List<RideControlProvider> result = new ArrayList<>(RideControlApi.matchingProviders(mount));
        if (RideService.isGenericRider(rider) && BasicRideControlProvider.INSTANCE.supports(mount)) {
            result.add(BasicRideControlProvider.INSTANCE);
        }
        return List.copyOf(result);
    }

    private static List<RideControlProvider> candidateProviders(ServerPlayer rider, LivingEntity mount, RideAction action) {
        List<RideControlProvider> result = new ArrayList<>();
        for (RideControlProvider provider : RideControlApi.matchingProviders(mount)) {
            RideCapabilities capabilities = safeCapabilities(provider, mount);
            if (capabilities != null && capabilities.supports(action)) {
                result.add(provider);
            }
        }
        if (RideService.isGenericRider(rider) && BasicRideControlProvider.INSTANCE.supports(mount)) {
            RideCapabilities capabilities = BasicRideControlProvider.INSTANCE.capabilities(mount);
            if (capabilities.supports(action)) {
                result.add(BasicRideControlProvider.INSTANCE);
            }
        }
        return List.copyOf(result);
    }

    private static RideControlProvider firstProviderForAction(List<RideControlProvider> providers, LivingEntity mount, RideAction action) {
        for (RideControlProvider provider : providers) {
            RideCapabilities capabilities = safeCapabilities(provider, mount);
            if (capabilities != null && capabilities.supports(action)) {
                return provider;
            }
        }
        return null;
    }

    private static RideCapabilities safeCapabilities(RideControlProvider provider, LivingEntity mount) {
        try {
            RideCapabilities capabilities = provider.capabilities(mount);
            return capabilities == null ? RideCapabilities.NONE : capabilities;
        } catch (RuntimeException exception) {
            providerFailed(provider, exception);
            return null;
        }
    }

    private static RideActionInfo safeInfo(RideControlProvider provider, LivingEntity mount, RideAction action) {
        try {
            return provider.actionInfo(mount, action);
        } catch (RuntimeException exception) {
            providerFailed(provider, exception);
            return null;
        }
    }

    private static RideActionStatus safeStatus(RideControlProvider provider, ServerPlayer rider, LivingEntity mount, RideAction action) {
        try {
            RideActionStatus status = provider.actionStatus(rider, mount, action);
            return status == null ? RideActionStatus.READY : status;
        } catch (RuntimeException exception) {
            providerFailed(provider, exception);
            return RideActionStatus.BLOCKED;
        }
    }

    private static void providerFailed(RideControlProvider provider, RuntimeException exception) {
        if (provider != BasicRideControlProvider.INSTANCE) {
            RideControlApi.disableProvider(provider);
        }
        LOGGER.warn("Ride control provider {} failed while handling a rider action.", provider.getClass().getName(), exception);
    }

    private static void sendState(ServerPlayer rider, LivingEntity mount, RideAction action, RideActionStatus status) {
        NetworkHandler.sendRideActionState(rider, mount.getId(), action, status);
    }

    private static boolean hasMatchingMountedEntity(ServerPlayer rider, int mountEntityId) {
        return rider != null
                && rider.getVehicle() instanceof LivingEntity mount
                && mount.getId() == mountEntityId;
    }

    private static boolean acceptSequence(Map<UUID, Integer> sequences, ServerPlayer rider, int sequence) {
        if (rider == null || sequence < 0) {
            return false;
        }
        UUID uuid = rider.getUUID();
        int previous = sequences.getOrDefault(uuid, Integer.MIN_VALUE);
        if (sequence <= previous && !(previous == Integer.MAX_VALUE && sequence == 1)) {
            return false;
        }
        sequences.put(uuid, sequence);
        return true;
    }
}
