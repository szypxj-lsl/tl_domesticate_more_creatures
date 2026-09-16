package com.szypxj.tldomesticatemorecreatures.talent.active;

import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CActiveTalentVisualPacket;
import com.szypxj.tldomesticatemorecreatures.riding.RideCapabilityResolver;
import com.szypxj.tldomesticatemorecreatures.riding.capability.RideCapabilityProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingConfigManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ShadowstepService {
    private static final double SHADOWSTEP_VERTICAL_TOLERANCE = 8.0D;
    private static final int MAX_BLOCKED_DASH_TICKS = 3;

    private ShadowstepService() {
    }

    public static void start(ServerPlayer rider, LivingEntity mount, ActiveTalentConfig.ShadowstepConfig config) {
        if (rider == null || mount == null || config == null) return;
        long now = mount.level().getGameTime();
        ActiveTalentRuntimeState state = new ActiveTalentRuntimeState(
                rider.getUUID(), mount.getUUID(), mount.getId(), ActiveTalentIds.SHADOWSTEP,
                ActiveTalentPhase.MARKING, now + config.markingDurationTicks(), config, null
        );
        ActiveTalentService.install(mount, state);
        NetworkHandler.sendActiveTalentVisual(mount, new S2CActiveTalentVisualPacket(
                mount.getId(), -1, S2CActiveTalentVisualPacket.VisualType.SHADOWSTEP_START, 1.0F
        ));
        refreshSlowField(mount, state);
        ActiveTalentService.sync(rider, mount, state);
    }

    public static void tryAddMark(ServerPlayer rider, LivingEntity mount, ActiveTalentRuntimeState state) {
        if (rider == null || mount == null || state == null || state.phase() != ActiveTalentPhase.MARKING) return;
        LivingEntity target = raycastTarget(rider, mount, state.shadowstepConfig().markRange());
        if (target == null) return;
        state.marks().add(new ShadowstepMark(target.getUUID(), target.getId()));
        NetworkHandler.sendActiveTalentVisual(mount, new S2CActiveTalentVisualPacket(
                mount.getId(), target.getId(), S2CActiveTalentVisualPacket.VisualType.SHADOWSTEP_MARK, 1.0F
        ));
        if (state.marks().size() >= state.shadowstepConfig().maxMarks()) beginExecution(rider, mount, state);
        else ActiveTalentService.sync(rider, mount, state);
    }

    public static void tick(ServerPlayer rider, LivingEntity mount, ActiveTalentRuntimeState state) {
        if (!(mount.level() instanceof ServerLevel)) return;
        if (state.phase() == ActiveTalentPhase.MARKING) {
            refreshSlowField(mount, state);
            if (mount.level().getGameTime() >= state.phaseEndGameTime()) beginExecution(rider, mount, state);
            return;
        }
        if (state.phase() == ActiveTalentPhase.EXECUTING) tickExecution(rider, mount, state);
    }

    public static int shadowstepCooldownTicks(ActiveTalentConfig.ShadowstepConfig config, int successfulAttacks) {
        if (config == null) return 0;
        int success = Math.max(0, Math.min(config.maxMarks(), successfulAttacks));
        double factor = 1.0D - (config.maxMarks() - success) * config.refundPerUnusedMark();
        return Math.max(0, (int) Math.round(config.cooldownTicks() * Math.max(0.0D, factor)));
    }

    static void cleanupSlow(LivingEntity mount, ActiveTalentRuntimeState state) {
        if (state == null || state.slowedMobs().isEmpty()) return;
        if (mount != null && mount.level() instanceof ServerLevel level) {
            for (UUID uuid : new ArrayList<>(state.slowedMobs())) {
                Entity entity = level.getEntity(uuid);
                if (entity instanceof Mob mob) ShadowstepSlowRegistry.remove(mob, state.mountUuid());
            }
        }
        state.slowedMobs().clear();
    }

    private static void beginExecution(ServerPlayer rider, LivingEntity mount, ActiveTalentRuntimeState state) {
        if (state.phase() != ActiveTalentPhase.MARKING) return;
        cleanupSlow(mount, state);
        NetworkHandler.sendActiveTalentVisual(mount, new S2CActiveTalentVisualPacket(
                mount.getId(), -1, S2CActiveTalentVisualPacket.VisualType.SHADOWSTEP_CLEAR, 1.0F
        ));
        state.phase(ActiveTalentPhase.EXECUTING);
        state.phaseEndGameTime(0L);
        state.blockedDashTicks(0);
        mount.setDeltaMovement(Vec3.ZERO);
        mount.fallDistance = 0.0F;
        ActiveTalentService.sync(rider, mount, state);
        if (state.marks().isEmpty()) finish(rider, mount, state);
    }

    private static void tickExecution(ServerPlayer rider, LivingEntity mount, ActiveTalentRuntimeState state) {
        if (!(mount instanceof Mob mob) || !(mount.level() instanceof ServerLevel level)) {
            finish(rider, mount, state);
            return;
        }
        mount.setDeltaMovement(Vec3.ZERO);
        mount.fallDistance = 0.0F;
        if (state.markCursor() >= state.marks().size()) {
            finish(rider, mount, state);
            return;
        }
        ShadowstepMark mark = state.marks().get(state.markCursor());
        Entity entity = level.getEntity(mark.targetUuid());
        if (!(entity instanceof LivingEntity target)
                || !ActiveTalentService.isAttackableTarget(mount, target)
                || mount.distanceToSqr(target) > state.shadowstepConfig().markRange() * state.shadowstepConfig().markRange()
                || !movementModeCompatible(mob, target)) {
            advance(state);
            return;
        }

        double stopDistance = Math.max(1.0D, (mount.getBbWidth() + target.getBbWidth()) * 0.65D);
        Vec3 delta = target.position().subtract(mount.position());
        double distance = delta.length();
        if (distance <= stopDistance) {
            tryStrikeCurrentTarget(mob, target, state);
            return;
        }

        double maxMove = Math.min(state.shadowstepConfig().dashBlocksPerTick(), Math.max(0.0D, distance - stopDistance));
        if (maxMove <= 0.0D) {
            tryStrikeCurrentTarget(mob, target, state);
            return;
        }

        Vec3 dashStart = mount.position().add(0.0D, mount.getBbHeight() * 0.45D, 0.0D);
        Vec3 move = delta.normalize().scale(maxMove);
        boolean moved = dashCollisionSafe(level, mob, move);
        if (!moved && target.getY() > mount.getY() + 0.25D) {
            double verticalMove = Math.min(maxMove, target.getY() - mount.getY());
            moved = dashCollisionSafe(level, mob, new Vec3(0.0D, verticalMove, 0.0D));
        } else if (!moved && target.getY() < mount.getY() - 0.25D) {
            Vec3 horizontalDelta = new Vec3(delta.x, 0.0D, delta.z);
            if (horizontalDelta.lengthSqr() > 1.0E-6D) {
                double horizontalMove = Math.min(maxMove, horizontalDelta.length());
                moved = dashCollisionSafe(level, mob, horizontalDelta.normalize().scale(horizontalMove));
            }
        }
        if (!moved) {
            state.blockedDashTicks(state.blockedDashTicks() + 1);
            if (state.blockedDashTicks() >= MAX_BLOCKED_DASH_TICKS) advance(state);
            return;
        }

        state.blockedDashTicks(0);
        mount.setDeltaMovement(Vec3.ZERO);
        mount.fallDistance = 0.0F;
        Vec3 dashEnd = mount.position().add(0.0D, mount.getBbHeight() * 0.45D, 0.0D);
        NetworkHandler.sendActiveTalentVisual(mount, S2CActiveTalentVisualPacket.shadowstepTrail(
                mount.getId(), target.getId(), dashStart, dashEnd
        ));

        if (mount.distanceTo(target) <= stopDistance + 0.05D) {
            tryStrikeCurrentTarget(mob, target, state);
        }
    }

    private static void tryStrikeCurrentTarget(Mob mob, LivingEntity target, ActiveTalentRuntimeState state) {
        ActiveSkillAttackContext.runActiveAttack(() -> {
            mob.doHurtTarget(target);
            return true;
        });
        state.successfulAttacks(state.successfulAttacks() + 1);
        NetworkHandler.sendActiveTalentVisual(mob, new S2CActiveTalentVisualPacket(
                mob.getId(), target.getId(), S2CActiveTalentVisualPacket.VisualType.SHADOWSTEP_HIT, 1.0F
        ));
        advance(state);
    }

    private static void advance(ActiveTalentRuntimeState state) {
        state.markCursor(state.markCursor() + 1);
        state.blockedDashTicks(0);
    }

    private static void finish(ServerPlayer rider, LivingEntity mount, ActiveTalentRuntimeState state) {
        cleanupSlow(mount, state);
        mount.setDeltaMovement(Vec3.ZERO);
        mount.fallDistance = 0.0F;
        ActiveTalentService.finish(rider, mount, state, shadowstepCooldownTicks(state.shadowstepConfig(), state.successfulAttacks()));
    }

    private static LivingEntity raycastTarget(ServerPlayer rider, LivingEntity mount, double range) {
        Vec3 start = rider.getEyePosition();
        Vec3 look = rider.getViewVector(1.0F);
        Vec3 requestedEnd = start.add(look.scale(range));
        HitResult blockHit = rider.pick(range, 1.0F, false);
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? requestedEnd : blockHit.getLocation();
        AABB area = rider.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        return rider.level().getEntitiesOfClass(LivingEntity.class, area, candidate ->
                        candidate != rider && ActiveTalentService.isAttackableTarget(mount, candidate))
                .stream()
                .map(candidate -> new Candidate(candidate, candidate.getBoundingBox().inflate(candidate.getPickRadius() + 0.25D).clip(start, end)))
                .filter(candidate -> candidate.hit().isPresent())
                .filter(candidate -> rider.hasLineOfSight(candidate.entity()))
                .min(Comparator.comparingDouble(candidate -> candidate.hit().get().distanceToSqr(start)))
                .map(Candidate::entity)
                .orElse(null);
    }

    private static void refreshSlowField(LivingEntity mount, ActiveTalentRuntimeState state) {
        if (!(mount.level() instanceof ServerLevel level)) return;
        double radius = state.shadowstepConfig().slowRadius();
        Set<UUID> next = new HashSet<>();
        for (Mob mob : level.getEntitiesOfClass(Mob.class, mount.getBoundingBox().inflate(radius), candidate ->
                ActiveTalentService.isAttackableTarget(mount, candidate))) {
            if (mob.distanceToSqr(mount) > radius * radius) continue;
            next.add(mob.getUUID());
            if (!state.slowedMobs().contains(mob.getUUID())) {
                ShadowstepSlowRegistry.add(mob, state.mountUuid(), state.shadowstepConfig().enemyActionMultiplier());
            }
        }
        for (UUID uuid : new ArrayList<>(state.slowedMobs())) {
            if (next.contains(uuid)) continue;
            Entity entity = level.getEntity(uuid);
            if (entity instanceof Mob mob) ShadowstepSlowRegistry.remove(mob, state.mountUuid());
        }
        state.slowedMobs().clear();
        state.slowedMobs().addAll(next);
    }

    private static boolean movementModeCompatible(Mob mount, LivingEntity target) {
        RideCapabilityProfile profile = RideCapabilityResolver.profile(mount, RidingConfigManager.profile(mount.getType()));
        if (mount.isInWaterOrBubble() || target.isInWaterOrBubble()) return profile.swim() || profile.flight();
        double verticalDistance = Math.abs(target.getY() - mount.getY());
        if (verticalDistance <= SHADOWSTEP_VERTICAL_TOLERANCE) return profile.ground() || profile.flight();
        return profile.flight();
    }

    private static boolean dashCollisionSafe(ServerLevel level, Mob mount, Vec3 totalMove) {
        double length = totalMove.length();
        if (length <= 0.0D) return false;
        int steps = Math.max(1, (int) Math.ceil(length / 0.35D));
        Vec3 step = totalMove.scale(1.0D / steps);
        AABB startBox = mount.getBoundingBox();
        Vec3 offset = Vec3.ZERO;
        for (int i = 0; i < steps; i++) {
            offset = offset.add(step);
            AABB nextBox = startBox.move(offset);
            if (!level.noCollision(mount, nextBox)) return false;
        }
        mount.setPos(mount.getX() + totalMove.x, mount.getY() + totalMove.y, mount.getZ() + totalMove.z);
        return true;
    }

    private record Candidate(LivingEntity entity, Optional<Vec3> hit) {
    }
}
