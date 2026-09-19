package com.szypxj.tldomesticatemorecreatures.compat.iceandfire;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Ensures held Ice and Fire control bits cannot survive a TDMC-controlled dismount. */
@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID)
public final class IceAndFireRideControlEvents {
    private IceAndFireRideControlEvents() {
    }

    @SubscribeEvent
    public static void onUnmount(EntityMountEvent event) {
        if (event.isMounting() || event.getEntityMounting().level().isClientSide) {
            return;
        }
        if (event.getEntityMounting() instanceof ServerPlayer rider
                && event.getEntityBeingMounted() instanceof LivingEntity mount
                && mount instanceof IceAndFireRideBridge bridge
                && bridge.tdmc$isRidingPlayer(rider)) {
            IceAndFireRideControlProvider.clearControls(mount);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.getVehicle() instanceof LivingEntity mount
                && mount instanceof IceAndFireRideBridge bridge
                && bridge.tdmc$isRidingPlayer(player)) {
            IceAndFireRideControlProvider.clearControls(mount);
        }
    }
}
