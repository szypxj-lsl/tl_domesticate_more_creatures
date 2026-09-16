package com.szypxj.tldomesticatemorecreatures.api.torpor;

import com.szypxj.tldomesticatemorecreatures.torpor.TorporService;
import net.minecraft.world.entity.LivingEntity;

public final class TorporApi {
    private TorporApi() {
    }

    public static boolean isEnabled(LivingEntity entity) {
        return entity != null && TorporService.isEnabled(entity);
    }

    public static boolean isUnconscious(LivingEntity entity) {
        return entity != null && TorporService.isUnconscious(entity);
    }

    public static double currentTorpor(LivingEntity entity) {
        return entity == null ? 0.0D : TorporService.currentTorpor(entity);
    }

    public static double maxTorpor(LivingEntity entity) {
        return entity == null ? 0.0D : TorporService.maxTorpor(entity);
    }
}
