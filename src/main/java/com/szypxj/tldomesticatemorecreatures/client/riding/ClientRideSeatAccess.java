package com.szypxj.tldomesticatemorecreatures.client.riding;

import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import com.szypxj.tldomesticatemorecreatures.riding.NativeRideCapabilityResolver;
import com.szypxj.tldomesticatemorecreatures.riding.RideMode;
import com.szypxj.tldomesticatemorecreatures.riding.RideSeatPositioner;
import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.RiderVisualProfile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public final class ClientRideSeatAccess {
    private ClientRideSeatAccess() {
    }

    public static RideSeatPositioner.SeatVisualState state(Entity mount) {
        if (mount instanceof LivingEntity living && NativeRideCapabilityResolver.hasNativePlayerControl(living)) {
            return new RideSeatPositioner.SeatVisualState(false, null);
        }
        RiderVisualProfile authorized = RideVisualAuthority.clientProfile(mount.getUUID());
        if (authorized != null) {
            return new RideSeatPositioner.SeatVisualState(true, authorized);
        }

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mount.getType());
        if (id == null) {
            return new RideSeatPositioner.SeatVisualState(false, null);
        }
        EntityRideProfile profile = ClientRidingConfigCache.profile(id);
        boolean override = ClientState.isGenericRideMount(mount.getId())
                || profile.mode() == RideMode.FORCE_GENERIC
                || profile.visualOverride();
        return new RideSeatPositioner.SeatVisualState(override, profile.visual());
    }
}
