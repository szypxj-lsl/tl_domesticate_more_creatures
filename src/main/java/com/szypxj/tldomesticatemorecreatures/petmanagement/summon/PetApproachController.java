package com.szypxj.tldomesticatemorecreatures.petmanagement.summon;

import com.szypxj.tldomesticatemorecreatures.mixin.accessor.MobGoalSelectorAccessor;
import com.szypxj.tldomesticatemorecreatures.riding.RideService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public final class PetApproachController {
    private static final int MAX_AGE_TICKS = PetApproachMath.MAX_AGE_TICKS;

    private PetApproachController() {
    }

    public static void beginApproach(LivingEntity pet, PetSummonSession session) {
        if (pet == null || session == null || session.approachPrepared()) return;
        session.originalNoPhysics(pet.noPhysics);
        session.originalNoGravity(pet.isNoGravity());
        session.originalNoAi(pet instanceof Mob mob && mob.isNoAi());
        session.approachPrepared(true);
        pet.noPhysics = !session.flying();
        pet.setNoGravity(session.flying() || session.originalNoGravity());
        if (pet instanceof Mob mob) {
            mob.getNavigation().stop();
            Goal movementLease = createMovementLease(session);
            ((MobGoalSelectorAccessor) mob).tdmc$getGoalSelector().addGoal(0, movementLease);
            session.movementLease(movementLease);
        }
    }

    public static void tick(ServerPlayer player, LivingEntity pet, PetSummonSession session) {
        if (player == null || pet == null || session == null) return;
        if (player.getVehicle() == pet) {
            session.state(PetSummonSession.State.COMPLETE);
            return;
        }

        beginApproach(pet, session);
        prepareArrivalFrame(pet, session);
        Vec3 target = target(player, pet, session);
        double completionRadius = completionRadius(pet);
        if (isCatchable(player, pet, session, completionRadius)) {
            tryComplete(player, pet, session);
            return;
        }

        double distance = target.distanceTo(pet.position());
        double cruiseSpeed = PetApproachMath.cruiseSpeed(pet.getBbWidth(), pet.getBbHeight(), session.flying());
        double step = PetApproachMath.stepDistance(distance, completionRadius, cruiseSpeed, session.approachTicks());
        if (step > 1.0E-4D) {
            if (session.flying() || session.airborneRescue()) {
                moveFlyingStep(pet, target, step);
            } else {
                moveGroundStep(player, pet, target, step);
            }
        }
        session.incrementApproachTicks();

        if (session.airborneRescue() && !player.onGround()) {
            forceEmergencyInterceptIfNeeded(player, pet, session);
        }

        if (isCatchable(player, pet, session, completionRadius)) {
            tryComplete(player, pet, session);
        } else {
            session.state(PetSummonSession.State.APPROACHING);
        }
    }

    public static boolean timedOut(PetSummonSession session) {
        return session != null && session.approachTicks() >= MAX_AGE_TICKS;
    }

    public static void finishApproach(LivingEntity pet, PetSummonSession session) {
        if (pet == null || session == null || !session.approachPrepared()) return;
        pet.noPhysics = session.originalNoPhysics();
        pet.setNoGravity(session.originalNoGravity());
        pet.setDeltaMovement(pet.getDeltaMovement().scale(0.35D));
        pet.fallDistance = 0.0F;
        if (pet instanceof Mob mob) {
            Goal movementLease = session.movementLease();
            if (movementLease != null) {
                movementLease.stop();
                ((MobGoalSelectorAccessor) mob).tdmc$getGoalSelector().removeGoal(movementLease);
                session.movementLease(null);
            }
            mob.getNavigation().stop();
            if (mob.isNoAi() != session.originalNoAi()) {
                mob.setNoAi(session.originalNoAi());
            }
        }
        session.approachPrepared(false);
    }

    static double completionRadius(LivingEntity pet) {
        return PetApproachMath.completionRadius(pet == null ? 0.0D : pet.getBbWidth());
    }

    private static Goal createMovementLease(PetSummonSession session) {
        return new Goal() {
            {
                setFlags(EnumSet.of(Flag.MOVE));
            }

            @Override
            public boolean canUse() {
                return session.approachPrepared()
                        && session.state() != PetSummonSession.State.COMPLETE
                        && session.state() != PetSummonSession.State.CANCELLED;
            }

            @Override
            public boolean canContinueToUse() {
                return canUse();
            }
        };
    }

    private static void prepareArrivalFrame(LivingEntity pet, PetSummonSession session) {
        pet.noPhysics = !session.flying();
        pet.setNoGravity(session.flying() || session.originalNoGravity());
        pet.fallDistance = 0.0F;
        if (pet instanceof Mob mob) {
            mob.getNavigation().stop();
        }
    }

    private static void tryComplete(ServerPlayer player, LivingEntity pet, PetSummonSession session) {
        session.state(PetSummonSession.State.CATCHING);
        finishApproach(pet, session);
        if (RideService.trySummonMount(player, pet)) {
            session.state(PetSummonSession.State.COMPLETE);
        } else {
            session.state(PetSummonSession.State.APPROACHING);
            beginApproach(pet, session);
        }
    }

    private static void moveFlyingStep(LivingEntity pet, Vec3 target, double step) {
        Vec3 delta = target.subtract(pet.position());
        if (delta.lengthSqr() <= 1.0E-8D) return;
        Vec3 direction = delta.normalize();
        Vec3 next = pet.position().add(direction.scale(step));
        faceDirection(pet, direction);
        float pitch = (float) (-(Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)) * 180.0D / Math.PI));
        pet.moveTo(next.x, next.y, next.z, pet.getYRot(), pitch);
        pet.setXRot(pitch);
        pet.setDeltaMovement(direction.scale(step * 0.45D));
        pet.fallDistance = 0.0F;
        pet.hasImpulse = true;
    }

    private static void moveGroundStep(ServerPlayer player, LivingEntity pet, Vec3 target, double step) {
        Vec3 delta = target.subtract(pet.position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        if (horizontal.lengthSqr() <= 1.0E-8D) return;
        Vec3 direction = horizontal.normalize();
        double nextX = pet.getX() + direction.x * step;
        double nextZ = pet.getZ() + direction.z * step;
        double nextY = PetPlacementFinder.findContinuousGroundY(player.serverLevel(), pet, nextX, pet.getY(), nextZ);
        if (!Double.isFinite(nextY)) return;
        faceDirection(pet, direction);
        pet.moveTo(nextX, nextY, nextZ, pet.getYRot(), pet.getXRot());
        pet.setDeltaMovement(direction.scale(step * 0.55D));
        pet.fallDistance = 0.0F;
        pet.hasImpulse = true;
    }

    private static void faceDirection(LivingEntity pet, Vec3 direction) {
        if (direction.lengthSqr() <= 1.0E-8D) return;
        float yaw = (float) (Math.atan2(direction.z, direction.x) * 180.0D / Math.PI) - 90.0F;
        pet.setYRot(yaw);
        pet.setYHeadRot(yaw);
    }

    private static void forceEmergencyInterceptIfNeeded(ServerPlayer player, LivingEntity pet, PetSummonSession session) {
        if (isCatchable(player, pet, session, completionRadius(pet))) return;
        long now = player.serverLevel().getGameTime();
        if (now - session.lastEmergencyRepositionGameTime() < 4L) return;
        Vec3 intercept = PetPlacementFinder.findEmergencyIntercept(player, pet);
        if (intercept == null) return;
        pet.teleportTo(intercept.x, intercept.y, intercept.z);
        pet.setDeltaMovement(Vec3.ZERO);
        pet.fallDistance = 0.0F;
        session.lastEmergencyRepositionGameTime(now);
        PetRescueFallProtection.protect(player, pet);
    }

    private static boolean isCatchable(ServerPlayer player, LivingEntity pet, PetSummonSession session, double completionRadius) {
        double horizontalGap = Math.sqrt(horizontalGapSqr(player.getBoundingBox(), pet.getBoundingBox()));
        double verticalGap = verticalGap(player.getBoundingBox(), pet.getBoundingBox());
        if (session.airborneRescue()) return horizontalGap <= Math.max(4.5D, completionRadius) && verticalGap <= 5.0D;
        return horizontalGap <= completionRadius && verticalGap <= Math.max(3.0D, completionRadius);
    }

    private static double horizontalGapSqr(AABB a, AABB b) {
        double dx = axisGap(a.minX, a.maxX, b.minX, b.maxX);
        double dz = axisGap(a.minZ, a.maxZ, b.minZ, b.maxZ);
        return dx * dx + dz * dz;
    }

    private static double verticalGap(AABB a, AABB b) {
        return axisGap(a.minY, a.maxY, b.minY, b.maxY);
    }

    private static double axisGap(double aMin, double aMax, double bMin, double bMax) {
        if (aMax < bMin) return bMin - aMax;
        if (bMax < aMin) return aMin - bMax;
        return 0.0D;
    }

    private static Vec3 target(ServerPlayer player, LivingEntity pet, PetSummonSession session) {
        if (session.airborneRescue() && !player.onGround()) {
            Vec3 intercept = PetPlacementFinder.findEmergencyIntercept(player, pet);
            if (intercept != null) return intercept;
        }
        if (session.flying()) return player.position().add(0.0D, 0.75D, 0.0D);
        return player.position();
    }
}
