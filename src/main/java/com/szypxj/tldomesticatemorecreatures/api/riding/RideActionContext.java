package com.szypxj.tldomesticatemorecreatures.api.riding;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;

public record RideActionContext(
        ServerPlayer rider,
        LivingEntity mount,
        RideInputPhase phase,
        int sequence
) {
    public RideActionContext {
        rider = Objects.requireNonNull(rider, "rider");
        mount = Objects.requireNonNull(mount, "mount");
        phase = Objects.requireNonNull(phase, "phase");
    }
}
