package com.szypxj.tldomesticatemorecreatures.torpor;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingService;
import com.szypxj.tldomesticatemorecreatures.item.NarcoticArrowItem;
import com.szypxj.tldomesticatemorecreatures.registry.ModEffects;
import com.szypxj.tldomesticatemorecreatures.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID)
public final class TorporEvents {
    private TorporEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            TorporService.onEntityJoin(living);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TorporService.syncClientState(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TorporService.syncClientState(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        Entity direct = event.getSource().getDirectEntity();
        if (direct instanceof AbstractArrow arrow
                && arrow.getPersistentData().getBoolean(NarcoticArrowItem.PROJECTILE_TAG)
                && TorporService.canGainTorpor(event.getEntity())) {
            int level = NarcoticArrowItem.getEffectLevel(arrow);
            int duration = Math.max(1, Config.NARCOTIC_ARROW_DURATION_SECONDS.get()) * 20;
            event.getEntity().addEffect(new MobEffectInstance(ModEffects.NARCOTIC.get(), duration, level - 1));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || event.getAmount() <= 0.0F) {
            return;
        }

        double damage = event.getAmount();
        if (TorporService.isUnconscious(entity)) {
            TamingService.recordUnconsciousDamage(entity, damage);

            if (damage >= entity.getHealth()) {
                if (entity.getHealth() < 1.0F) {
                    entity.setHealth(Math.min(1.0F, entity.getMaxHealth()));
                }
                event.setAmount((float) TorporMath.clampLethalDamage(entity.getHealth(), damage));
                TorporService.clear(entity);
                return;
            }

            TorporService.reduceFromDamage(entity, damage);
        }

        applyTorporWeaponHit(event, damage);
    }

    private static void applyTorporWeaponHit(LivingDamageEvent event, double finalDamage) {
        Entity direct = event.getSource().getDirectEntity();
        if (direct instanceof AbstractArrow arrow
                && arrow.getPersistentData().getBoolean(NarcoticArrowItem.PROJECTILE_TAG)) {
            int level = NarcoticArrowItem.getEffectLevel(arrow);
            TorporWeaponService.applyFromHit(
                    event.getEntity(),
                    finalDamage,
                    TorporMath.narcoticArrowImpactTorpor(level, Config.NARCOTIC_TORPOR_PER_LEVEL.get())
            );
            return;
        }

        Entity attackerEntity = event.getSource().getEntity();
        if (direct instanceof LivingEntity attacker
                && attackerEntity == attacker
                && attacker.getMainHandItem().is(ModItems.CLUB.get())) {
            TorporWeaponService.applyFromHit(
                    event.getEntity(),
                    finalDamage,
                    Config.CLUB_FIXED_TORPOR.get()
            );
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        Entity source = event.getSource().getEntity();
        if (source instanceof LivingEntity attacker && TorporService.isUnconscious(attacker)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != TickEvent.Phase.END || player.level().isClientSide) {
            return;
        }
        if (TorporService.clearIfPlayerImmune(player)) {
            return;
        }
        if (!TorporData.of(player).unconscious()) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(0.0D, Math.min(0.0D, motion.y), 0.0D);
        player.setSprinting(false);
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!event.getEntity().level().isClientSide && TorporData.of(event.getEntity()).unconscious()) {
            Vec3 motion = event.getEntity().getDeltaMovement();
            event.getEntity().setDeltaMovement(motion.x, Math.min(0.0D, motion.y), motion.z);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!event.getEntity().level().isClientSide && TorporData.of(event.getEntity()).unconscious()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteract(PlayerInteractEvent event) {
        if (!event.getEntity().level().isClientSide && TorporData.of(event.getEntity()).unconscious() && event.isCancelable()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (TorporData.of(event.getPlayer()).unconscious()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player && TorporData.of(event.getEntity()).unconscious()) {
            event.setCanceled(true);
        }
    }
}
