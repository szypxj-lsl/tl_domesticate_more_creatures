package com.szypxj.tldomesticatemorecreatures.riding.control;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class RideControlMath {
    public static final double BACKWARD_SPEED_MULTIPLIER = 0.5D;
    private static final double INPUT_EPSILON = 1.0E-4D;
    private static final double INPUT_EPSILON_SQR = 1.0E-8D;

    private RideControlMath() {
    }

    public static Vec3 worldForwardDirection(float mountYawDegrees, float forwardInput) {
        if (Math.abs(forwardInput) < INPUT_EPSILON) {
            return Vec3.ZERO;
        }
        double direction = forwardInput > 0.0F ? 1.0D : -1.0D;
        float yaw = mountYawDegrees * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw);
        double cos = Mth.cos(yaw);
        return new Vec3(-sin * direction, 0.0D, cos * direction);
    }

    public static double horizontalSpeedScale(float forwardInput) {
        double magnitude = Mth.clamp(Math.abs((double) forwardInput), 0.0D, 1.0D);
        if (magnitude < INPUT_EPSILON) {
            return 0.0D;
        }
        return magnitude * (forwardInput < 0.0F ? BACKWARD_SPEED_MULTIPLIER : 1.0D);
    }

    public static Vec3 worldToLocalTravelInput(float mountYawDegrees, Vec3 worldInput) {
        if (worldInput == null || worldInput.lengthSqr() < INPUT_EPSILON_SQR) {
            return Vec3.ZERO;
        }
        float yaw = mountYawDegrees * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw);
        double cos = Mth.cos(yaw);
        return new Vec3(
                worldInput.x * cos + worldInput.z * sin,
                worldInput.y,
                worldInput.z * cos - worldInput.x * sin
        );
    }

    public static double approachSpeed(
            double current,
            double target,
            double acceleration,
            double deceleration
    ) {
        double safeCurrent = Math.max(0.0D, current);
        double safeTarget = Math.max(0.0D, target);
        double step = safeTarget > safeCurrent ? Math.max(0.0D, acceleration) : Math.max(0.0D, deceleration);
        if (safeCurrent < safeTarget) {
            return Math.min(safeTarget, safeCurrent + step);
        }
        if (safeCurrent > safeTarget) {
            return Math.max(safeTarget, safeCurrent - step);
        }
        return safeCurrent;
    }

    public static void applyRiderHeading(Mob mount, float viewYawDegrees, float turnInput, double turnRateDegrees) {
        if (mount == null) {
            return;
        }
        float step = (float) Math.max(0.0D, turnRateDegrees);
        float yaw;
        if (Math.abs(turnInput) >= INPUT_EPSILON) {
            float clampedTurn = Mth.clamp(turnInput, -1.0F, 1.0F);
            yaw = Mth.wrapDegrees(mount.getYRot() - clampedTurn * step);
        } else {
            yaw = Mth.approachDegrees(mount.getYRot(), viewYawDegrees, step);
        }
        mount.setYRot(yaw);
        mount.yBodyRot = yaw;
        mount.setYHeadRot(yaw);
    }
}
