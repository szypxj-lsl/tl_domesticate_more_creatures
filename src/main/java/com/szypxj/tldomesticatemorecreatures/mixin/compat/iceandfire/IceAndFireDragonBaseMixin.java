package com.szypxj.tldomesticatemorecreatures.mixin.compat.iceandfire;

import com.szypxj.tldomesticatemorecreatures.compat.iceandfire.IceAndFireRideBridge;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticEntityCarrier;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchGeneticPayload;
import com.szypxj.tldomesticatemorecreatures.game.genetics.HatchGeneticsService;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexthe666.iceandfire.entity.EntityDragonBase", remap = false)
public abstract class IceAndFireDragonBaseMixin implements IceAndFireRideBridge {
    @Shadow(remap = false) public abstract void up(boolean active);
    @Shadow(remap = false) public abstract void down(boolean active);
    @Shadow(remap = false) public abstract void attack(boolean active);
    @Shadow(remap = false) public abstract void strike(boolean active);
    @Shadow(remap = false) public abstract void roar();
    @Shadow(remap = false) public abstract boolean isGoingUp();
    @Shadow(remap = false) public abstract boolean isGoingDown();
    @Shadow(remap = false) public abstract boolean isAttacking();
    @Shadow(remap = false) public abstract boolean isStriking();
    @Shadow(remap = false) public abstract boolean isBreathingFire();
    @Shadow(remap = false) public abstract int getDragonStage();
    @Shadow(remap = false) public abstract byte getControlState();
    @Shadow(remap = false) public abstract boolean isRidingPlayer(Player player);

    @Override
    public void tdmc$setGoingUp(boolean active) { up(active); }

    @Override
    public void tdmc$setGoingDown(boolean active) { down(active); }

    @Override
    public void tdmc$setAttacking(boolean active) { attack(active); }

    @Override
    public void tdmc$setStriking(boolean active) { strike(active); }

    @Override
    public void tdmc$roar() { roar(); }

    @Override
    public boolean tdmc$isGoingUp() { return isGoingUp(); }

    @Override
    public boolean tdmc$isGoingDown() { return isGoingDown(); }

    @Override
    public boolean tdmc$isAttacking() { return isAttacking(); }

    @Override
    public boolean tdmc$isStriking() { return isStriking(); }

    @Override
    public boolean tdmc$isBreathingFire() { return isBreathingFire(); }

    @Override
    public int tdmc$getDragonStage() { return getDragonStage(); }

    @Override
    public byte tdmc$getControlState() { return getControlState(); }

    @Override
    public boolean tdmc$isRidingPlayer(Player player) { return isRidingPlayer(player); }

    @Inject(method = "createEgg", at = @At("RETURN"), remap = false, require = 0)
    private void tdmc$captureEggGenetics(@Coerce Object mateObject, CallbackInfoReturnable<Object> cir) {
        Object selfObject = this;
        Object eggObject = cir.getReturnValue();
        if (!(selfObject instanceof LivingEntity parentA)
                || !(mateObject instanceof LivingEntity parentB)
                || !(eggObject instanceof Entity egg)
                || parentA.level().isClientSide) {
            return;
        }
        HatchGeneticPayload payload = HatchGeneticsService.createPayload(parentA, parentB, parentA.getRandom().nextLong());
        if (payload != null) {
            GeneticEntityCarrier.set(egg, payload);
        }
    }
}
