package com.szypxj.tldomesticatemorecreatures.riding.capability;

public record RideCapabilityProfile(boolean ground, boolean flight, boolean swim) {
    public static final RideCapabilityProfile GROUND_ONLY = new RideCapabilityProfile(true, false, false);
}
