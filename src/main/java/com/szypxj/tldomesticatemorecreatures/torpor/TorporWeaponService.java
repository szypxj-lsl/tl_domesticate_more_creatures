package com.szypxj.tldomesticatemorecreatures.torpor;

import net.minecraft.world.entity.LivingEntity;

public final class TorporWeaponService {
    private TorporWeaponService() {
    }

    public static void applyFromHit(LivingEntity target, double finalDamage, double fixedTorpor) {
        if (target.level().isClientSide || finalDamage <= 0.0D || !TorporService.canGainTorpor(target)) {
            return;
        }
        TorporService.addTorpor(target, TorporWeaponMath.torporFromHit(finalDamage, fixedTorpor));
    }
}
