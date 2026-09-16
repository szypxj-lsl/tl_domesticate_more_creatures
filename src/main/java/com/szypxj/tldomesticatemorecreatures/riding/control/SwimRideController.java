package com.szypxj.tldomesticatemorecreatures.riding.control;

import com.szypxj.tldomesticatemorecreatures.riding.RideEnvironment;
import com.szypxj.tldomesticatemorecreatures.riding.RideInputState;
import com.szypxj.tldomesticatemorecreatures.riding.RideMovementMode;
import com.szypxj.tldomesticatemorecreatures.riding.RideRuntimeState;
import com.szypxj.tldomesticatemorecreatures.riding.capability.RideCapability;
import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public final class SwimRideController implements RideCapability {
    @Override
    public String id() {
        return "generic_swim";
    }

    @Override
    public boolean supports(Mob mount, EntityRideProfile profile) {
        RideMovementMode mode = profile.movementMode();
        return mode == RideMovementMode.AUTO || mode == RideMovementMode.SWIM
                || mode == RideMovementMode.GROUND_SWIM || mode == RideMovementMode.FLIGHT_SWIM;
    }

    @Override
    public RideEnvironment environment() {
        return RideEnvironment.WATER;
    }

    @Override
    public boolean tick(ServerPlayer rider, Mob mount, RideInputState input, EntityRideProfile profile, RideRuntimeState runtime) {
        mount.getNavigation().stop();
        RideControlMath.applyRiderHeading(mount, rider.getYRot(), input.strafe(), profile.turnRateDegrees());

        Vec3 horizontal = RideControlMath.worldForwardDirection(mount.getYRot(), input.forward());
        double horizontalScale = RideControlMath.horizontalSpeedScale(input.forward());
        double vertical = input.jump() ? 1.0D : input.descend() ? -1.0D : 0.0D;
        Vec3 travel = new Vec3(horizontal.x * horizontalScale, vertical, horizontal.z * horizontalScale);
        double speed = Math.max(0.0D, mount.getAttributeValue(Attributes.MOVEMENT_SPEED)) * profile.swimSpeedMultiplier();
        double activeSpeed = travel.lengthSqr() > 1.0E-8D ? speed : 0.0D;

        runtime.lastMovementVector(travel);
        runtime.currentSpeed(activeSpeed);
        mount.setSpeed((float) activeSpeed);
        runtime.lastJumpDown(input.jump());
        return true;
    }

    @Override
    public void stop(Mob mount, RideRuntimeState runtime) {
        runtime.lastMovementVector(Vec3.ZERO);
        runtime.currentSpeed(0.0D);
    }
}
