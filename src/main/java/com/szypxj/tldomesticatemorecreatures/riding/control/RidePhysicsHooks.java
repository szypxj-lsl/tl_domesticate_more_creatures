package com.szypxj.tldomesticatemorecreatures.riding.control;

import com.szypxj.tldomesticatemorecreatures.riding.RideEnvironment;
import com.szypxj.tldomesticatemorecreatures.riding.RideRuntimeState;
import com.szypxj.tldomesticatemorecreatures.riding.RideService;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class RidePhysicsHooks {
    private RidePhysicsHooks() {
    }

    public static Vec3 overrideTravelVector(LivingEntity entity, Vec3 vanillaInput) {
        RideRuntimeState runtime = RideService.runtime(entity);
        if (runtime == null) {
            return vanillaInput;
        }
        if (runtime.environment() == RideEnvironment.GROUND) {
            return Vec3.ZERO;
        }
        return RideControlMath.worldToLocalTravelInput(entity.getYRot(), runtime.lastMovementVector());
    }

    public static void beforeTravel(LivingEntity entity) {
        RideRuntimeState runtime = RideService.runtime(entity);
        if (runtime == null || !(entity instanceof Mob mob)) {
            return;
        }
        mob.getNavigation().stop();
        mob.setSpeed((float) runtime.currentSpeed());
        if (runtime.environment() == RideEnvironment.GROUND) {
            Vec3 motion = mob.getDeltaMovement();
            mob.setDeltaMovement(0.0D, motion.y, 0.0D);
        }
    }
}

