package com.szypxj.tldomesticatemorecreatures.torpor;

import com.szypxj.tldomesticatemorecreatures.config.Config;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class NarcoticEffect extends MobEffect {
    public NarcoticEffect() {
        super(MobEffectCategory.HARMFUL, 0x6AA5D8);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        TorporService.addTorpor(
                entity,
                TorporMath.narcoticPerSecond(amplifier + 1, Config.NARCOTIC_TORPOR_PER_LEVEL.get())
        );
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}
