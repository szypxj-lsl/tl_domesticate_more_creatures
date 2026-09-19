package com.szypxj.tldomesticatemorecreatures.riding;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.riding.control.action.RideControlDispatcher;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID)
public final class RideEvents {
    private RideEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            RideService.serverTick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onEntityMount(EntityMountEvent event) {
        if (event.getEntityMounting().level().isClientSide) return;
        if (!(event.getEntityMounting() instanceof ServerPlayer rider)
                || !(event.getEntityBeingMounted() instanceof LivingEntity mount)) {
            return;
        }
        var server = rider.getServer();
        if (server == null) return;
        boolean mounting = event.isMounting();
        server.execute(() -> {
            if (mounting) {
                if (rider.getVehicle() == mount) {
                    RideControlDispatcher.syncProfile(rider, mount, true);
                }
            } else if (rider.getVehicle() != mount) {
                RideControlDispatcher.syncProfile(rider, mount, false);
            }
        });
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity living && RideService.isGenericControlled(living)) {
            RideService.stopRide(living, RideService.StopReason.ENTITY_REMOVED);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RideAttackService.clearPlayer(player);
            RideControlDispatcher.clearPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        RideAttackService.clearAll();
        RideControlDispatcher.clearAll();
        RideService.clearAll();
    }
}
