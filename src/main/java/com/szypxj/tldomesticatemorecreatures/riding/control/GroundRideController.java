package com.szypxj.tldomesticatemorecreatures.riding.control;

import com.szypxj.tldomesticatemorecreatures.riding.RideEnvironment;
import com.szypxj.tldomesticatemorecreatures.riding.RideInputState;
import com.szypxj.tldomesticatemorecreatures.riding.RideMovementMode;
import com.szypxj.tldomesticatemorecreatures.riding.RideRuntimeState;
import com.szypxj.tldomesticatemorecreatures.riding.capability.RideCapability;
import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import com.szypxj.tldomesticatemorecreatures.riding.provider.RideCompatibilityApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public final class GroundRideController implements RideCapability {
    @Override
    public String id() {
        return "generic_ground";
    }

    @Override
    public boolean supports(Mob mount, EntityRideProfile profile) {
        RideMovementMode mode = profile.movementMode();
        return mode == RideMovementMode.AUTO || mode == RideMovementMode.GROUND || mode == RideMovementMode.GROUND_SWIM;
    }

    @Override
    public RideEnvironment environment() {
        return RideEnvironment.GROUND;
    }

    @Override
    public boolean tick(ServerPlayer rider, Mob mount, RideInputState input, EntityRideProfile profile, RideRuntimeState runtime) {
        mount.getNavigation().stop();

        RideControlMath.applyRiderHeading(mount, rider.getYRot(), input.strafe(), profile.turnRateDegrees());
        Vec3 world = RideControlMath.worldForwardDirection(mount.getYRot(), input.forward());
        double baseSpeed = Math.max(0.0D, mount.getAttributeValue(Attributes.MOVEMENT_SPEED));
        double targetSpeed = baseSpeed * profile.groundSpeedMultiplier() * RideControlMath.horizontalSpeedScale(input.forward());
        double current = RideControlMath.approachSpeed(
                runtime.currentSpeed(),
                targetSpeed,
                profile.acceleration(),
                profile.deceleration()
        );
        runtime.currentSpeed(current);
        runtime.lastMovementVector(world);
        mount.setSpeed((float) current);

        boolean wasOnGround = mount.onGround();
        Vec3 existingMotion = mount.getDeltaMovement();
        double moveX = world.x * current;
        double moveZ = world.z * current;
        mount.setDeltaMovement(moveX, existingMotion.y, moveZ);
        if (world.lengthSqr() > 1.0E-8D && current > 1.0E-8D) {
            mount.move(MoverType.SELF, new Vec3(moveX, 0.0D, moveZ));
        }

        boolean jumpEdge = input.jump() && !runtime.lastJumpDown();
        if (jumpEdge && wasOnGround && runtime.jumpCooldownTicks() == 0) {
            if (!providerJump(mount, profile.jumpStrength())) {
                Vec3 movement = mount.getDeltaMovement();
                mount.setDeltaMovement(movement.x, Math.max(movement.y, profile.jumpStrength()), movement.z);
                mount.hasImpulse = true;
            }
            runtime.jumpCooldownTicks(5);
        }
        runtime.lastJumpDown(input.jump());
        return true;
    }

    @Override
    public void stop(Mob mount, RideRuntimeState runtime) {
        runtime.lastMovementVector(Vec3.ZERO);
        runtime.currentSpeed(0.0D);
        runtime.lastJumpDown(false);
    }

    private static boolean providerJump(Mob mount, double strength) {
        for (RideCompatibilityApi.JumpRideProvider provider : RideCompatibilityApi.jumpRideProviders()) {
            if (!RideCompatibilityApi.enabled(provider)) {
                continue;
            }
            try {
                if (provider.supports(mount) && provider.jump(mount, strength)) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                RideCompatibilityApi.disableProvider(provider);
            }
        }
        return false;
    }
}
