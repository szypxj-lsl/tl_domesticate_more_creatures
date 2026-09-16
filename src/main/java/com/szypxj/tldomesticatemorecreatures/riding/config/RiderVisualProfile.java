package com.szypxj.tldomesticatemorecreatures.riding.config;

import net.minecraft.util.Mth;

public record RiderVisualProfile(
        double lateralRatio,
        double verticalRatio,
        double forwardRatio,
        double offsetX,
        double offsetY,
        double offsetZ,
        RiderPosePreset posePreset,
        float bodyPitch,
        float bodyYawOffset,
        LimbRotation leftLeg,
        LimbRotation rightLeg,
        LimbRotation leftArm,
        LimbRotation rightArm,
        float visualPlayerScale,
        double cameraVerticalOffset,
        double cameraBackwardOffset
) {
    public static RiderVisualProfile defaults() {
        return new RiderVisualProfile(
                0.0D, 0.75D, 0.0D,
                0.0D, 0.0D, 0.0D,
                RiderPosePreset.SIT_NORMAL,
                0.0F, 0.0F,
                LimbRotation.ZERO, LimbRotation.ZERO,
                LimbRotation.ZERO, LimbRotation.ZERO,
                1.0F, 0.0D, 0.0D
        );
    }

    public RiderVisualProfile validated() {
        return new RiderVisualProfile(
                clampSeatRatio(lateralRatio),
                clampSeatRatio(verticalRatio),
                clampSeatRatio(forwardRatio),
                clampOffset(offsetX),
                clampOffset(offsetY),
                clampOffset(offsetZ),
                posePreset == null ? RiderPosePreset.SIT_NORMAL : posePreset,
                clampAngle(bodyPitch),
                clampAngle(bodyYawOffset),
                validateRotation(leftLeg),
                validateRotation(rightLeg),
                validateRotation(leftArm),
                validateRotation(rightArm),
                clampScale(visualPlayerScale),
                clampOffset(cameraVerticalOffset),
                clampOffset(cameraBackwardOffset)
        );
    }

    public static double clampMultiplier(double value) {
        return Mth.clamp(value, 0.05D, 8.0D);
    }

    public static double clampSeatRatio(double value) {
        return Mth.clamp(value, -2.0D, 2.0D);
    }

    public static double clampOffset(double value) {
        return Mth.clamp(value, -16.0D, 16.0D);
    }

    public static float clampAngle(float value) {
        return Mth.clamp(value, -180.0F, 180.0F);
    }

    public static float clampScale(float value) {
        return Mth.clamp(value, 0.25F, 4.0F);
    }

    private static LimbRotation validateRotation(LimbRotation value) {
        return value == null ? LimbRotation.ZERO : value.validated();
    }

    public record LimbRotation(float pitch, float yaw, float roll) {
        public static final LimbRotation ZERO = new LimbRotation(0.0F, 0.0F, 0.0F);

        public LimbRotation validated() {
            return new LimbRotation(clampAngle(pitch), clampAngle(yaw), clampAngle(roll));
        }
    }
}
