package com.szypxj.tldomesticatemorecreatures.talent;

import com.szypxj.tldomesticatemorecreatures.config.SpecialTalentConfigManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class BleedingEffect extends MobEffect {
    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0xB32626);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide || !entity.isAlive()) {
            return;
        }
        double percent = SpecialTalentConfigManager.bloodthirsty().bleedCurrentHealthPercentPerSecond();
        float damage = (float) SpecialTalentMath.bleedingDamage(entity.getHealth(), percent);
        if (damage > 0.0F) {
            entity.hurt(BleedingDamage.source(entity.level()), damage);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}
