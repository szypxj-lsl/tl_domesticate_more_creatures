package com.szypxj.tldomesticatemorecreatures.client.riding;

import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionInfo;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionState;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionStatus;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ClientRideControlState {
    private static int mountEntityId = -1;
    private static RideCapabilities capabilities = RideCapabilities.NONE;
    private static List<RideActionInfo> actionOrder = List.of();
    private static final Map<RideAction, RideActionInfo> ACTIONS = new EnumMap<>(RideAction.class);
    private static final Map<RideAction, TimedStatus> STATUSES = new EnumMap<>(RideAction.class);

    private ClientRideControlState() {
    }

    public static synchronized void applyProfile(
            int mountId,
            boolean active,
            RideCapabilities nextCapabilities,
            List<RideActionInfo> actions
    ) {
        if (!active) {
            if (mountEntityId == mountId) {
                clear();
            }
            return;
        }
        mountEntityId = mountId;
        capabilities = nextCapabilities == null ? RideCapabilities.NONE : nextCapabilities;
        ACTIONS.clear();
        List<RideActionInfo> ordered = new ArrayList<>();
        if (actions != null) {
            for (RideActionInfo info : actions) {
                if (info == null || !info.action().unifiedControlAction()) continue;
                ACTIONS.put(info.action(), info);
                ordered.add(info);
            }
        }
        actionOrder = List.copyOf(ordered);
        STATUSES.keySet().removeIf(action -> !ACTIONS.containsKey(action));
    }

    public static synchronized void applyStatus(int mountId, RideAction action, RideActionStatus status) {
        if (mountEntityId != mountId || action == null || !ACTIONS.containsKey(action)) {
            return;
        }
        long now = clientGameTime();
        STATUSES.put(action, new TimedStatus(status == null ? RideActionStatus.READY : status, now));
    }

    public static synchronized boolean activeForCurrentMount(Minecraft minecraft) {
        if (minecraft == null || minecraft.player == null || mountEntityId < 0) {
            return false;
        }
        Entity vehicle = minecraft.player.getVehicle();
        return vehicle != null && vehicle.getId() == mountEntityId;
    }

    public static synchronized int mountEntityId() {
        return mountEntityId;
    }

    public static synchronized RideCapabilities capabilities() {
        return capabilities;
    }

    public static synchronized boolean supports(RideAction action) {
        return action != null && ACTIONS.containsKey(action) && capabilities.supports(action);
    }

    public static synchronized RideActionInfo info(RideAction action) {
        return ACTIONS.get(action);
    }

    public static synchronized List<RideActionInfo> actions() {
        return actionOrder;
    }

    public static synchronized RideActionStatus status(RideAction action) {
        TimedStatus timed = STATUSES.get(action);
        if (timed == null) {
            return RideActionStatus.READY;
        }
        RideActionStatus status = timed.status();
        if (status.state() != RideActionState.COOLDOWN || status.remainingTicks() <= 0) {
            return status;
        }
        long elapsed = Math.max(0L, clientGameTime() - timed.receivedGameTime());
        int remaining = (int) Math.max(0L, status.remainingTicks() - elapsed);
        return remaining <= 0
                ? RideActionStatus.READY
                : RideActionStatus.cooldown(remaining, status.totalTicks());
    }

    public static synchronized void clearIfMountMismatch(Minecraft minecraft) {
        if (mountEntityId < 0) return;
        if (minecraft == null || minecraft.player == null) {
            clear();
            return;
        }
        Entity vehicle = minecraft.player.getVehicle();
        if (vehicle == null || vehicle.getId() != mountEntityId) {
            clear();
        }
    }

    public static synchronized void clear() {
        mountEntityId = -1;
        capabilities = RideCapabilities.NONE;
        actionOrder = List.of();
        ACTIONS.clear();
        STATUSES.clear();
    }

    private static long clientGameTime() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }

    private record TimedStatus(RideActionStatus status, long receivedGameTime) {
    }
}
