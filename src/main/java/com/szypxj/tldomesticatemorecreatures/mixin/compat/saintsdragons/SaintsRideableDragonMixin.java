package com.szypxj.tldomesticatemorecreatures.mixin.compat.saintsdragons;

import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionApi;
import com.szypxj.tldomesticatemorecreatures.riding.NativeRideMarker;
import com.szypxj.tldomesticatemorecreatures.riding.NativeRideStateMarker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Pseudo
@Mixin(targets = "com.leon.saintsdragons.server.entity.base.RideableDragonBase", remap = false)
public abstract class SaintsRideableDragonMixin implements NativeRideMarker, NativeRideStateMarker {
    @Unique
    private static final ConcurrentHashMap<Class<?>, Optional<Method>> tdmc$forceEndAbilityMethods = new ConcurrentHashMap<>();

    @Unique
    private boolean tdmc$abilityBlocked;

    @Shadow public abstract boolean isFlying();
    @Shadow public abstract boolean isAccelerating();
    @Shadow public abstract void setAccelerating(boolean accelerating);
    @Shadow public abstract void setGoingUp(boolean goingUp);
    @Shadow public abstract void setGoingDown(boolean goingDown);

    @Override
    public boolean tdmc$isFlying() {
        return isFlying();
    }

    @Override
    public boolean tdmc$isAccelerating() {
        return isAccelerating();
    }

    @Inject(method = "onRiderTakeoffRequest", at = @At("HEAD"), cancellable = true)
    private void tdmc$guardTakeoff(Player rider, CallbackInfo ci) {
        if (!RideActionApi.allows(rider, tdmc$self(), RideAction.FLIGHT)) {
            ci.cancel();
        }
    }

    @Inject(method = "onRiderAccelerationStart", at = @At("HEAD"), cancellable = true)
    private void tdmc$guardAcceleration(Player rider, CallbackInfo ci) {
        if (!RideActionApi.allows(rider, tdmc$self(), RideAction.BOOST)) {
            setAccelerating(false);
            ci.cancel();
        }
    }

    @Inject(method = "onRiderAbilityUse", at = @At("HEAD"), cancellable = true)
    private void tdmc$guardAbility(Player rider, String abilityName, CallbackInfo ci) {
        if (!RideActionApi.allows(rider, tdmc$self(), RideAction.ABILITY)) {
            ci.cancel();
        }
    }

    @Inject(method = "applyRiderVerticalInput", at = @At("HEAD"), cancellable = true)
    private void tdmc$enforceFlightState(Player rider, boolean goingUp, boolean goingDown, boolean inputLocked, CallbackInfo ci) {
        if (isFlying() && !RideActionApi.allows(rider, tdmc$self(), RideAction.FLIGHT)) {
            setAccelerating(false);
            setGoingUp(false);
            setGoingDown(true);
            ci.cancel();
        }
    }

    @Inject(method = "applyRiderMovementInput", at = @At("HEAD"))
    private void tdmc$enforceAccelerationState(Player rider, float forward, float strafe, boolean inputLocked, CallbackInfo ci) {
        if (!RideActionApi.allows(rider, tdmc$self(), RideAction.BOOST)) {
            setAccelerating(false);
        }
    }

    @Inject(method = "tickMountedState", at = @At("TAIL"))
    private void tdmc$enforceMountedExhaustion(CallbackInfo ci) {
        Player rider = tdmc$rider();
        if (rider == null) {
            tdmc$abilityBlocked = false;
            return;
        }
        LivingEntity self = tdmc$self();
        if (!RideActionApi.allows(rider, self, RideAction.BOOST)) {
            setAccelerating(false);
        }
        if (isFlying() && !RideActionApi.allows(rider, self, RideAction.FLIGHT)) {
            setAccelerating(false);
            setGoingUp(false);
            setGoingDown(true);
        }
        boolean abilityAllowed = RideActionApi.allows(rider, self, RideAction.ABILITY);
        if (!abilityAllowed && !tdmc$abilityBlocked) {
            tdmc$forceEndActiveAbilityIfPresent();
        }
        tdmc$abilityBlocked = !abilityAllowed;
    }

    @Unique
    private void tdmc$forceEndActiveAbilityIfPresent() {
        Optional<Method> method = tdmc$forceEndAbilityMethods.computeIfAbsent(
                tdmc$self().getClass(),
                SaintsRideableDragonMixin::tdmc$findForceEndActiveAbility
        );
        if (method.isEmpty()) {
            return;
        }
        try {
            method.get().invoke(tdmc$self());
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @Unique
    private static Optional<Method> tdmc$findForceEndActiveAbility(Class<?> type) {
        try {
            return Optional.of(type.getMethod("forceEndActiveAbility"));
        } catch (NoSuchMethodException exception) {
            return Optional.empty();
        }
    }

    private LivingEntity tdmc$self() {
        return (LivingEntity) (Object) this;
    }

    private Player tdmc$rider() {
        for (net.minecraft.world.entity.Entity passenger : tdmc$self().getPassengers()) {
            if (passenger instanceof Player player) {
                return player;
            }
        }
        return null;
    }
}
