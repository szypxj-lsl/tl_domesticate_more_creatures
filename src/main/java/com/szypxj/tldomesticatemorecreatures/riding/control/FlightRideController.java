package com.szypxj.tldomesticatemorecreatures.riding.control;

import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionApi;
import com.szypxj.tldomesticatemorecreatures.riding.RideEnvironment;
import com.szypxj.tldomesticatemorecreatures.riding.RideInputState;
import com.szypxj.tldomesticatemorecreatures.riding.RideMovementMode;
import com.szypxj.tldomesticatemorecreatures.riding.RideRuntimeState;
import com.szypxj.tldomesticatemorecreatures.riding.capability.RideCapability;
import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public final class FlightRideController implements RideCapability {
    @Override
    public String id() {
        return "generic_flight";
    }

    @Override
    public boolean supports(Mob mount, EntityRideProfile profile) {
        RideMovementMode mode = profile.movementMode();
        return mode == RideMovementMode.AUTO || mode == RideMovementMode.FLIGHT || mode == RideMovementMode.FLIGHT_SWIM;
    }

    @Override
    public RideEnvironment environment() {
        return RideEnvironment.AIR;
    }

    @Override
    public boolean tick(ServerPlayer rider, Mob mount, RideInputState input, EntityRideProfile profile, RideRuntimeState runtime) {
        mount.getNavigation().stop();
        if (!runtime.gravityCaptured()) {
            runtime.previousNoGravity(mount.isNoGravity());
            runtime.gravityCaptured(true);
        }
        mount.setNoGravity(true);

        RideControlMath.applyRiderHeading(mount, rider.getYRot(), input.strafe(), profile.turnRateDegrees());
        Vec3 horizontal = RideControlMath.worldForwardDirection(mount.getYRot(), input.forward());
        double baseSpeed = Math.max(0.0D, mount.getAttributeValue(Attributes.MOVEMENT_SPEED));
        double targetSpeed = baseSpeed * profile.flightSpeedMultiplier() * RideControlMath.horizontalSpeedScale(input.forward());
        double current = RideControlMath.approachSpeed(
                runtime.currentSpeed(),
                targetSpeed,
                profile.acceleration(),
                profile.deceleration()
        );
        runtime.currentSpeed(current);
        runtime.lastMovementVector(horizontal);
        mount.setSpeed((float) current);

        boolean flightAllowed = RideActionApi.allows(rider, mount, RideAction.FLIGHT);
        double verticalTarget = flightAllowed
                ? input.jump() ? profile.ascentSpeed() : input.descend() ? -profile.descentSpeed() : 0.0D
                : mount.onGround() ? 0.0D : -profile.descentSpeed();
        Vec3 motion = mount.getDeltaMovement();
        double vertical = Mth.lerp(0.35D, motion.y, verticalTarget);
        mount.setDeltaMovement(motion.x, vertical, motion.z);

        runtime.lastJumpDown(input.jump());
        return true;
    }

    @Override
    public void stop(Mob mount, RideRuntimeState runtime) {
        runtime.lastMovementVector(Vec3.ZERO);
        runtime.currentSpeed(0.0D);
        if (runtime.gravityCaptured()) {
            mount.setNoGravity(runtime.previousNoGravity());
            runtime.gravityCaptured(false);
        }
    }
}
