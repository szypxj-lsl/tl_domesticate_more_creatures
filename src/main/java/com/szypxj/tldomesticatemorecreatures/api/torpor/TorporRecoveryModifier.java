package com.szypxj.tldomesticatemorecreatures.api.torpor;

import net.minecraft.world.entity.LivingEntity;

@FunctionalInterface
public interface TorporRecoveryModifier {
    double recoveryPerSecond(
            LivingEntity entity,
            double currentTorpor,
            double maxTorpor,
            double normalRecoveryPerSecond,
            boolean unconscious
    );
}
