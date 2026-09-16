package com.szypxj.tldomesticatemorecreatures.talent;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.config.SpecialTalentConfigManager;
import com.szypxj.tldomesticatemorecreatures.registry.ModEffects;
import com.szypxj.tldomesticatemorecreatures.talent.active.CamouflageService;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID)
public final class SpecialTalentCombatEvents {
    private SpecialTalentCombatEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0.0F || BleedingDamage.is(event.getSource())) {
            return;
        }
        LivingEntity attacker = resolveAttacker(event.getSource());
        if (attacker == null) {
            return;
        }
        double damage = event.getAmount();
        if (FuryService.hasFury(attacker)) {
            var fury = SpecialTalentConfigManager.fury();
            damage = SpecialTalentMath.outgoingDamage(
                    damage,
                    FuryService.isBerserk(attacker),
                    fury.normalOutgoingDamageMultiplier(),
                    fury.berserkOutgoingDamageMultiplier()
            );
        }
        double ambushMultiplier = event.getSource().getDirectEntity() instanceof Projectile projectile
                ? CamouflageService.takeProjectileAmbushMultiplier(projectile)
                : CamouflageService.consumeAmbushMultiplier(attacker);
        damage *= ambushMultiplier;
        event.setAmount((float) Math.max(0.0D, damage));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || event.getAmount() <= 0.0F) {
            return;
        }

        boolean bleeding = BleedingDamage.is(event.getSource());
        if (FuryService.hasFury(victim)) {
            double amplified = SpecialTalentMath.incomingDamage(
                    event.getAmount(),
                    SpecialTalentConfigManager.fury().incomingDamageMultiplier()
            );
            event.setAmount((float) Math.max(0.0D, amplified));
            double actualDamage = Math.min(Math.max(0.0D, event.getAmount()), Math.max(0.0D, victim.getHealth()));
            FuryService.recordDamage(victim, actualDamage, !bleeding);
        }

        if (bleeding || event.getAmount() <= 0.0F) {
            return;
        }
        LivingEntity attacker = resolveAttacker(event.getSource());
        if (attacker == null || attacker == victim || !SpecialTalentService.hasActive(attacker, SpecialTalentIds.BLOODTHIRSTY)) {
            return;
        }
        int duration = Math.max(1, SpecialTalentConfigManager.bloodthirsty().bleedDurationSeconds()) * 20;
        victim.addEffect(new MobEffectInstance(ModEffects.BLEEDING.get(), duration, 0, false, true, false));
    }

    private static LivingEntity resolveAttacker(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living) {
            return living;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner) {
            return owner;
        }
        return direct instanceof LivingEntity living ? living : null;
    }
}
