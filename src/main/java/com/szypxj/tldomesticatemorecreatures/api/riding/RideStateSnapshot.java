package com.szypxj.tldomesticatemorecreatures.api.riding;

public record RideStateSnapshot(boolean ridden, boolean flying, boolean accelerating) {
    public static final RideStateSnapshot NONE = new RideStateSnapshot(false, false, false);
}
