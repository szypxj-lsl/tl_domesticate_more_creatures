package com.szypxj.tldomesticatemorecreatures.compat.ers;

/** Optional flight extension for ERS flyable vehicles. */
public interface ErsFlyableRideBridge {
    void tdmc$setFlightVerticalInput(int input);
    int tdmc$getFlightVerticalInput();
    boolean tdmc$isFlying();
}
