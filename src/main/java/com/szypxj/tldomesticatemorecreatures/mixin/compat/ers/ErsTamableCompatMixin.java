package com.szypxj.tldomesticatemorecreatures.mixin.compat.ers;

import com.szypxj.tldomesticatemorecreatures.api.compat.ErsTamableBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = "cn.aurorian.ers.entity.ErsTamable", remap = false)
public abstract class ErsTamableCompatMixin implements ErsTamableBridge {
    @Shadow(remap = false)
    public abstract float getHunger();

    @Shadow(remap = false)
    public abstract void setHunger(float value);

    @Shadow(remap = false)
    public abstract boolean doHunger();

    @Override
    @Unique
    public float tdmc$getErsHunger() {
        return getHunger();
    }

    @Override
    @Unique
    public void tdmc$setErsHunger(float value) {
        setHunger(value);
    }

    @Override
    @Unique
    public boolean tdmc$usesErsHunger() {
        return doHunger();
    }
}
