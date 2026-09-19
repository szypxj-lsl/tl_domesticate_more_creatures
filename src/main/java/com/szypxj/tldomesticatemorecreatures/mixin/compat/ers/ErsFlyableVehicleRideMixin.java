package com.szypxj.tldomesticatemorecreatures.mixin.compat.ers;

import com.szypxj.tldomesticatemorecreatures.compat.ers.ErsFlyableRideBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = "cn.aurorian.ers.entity.ErsFlyableVehicle", remap = false)
public abstract class ErsFlyableVehicleRideMixin implements ErsFlyableRideBridge {
    @Shadow(remap = false) public abstract void onFlightKeyUpdate(int input);
    @Shadow(remap = false) public abstract int getFlightVerticalInput();
    @Shadow(remap = false) public abstract boolean isFlying();

    @Override @Unique
    public void tdmc$setFlightVerticalInput(int input) {
        onFlightKeyUpdate(input);
    }

    @Override @Unique
    public int tdmc$getFlightVerticalInput() {
        return getFlightVerticalInput();
    }

    @Override @Unique
    public boolean tdmc$isFlying() {
        return isFlying();
    }
}
