package com.szypxj.tldomesticatemorecreatures.riding;

public record RideInputState(
        float forward,
        float strafe,
        boolean jump,
        boolean descend,
        int sequence
) {
    public static final RideInputState ZERO = new RideInputState(0.0F, 0.0F, false, false, 0);
}
