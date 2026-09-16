package com.szypxj.tldomesticatemorecreatures.riding;

import com.szypxj.tldomesticatemorecreatures.client.riding.ClientRideSeatAccess;
import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.RiderVisualProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingConfigManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Unified rider positioning. The math deliberately mirrors Salvation's
 * UnifiedSeatPositioner: X and Z are offsets from the vehicle AABB centre,
 * Y is anchored from minY, and local X/Z are rotated by vehicle yaw.
 */
public final class RideSeatPositioner {
    private RideSeatPositioner() {
    }

    public static void positionOrVanilla(Entity mount, Entity passenger) {
        if (!(mount instanceof LivingEntity) || !(passenger instanceof Player)) {
            mount.positionRider(passenger);
            return;
        }
        SeatVisualState state = state(mount);
        if (state == null || !state.overrideSeat()) {
            mount.positionRider(passenger);
            return;
        }
        position(mount, passenger, state.visual());
    }

    public static void position(Entity mount, Entity passenger, RiderVisualProfile visual) {
        Vec3 position = computePosition(mount, passenger, visual);
        passenger.setPos(position.x, position.y, position.z);
    }

    public static Vec3 computePosition(Entity mount, Entity passenger, RiderVisualProfile visual) {
        RiderVisualProfile profile = visual == null ? RiderVisualProfile.defaults() : visual.validated();
        AABB bounds = mount.getBoundingBox();
        Vec3 center = bounds.getCenter();

        double localX = bounds.getXsize() * 0.5D * profile.lateralRatio() + profile.offsetX();
        double localZ = bounds.getZsize() * 0.5D * profile.forwardRatio() + profile.offsetZ();
        double y = bounds.minY
                + bounds.getYsize() * profile.verticalRatio()
                + passenger.getMyRidingOffset()
                + profile.offsetY();

        double radians = Math.toRadians(mount.getYRot());
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        double worldX = localX * cos - localZ * sin;
        double worldZ = localX * sin + localZ * cos;
        return new Vec3(center.x + worldX, y, center.z + worldZ);
    }

    private static SeatVisualState state(Entity mount) {
        if (mount == null) {
            return null;
        }
        if (mount.level().isClientSide) {
            return DistExecutor.unsafeCallWhenOn(
                    Dist.CLIENT,
                    () -> () -> ClientRideSeatAccess.state(mount)
            );
        }
        if (mount instanceof LivingEntity livingMount && NativeRideCapabilityResolver.hasNativePlayerControl(livingMount)) {
            return new SeatVisualState(false, null);
        }
        EntityRideProfile profile = RidingConfigManager.profile(mount.getType());
        boolean override = (mount instanceof LivingEntity livingMount && RideService.isGenericControlled(livingMount))
                || profile.mode() == RideMode.FORCE_GENERIC
                || profile.visualOverride();
        return new SeatVisualState(override, profile.visual());
    }

    public record SeatVisualState(boolean overrideSeat, RiderVisualProfile visual) {
    }
}
