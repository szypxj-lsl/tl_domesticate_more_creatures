package com.szypxj.tldomesticatemorecreatures.api.riding;

public record RideActionStatus(
        RideActionState state,
        int remainingTicks,
        int totalTicks,
        float chargeProgress
) {
    public static final RideActionStatus READY = new RideActionStatus(RideActionState.READY, 0, 0, 0.0F);
    public static final RideActionStatus BLOCKED = new RideActionStatus(RideActionState.BLOCKED, 0, 0, 0.0F);
    public static final RideActionStatus UNSUPPORTED = new RideActionStatus(RideActionState.UNSUPPORTED, 0, 0, 0.0F);

    public RideActionStatus {
        state = state == null ? RideActionState.READY : state;
        remainingTicks = Math.max(0, remainingTicks);
        totalTicks = Math.max(0, totalTicks);
        if (!Float.isFinite(chargeProgress)) {
            chargeProgress = 0.0F;
        }
        chargeProgress = Math.max(0.0F, Math.min(1.0F, chargeProgress));
    }

    public static RideActionStatus cooldown(int remainingTicks, int totalTicks) {
        if (remainingTicks <= 0 || totalTicks <= 0) {
            return READY;
        }
        return new RideActionStatus(RideActionState.COOLDOWN, remainingTicks, totalTicks, 0.0F);
    }

    public static RideActionStatus charging(float progress) {
        return new RideActionStatus(RideActionState.CHARGING, 0, 0, progress);
    }

    public static RideActionStatus active(float progress) {
        return new RideActionStatus(RideActionState.ACTIVE, 0, 0, progress);
    }
}
