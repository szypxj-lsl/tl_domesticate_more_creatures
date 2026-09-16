package com.szypxj.tldomesticatemorecreatures.riding.provider;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideStateApi;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideStateProvider;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideStateSnapshot;
import com.szypxj.tldomesticatemorecreatures.riding.NativeRideStateMarker;
import com.szypxj.tldomesticatemorecreatures.riding.RideEnvironment;
import com.szypxj.tldomesticatemorecreatures.riding.RideRuntimeState;
import com.szypxj.tldomesticatemorecreatures.riding.RideService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

public final class RideStateBridge {
    private static final ResourceLocation ID = Objects.requireNonNull(
            ResourceLocation.tryBuild(TlDomesticateMoreCreatures.MOD_ID, "builtin_ride_state")
    );
    private static boolean registered;

    private RideStateBridge() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        RideStateApi.register(ID, new Provider());
        registered = true;
    }

    private static final class Provider implements RideStateProvider {
        @Override
        public boolean supports(LivingEntity entity) {
            return RideService.isGenericControlled(entity) || entity instanceof NativeRideStateMarker;
        }

        @Override
        public RideStateSnapshot snapshot(LivingEntity entity) {
            RideRuntimeState runtime = RideService.runtime(entity);
            if (runtime != null) {
                return new RideStateSnapshot(true, runtime.environment() == RideEnvironment.AIR && !entity.onGround(), false);
            }
            if (entity instanceof NativeRideStateMarker nativeState) {
                boolean ridden = entity.getPassengers().stream().anyMatch(Player.class::isInstance);
                return new RideStateSnapshot(ridden, nativeState.tdmc$isFlying(), nativeState.tdmc$isAccelerating());
            }
            return RideStateSnapshot.NONE;
        }
    }
}
