package com.szypxj.tldomesticatemorecreatures.riding.config;

import com.szypxj.tldomesticatemorecreatures.riding.RideMode;
import com.szypxj.tldomesticatemorecreatures.riding.RideMovementMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Objects;

public record EntityRideProfile(
        ResourceLocation entityId,
        RideMode mode,
        RideMovementMode movementMode,
        double groundSpeedMultiplier,
        double turnRateDegrees,
        double acceleration,
        double deceleration,
        double jumpStrength,
        double flightSpeedMultiplier,
        double ascentSpeed,
        double descentSpeed,
        double swimSpeedMultiplier,
        boolean autoAttackWhileRidden,
        boolean visualOverride,
        RiderVisualProfile visual
) {
    public static EntityRideProfile defaults(ResourceLocation entityId) {
        return new EntityRideProfile(
                Objects.requireNonNull(entityId),
                RideMode.AUTO,
                RideMovementMode.AUTO,
                1.0D,
                12.0D,
                0.12D,
                0.18D,
                0.42D,
                1.0D,
                0.35D,
                0.25D,
                1.0D,
                true,
                false,
                RiderVisualProfile.defaults()
        );
    }

    public EntityRideProfile validated() {
        return new EntityRideProfile(
                Objects.requireNonNull(entityId),
                mode == null ? RideMode.AUTO : mode,
                movementMode == null ? RideMovementMode.AUTO : movementMode,
                RiderVisualProfile.clampMultiplier(groundSpeedMultiplier),
                Mth.clamp(turnRateDegrees, 0.1D, 180.0D),
                Mth.clamp(acceleration, 0.001D, 4.0D),
                Mth.clamp(deceleration, 0.001D, 4.0D),
                Mth.clamp(jumpStrength, 0.0D, 4.0D),
                RiderVisualProfile.clampMultiplier(flightSpeedMultiplier),
                Mth.clamp(ascentSpeed, 0.0D, 4.0D),
                Mth.clamp(descentSpeed, 0.0D, 4.0D),
                RiderVisualProfile.clampMultiplier(swimSpeedMultiplier),
                autoAttackWhileRidden,
                visualOverride,
                visual == null ? RiderVisualProfile.defaults() : visual.validated()
        );
    }
}
