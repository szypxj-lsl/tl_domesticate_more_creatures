package com.szypxj.tldomesticatemorecreatures.api.riding;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public record RideCapabilities(
        boolean groundMovement,
        boolean flightMovement,
        boolean swimMovement,
        boolean jump,
        Set<RideAction> actions
) {
    public static final RideCapabilities NONE = new RideCapabilities(false, false, false, false, Set.of());

    public RideCapabilities {
        if (actions == null || actions.isEmpty()) {
            actions = Set.of();
        } else {
            actions = Collections.unmodifiableSet(EnumSet.copyOf(actions));
        }
    }

    public boolean supports(RideAction action) {
        return action != null && actions.contains(action);
    }
}
