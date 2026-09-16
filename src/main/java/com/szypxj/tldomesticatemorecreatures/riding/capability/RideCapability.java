package com.szypxj.tldomesticatemorecreatures.riding.capability;

import com.szypxj.tldomesticatemorecreatures.riding.RideEnvironment;
import com.szypxj.tldomesticatemorecreatures.riding.RideInputState;
import com.szypxj.tldomesticatemorecreatures.riding.RideRuntimeState;
import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

public interface RideCapability {
    String id();
    boolean supports(Mob mount, EntityRideProfile profile);
    RideEnvironment environment();
    boolean tick(ServerPlayer rider, Mob mount, RideInputState input, EntityRideProfile profile, RideRuntimeState runtime);
    void stop(Mob mount, RideRuntimeState runtime);
}
