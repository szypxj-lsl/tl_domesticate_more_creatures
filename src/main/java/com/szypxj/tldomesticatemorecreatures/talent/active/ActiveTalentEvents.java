package com.szypxj.tldomesticatemorecreatures.talent.active;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID)
public final class ActiveTalentEvents {
    private ActiveTalentEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ActiveTalentService.tick(event.getServer());
        }
    }


    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntityMounting() instanceof net.minecraft.server.level.ServerPlayer rider)
                || !(event.getEntityBeingMounted() instanceof LivingEntity mount)) return;
        if (event.isMounting()) {
            ActiveTalentService.syncCurrent(rider, mount);
        } else {
            ActiveTalentRuntimeState state = ActiveTalentService.runtime(mount);
            if (state != null && state.riderUuid().equals(rider.getUUID())) {
                ActiveTalentService.cancelForInterruption(mount, state);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Projectile projectile) {
            CamouflageService.captureProjectileAmbush(projectile);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof LivingEntity living)) return;
        ActiveTalentRuntimeState state = ActiveTalentService.runtime(living);
        if (state != null) ActiveTalentService.cancelForInterruption(living, state);
        if (living instanceof net.minecraft.world.entity.Mob mob) {
            ShadowstepSlowRegistry.clear(mob);
            CamouflageService.onMobRemoved(mob);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            ActiveTalentService.clearInputSequence(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer viewer
                && event.getTarget() instanceof LivingEntity mount) {
            CamouflageService.syncVisualTo(viewer, mount);
        }
    }


    @SubscribeEvent
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (!event.getEntity().level().isClientSide && ActiveTalentService.isShadowstepExecuting(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!event.getEntity().level().isClientSide && event.getAmount() > 0.0F) {
            CamouflageService.onPositiveIncomingDamage(event.getEntity(), event.getAmount());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ActiveTalentService.clearAll(event.getServer());
    }
}
