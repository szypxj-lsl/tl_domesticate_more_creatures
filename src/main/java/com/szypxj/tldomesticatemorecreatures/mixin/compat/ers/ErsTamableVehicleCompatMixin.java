package com.szypxj.tldomesticatemorecreatures.mixin.compat.ers;

import com.szypxj.tldomesticatemorecreatures.api.compat.ErsEliteBridge;
import com.szypxj.tldomesticatemorecreatures.compat.ers.ErsRideBridge;
import com.szypxj.tldomesticatemorecreatures.riding.NativeRideMarker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = "cn.aurorian.ers.entity.ErsTamableVehicle", remap = false)
public abstract class ErsTamableVehicleCompatMixin implements ErsEliteBridge, ErsRideBridge, NativeRideMarker {
    @Shadow(remap = false) public abstract boolean isElite();
    @Shadow(remap = false) public abstract void executeDefaultAttackType();
    @Shadow(remap = false) public abstract void executeSpecialAttackType();
    @Shadow(remap = false) public abstract void executeJudgementAttackType();
    @Shadow(remap = false) public abstract void executeTurnAttackType();
    @Shadow(remap = false) public abstract void executeJumpAttackType();
    @Shadow(remap = false) public abstract void onDiveKeyUpdate(boolean active);
    @Shadow(remap = false) public abstract boolean isDiving();

    @Override @Unique
    public boolean tdmc$isErsElite() {
        return isElite();
    }

    @Override @Unique public void tdmc$defaultAttack() { executeDefaultAttackType(); }
    @Override @Unique public void tdmc$specialAttack() { executeSpecialAttackType(); }
    @Override @Unique public void tdmc$judgementAttack() { executeJudgementAttackType(); }
    @Override @Unique public void tdmc$turnAttack() { executeTurnAttackType(); }
    @Override @Unique public void tdmc$jumpAttack() { executeJumpAttackType(); }
    @Override @Unique public void tdmc$setDiving(boolean active) { onDiveKeyUpdate(active); }
    @Override @Unique public boolean tdmc$isDiving() { return isDiving(); }
}
