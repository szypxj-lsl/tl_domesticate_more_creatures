package com.szypxj.tldomesticatemorecreatures.mixin.compat.ers;

import com.szypxj.tldomesticatemorecreatures.api.compat.ErsEliteBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = "cn.aurorian.ers.entity.ErsTamableVehicle", remap = false)
public abstract class ErsTamableVehicleCompatMixin implements ErsEliteBridge {
    @Shadow(remap = false)
    public abstract boolean isElite();

    @Override
    @Unique
    public boolean tdmc$isErsElite() {
        return isElite();
    }
}
