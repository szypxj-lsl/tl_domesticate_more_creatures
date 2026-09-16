package com.szypxj.tldomesticatemorecreatures.command.pet.capability;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommand;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandData;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandRuntimeState;
import com.szypxj.tldomesticatemorecreatures.command.pet.provider.PetCommandCompatibilityApi;
import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.domestication.FriendlyFireService;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public final class StandardPetCapabilities {
    private static final long COMBAT_INTENT_RETRY_TICKS = 20L;
    private static final PetCommandCapability COMBAT_INTENT = new CombatIntentCapability();
    private static final PetCommandCapability BRAIN_ATTACK_TARGET = new BrainAttackTargetCapability();
    private static final PetCommandCapability BRAIN_MOVEMENT = new BrainMovementCapability();
    private static final PetCommandCapability NAVIGATION_MOVEMENT = new NavigationMovementCapability();
    private static final PetCommandCapability TAMABLE_FOLLOW = new TamableFollowCapability();
    private static final PetCommandCapability POSITION_FOLLOW = new PositionFollowCapability();
    private static final PetCommandCapability FLYING_PATH_LANDING = new FlyingPathLandingCapability();

    private StandardPetCapabilities() {
    }

    public static PetCommandCapability combatIntent() {
        return COMBAT_INTENT;
    }

    public static PetCommandCapability brainAttackTarget() {
        return BRAIN_ATTACK_TARGET;
    }

    public static PetCommandCapability brainMovement() {
        return BRAIN_MOVEMENT;
    }

    public static PetCommandCapability navigationMovement() {
        return NAVIGATION_MOVEMENT;
    }

    public static PetCommandCapability tamableFollow() {
        return TAMABLE_FOLLOW;
    }

    public static PetCommandCapability nativeStateFollow(PetCommandCompatibilityApi.NativeStateProvider provider) {
        return new NativeStateFollowCapability(provider);
    }

    public static PetCommandCapability positionFollow() {
        return POSITION_FOLLOW;
    }

    public static PetCommandCapability flyingPathLanding() {
        return FLYING_PATH_LANDING;
    }

    public static PetCommandCapability safeLandingViaMovement(
            PetCommandCapability movement,
            List<PetCommandCompatibilityApi.FlightProvider> flightProviders
    ) {
        return new MovementBackedLandingCapability(movement, flightProviders);
    }

    public static boolean isMemoryRegistered(Mob mob, MemoryModuleType<?> type) {
        return mob.getBrain().checkMemory(type, MemoryStatus.REGISTERED);
    }

    public static boolean supportsBrainMovement(Mob mob) {
        return isMemoryRegistered(mob, MemoryModuleType.WALK_TARGET)
                && isMemoryRegistered(mob, MemoryModuleType.LOOK_TARGET);
    }

    public static LivingEntity registeredAttackTarget(Mob mob) {
        if (!isMemoryRegistered(mob, MemoryModuleType.ATTACK_TARGET)) {
            return null;
        }
        return mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
    }

    public static void eraseRegisteredMemory(Mob mob, MemoryModuleType<?> type) {
        if (isMemoryRegistered(mob, type)) {
            mob.getBrain().eraseMemory(type);
        }
    }

    public static void eraseBrainMovement(Mob mob) {
        eraseRegisteredMemory(mob, MemoryModuleType.WALK_TARGET);
        eraseRegisteredMemory(mob, MemoryModuleType.LOOK_TARGET);
    }

    public static LivingEntity resolveTarget(Mob mob, UUID uuid) {
        if (uuid == null || !(mob.level() instanceof ServerLevel level)) {
            return null;
        }
        Entity entity = level.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    public static LivingEntity validOwner(Mob mob) {
        LivingEntity owner = PetOwnershipService.ownerPlayer(mob).orElse(null);
        return owner != null && owner.isAlive() && !owner.isRemoved() && owner.level() == mob.level() ? owner : null;
    }

    public static LivingEntity resolveCommandOwner(Mob mob, PetCommandData data) {
        UUID ownerUuid = data.targetUuid().orElse(null);
        if (ownerUuid != null && mob.level() instanceof ServerLevel level) {
            Player player = level.getPlayerByUUID(ownerUuid);
            if (player != null
                    && player.isAlive()
                    && !player.isRemoved()
                    && player.level() == mob.level()
                    && PetOwnershipService.isOwnedBy(mob, player)) {
                return player;
            }
        }
        return validOwner(mob);
    }

    public static boolean validAttackTarget(Mob mob, LivingEntity target) {
        return target != null
                && target.isAlive()
                && !target.isRemoved()
                && !FriendlyFireService.areFriendly(mob, target);
    }

    public static boolean withinCommandAggroRange(Mob mob, LivingEntity target) {
        double range = Config.PET_COMMAND_RANGE.get();
        return mob.distanceToSqr(target) <= range * range;
    }

    public static boolean matchesCommandTarget(LivingEntity entity, UUID commandTargetUuid) {
        return entity != null && commandTargetUuid != null && commandTargetUuid.equals(entity.getUUID());
    }

    public static void clearTargetScopedAttackState(Mob mob, UUID commandTargetUuid) {
        boolean cleared = false;
        LivingEntity currentTarget = mob.getTarget();
        if (matchesCommandTarget(currentTarget, commandTargetUuid)) {
            mob.setTarget(null);
            cleared = true;
        }
        LivingEntity retaliationSource = mob.getLastHurtByMob();
        if (matchesCommandTarget(retaliationSource, commandTargetUuid)) {
            mob.setLastHurtByMob(null);
            cleared = true;
        }
        LivingEntity brainTarget = registeredAttackTarget(mob);
        if (matchesCommandTarget(brainTarget, commandTargetUuid)) {
            eraseRegisteredMemory(mob, MemoryModuleType.ATTACK_TARGET);
            cleared = true;
        }
        mob.getNavigation().stop();
        if (cleared && mob.getTarget() == null && registeredAttackTarget(mob) == null) {
            mob.setAggressive(false);
        }
    }

    public static Vec3 findSafeLandingPosition(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return null;
        }
        BlockPos origin = mob.blockPosition();
        Vec3 best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int radius = 0; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    Vec3 candidate = findLandingInColumn(level, mob, origin.getX() + dx, origin.getZ() + dz);
                    if (candidate == null) {
                        continue;
                    }
                    double distance = mob.position().distanceToSqr(candidate);
                    if (distance < bestDistance) {
                        best = candidate;
                        bestDistance = distance;
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    private static Vec3 findLandingInColumn(ServerLevel level, Mob mob, int x, int z) {
        int startY = Math.min(mob.blockPosition().getY(), level.getMaxBuildHeight() - 2);
        for (int y = startY; y >= level.getMinBuildHeight(); y--) {
            BlockPos floor = new BlockPos(x, y, z);
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                continue;
            }
            Vec3 landing = new Vec3(x + 0.5D, y + 1.0D, z + 0.5D);
            if (isSafeLandingPosition(mob, landing)) {
                return landing;
            }
        }
        return null;
    }

    public static boolean isSafeLandingPosition(Mob mob, Vec3 landing) {
        if (!(mob.level() instanceof ServerLevel level) || landing == null) {
            return false;
        }
        BlockPos feet = BlockPos.containing(landing);
        BlockPos floor = feet.below();
        if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
            return false;
        }
        if (!level.getFluidState(feet).isEmpty()) {
            return false;
        }
        AABB moved = mob.getBoundingBox().move(
                landing.x - mob.getX(),
                landing.y - mob.getY(),
                landing.z - mob.getZ()
        );
        return level.noCollision(mob, moved);
    }

    private static void setBrainWalkTarget(Mob mob, Vec3 position, float speed, int closeEnough) {
        BehaviorUtils.setWalkAndLookTargetMemories(mob, BlockPos.containing(position), speed, closeEnough);
    }

    private static final class CombatIntentCapability implements PetCommandCapability {
        @Override
        public String id() {
            return "standard:combat_intent";
        }

        @Override
        public boolean start(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            boolean alreadyPrimedThisTick = data.lastCombatIntentPulse() == mob.level().getGameTime();
            return apply(mob, data, !alreadyPrimedThisTick);
        }

        @Override
        public TickResult tick(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            return apply(mob, data, false) ? TickResult.ACTIVE : TickResult.COMPLETE;
        }

        @Override
        public void cancel(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            clearTargetScopedAttackState(mob, data.targetUuid().orElse(null));
        }

        private boolean apply(Mob mob, PetCommandData data, boolean forcePulse) {
            LivingEntity target = resolveTarget(mob, data.targetUuid().orElse(null));
            if (!validAttackTarget(mob, target) || !withinCommandAggroRange(mob, target)) {
                clearTargetScopedAttackState(mob, data.targetUuid().orElse(null));
                return false;
            }
            long gameTime = mob.level().getGameTime();
            long lastPulse = data.lastCombatIntentPulse();
            boolean cooldownElapsed = lastPulse == Long.MIN_VALUE
                    || gameTime < lastPulse
                    || gameTime - lastPulse >= COMBAT_INTENT_RETRY_TICKS;
            if (forcePulse || mob.getTarget() != target && cooldownElapsed) {
                mob.setLastHurtByMob(target);
                data.markCombatIntentPulse(gameTime);
            }
            mob.setTarget(target);
            if (isMemoryRegistered(mob, MemoryModuleType.ATTACK_TARGET)) {
                mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
            }
            return true;
        }
    }

    private static final class BrainAttackTargetCapability implements PetCommandCapability {
        @Override
        public String id() {
            return "standard:brain_attack_target";
        }

        @Override
        public boolean start(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (!isMemoryRegistered(mob, MemoryModuleType.ATTACK_TARGET)) {
                return false;
            }
            LivingEntity target = resolveTarget(mob, data.targetUuid().orElse(null));
            if (!validAttackTarget(mob, target)) {
                return false;
            }
            mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
            return true;
        }

        @Override
        public TickResult tick(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (!isMemoryRegistered(mob, MemoryModuleType.ATTACK_TARGET)) {
                return TickResult.FAILED;
            }
            LivingEntity target = resolveTarget(mob, data.targetUuid().orElse(null));
            if (!validAttackTarget(mob, target) || !withinCommandAggroRange(mob, target)) {
                return TickResult.COMPLETE;
            }
            LivingEntity current = registeredAttackTarget(mob);
            if (current != target) {
                mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
            }
            return TickResult.ACTIVE;
        }

        @Override
        public void cancel(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            UUID targetUuid = data.targetUuid().orElse(null);
            if (matchesCommandTarget(registeredAttackTarget(mob), targetUuid)) {
                eraseRegisteredMemory(mob, MemoryModuleType.ATTACK_TARGET);
            }
        }
    }

    private static final class BrainMovementCapability implements PetCommandCapability {
        @Override
        public String id() {
            return "standard:brain_movement";
        }

        @Override
        public boolean start(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (!supportsBrainMovement(mob)) {
                return false;
            }
            setBrainWalkTarget(mob, data.position(), 1.0F, 2);
            runtime.setLastMovementTarget(data.position());
            return true;
        }

        @Override
        public TickResult tick(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (!supportsBrainMovement(mob)) {
                return TickResult.FAILED;
            }
            if (data.command() == PetCommand.MOVE && mob.distanceToSqr(data.position()) <= 4.0D) {
                eraseBrainMovement(mob);
                return TickResult.COMPLETE;
            }
            Vec3 lastTarget = runtime.lastMovementTarget().orElse(null);
            if (lastTarget == null
                    || lastTarget.distanceToSqr(data.position()) > 4.0D
                    || mob.getBrain().getMemory(MemoryModuleType.WALK_TARGET).isEmpty()) {
                setBrainWalkTarget(mob, data.position(), 1.0F, 2);
                runtime.setLastMovementTarget(data.position());
            }
            return TickResult.ACTIVE;
        }

        @Override
        public void cancel(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            eraseBrainMovement(mob);
        }
    }

    private static class NavigationMovementCapability implements PetCommandCapability {
        @Override
        public String id() {
            return "standard:navigation_movement";
        }

        @Override
        public boolean start(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            Vec3 target = data.position();
            boolean started = mob.getNavigation().moveTo(target.x, target.y, target.z, 1.2D);
            if (started) {
                runtime.setLastMovementTarget(target);
            }
            return started;
        }

        @Override
        public TickResult tick(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (data.command() == PetCommand.MOVE && mob.distanceToSqr(data.position()) <= 4.0D) {
                mob.getNavigation().stop();
                return TickResult.COMPLETE;
            }
            Vec3 target = data.position();
            Vec3 lastTarget = runtime.lastMovementTarget().orElse(null);
            if (mob.getNavigation().isDone() || lastTarget == null || lastTarget.distanceToSqr(target) > 4.0D) {
                if (!mob.getNavigation().moveTo(target.x, target.y, target.z, 1.2D)) {
                    return TickResult.FAILED;
                }
                runtime.setLastMovementTarget(target);
            }
            return TickResult.ACTIVE;
        }

        @Override
        public void cancel(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            mob.getNavigation().stop();
        }
    }

    private static final class PositionFollowCapability extends NavigationMovementCapability {
        @Override
        public String id() {
            return "standard:position_follow";
        }
    }

    private static final class NativeStateFollowCapability implements PetCommandCompatibilityApi.FollowProvider {
        private final PetCommandCompatibilityApi.NativeStateProvider provider;

        private NativeStateFollowCapability(PetCommandCompatibilityApi.NativeStateProvider provider) {
            this.provider = provider;
        }

        @Override
        public String id() {
            return "native_state:follow:" + provider.id();
        }

        @Override
        public boolean supports(Mob mob) {
            return provider.supports(mob, PetCommand.FOLLOW);
        }

        @Override
        public boolean start(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (!supports(mob) || !provider.enter(mob, PetCommand.FOLLOW, runtime)) {
                return false;
            }
            runtime.markNativeStateEntered(provider.id());
            runtime.markNativeStateSelfManaged(provider.id());
            return true;
        }

        @Override
        public TickResult tick(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (!runtime.nativeStateEntered(provider.id())) {
                return TickResult.FAILED;
            }
            return provider.maintain(mob, PetCommand.FOLLOW, runtime)
                    ? TickResult.ACTIVE
                    : TickResult.FAILED;
        }

        @Override
        public void cancel(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (!runtime.nativeStateEntered(provider.id())) {
                return;
            }
            provider.restore(mob, PetCommand.FOLLOW, runtime);
            runtime.unmarkNativeStateEntered(provider.id());
        }
    }

    private static final class TamableFollowCapability implements PetCommandCapability {
        @Override
        public String id() {
            return "standard:tamable_follow";
        }

        @Override
        public boolean start(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (!(mob instanceof TamableAnimal tamable) || resolveCommandOwner(mob, data) == null) {
                return false;
            }
            runtime.rememberOrderedToSit(tamable.isOrderedToSit());
            tamable.setOrderedToSit(false);
            mob.getNavigation().stop();
            return true;
        }

        @Override
        public TickResult tick(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (!(mob instanceof TamableAnimal tamable) || resolveCommandOwner(mob, data) == null) {
                return TickResult.COMPLETE;
            }
            if (tamable.isOrderedToSit()) {
                tamable.setOrderedToSit(false);
            }
            return TickResult.ACTIVE;
        }

        @Override
        public void cancel(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (mob instanceof TamableAnimal tamable) {
                runtime.previousOrderedToSit().ifPresent(tamable::setOrderedToSit);
            }
        }
    }

    private static final class MovementBackedLandingCapability implements PetCommandCapability {
        private final PetCommandCapability movement;
        private final List<PetCommandCompatibilityApi.FlightProvider> flightProviders;

        private MovementBackedLandingCapability(
                PetCommandCapability movement,
                List<PetCommandCompatibilityApi.FlightProvider> flightProviders
        ) {
            this.movement = movement;
            this.flightProviders = List.copyOf(flightProviders);
        }

        @Override
        public String id() {
            return "standard:native_movement_landing";
        }

        @Override
        public boolean start(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (mob.onGround() || mob.isInWaterOrBubble()) {
                return true;
            }
            if (!isAirborne(mob)) {
                return false;
            }
            Vec3 landing = findSafeLandingPosition(mob);
            if (landing == null) {
                return false;
            }
            runtime.setLandingTarget(landing);
            data.updatePosition(landing);
            return movement.start(mob, data, runtime);
        }

        @Override
        public TickResult tick(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (mob.onGround() || mob.isInWaterOrBubble() || !isAirborne(mob)) {
                movement.cancel(mob, data, runtime);
                return TickResult.COMPLETE;
            }

            Vec3 landing = runtime.landingTarget().orElse(null);
            if (!isSafeLandingPosition(mob, landing)) {
                landing = findSafeLandingPosition(mob);
                if (landing == null) {
                    return TickResult.FAILED;
                }
                movement.cancel(mob, data, runtime);
                runtime.setLandingTarget(landing);
                data.updatePosition(landing);
                if (!movement.start(mob, data, runtime)) {
                    return TickResult.FAILED;
                }
            }

            TickResult result = movement.tick(mob, data, runtime);
            return result == TickResult.FAILED ? TickResult.FAILED : TickResult.ACTIVE;
        }

        @Override
        public void cancel(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            movement.cancel(mob, data, runtime);
        }

        private boolean isAirborne(Mob mob) {
            for (PetCommandCompatibilityApi.FlightProvider provider : flightProviders) {
                if (provider.isAirborne(mob)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class FlyingPathLandingCapability implements PetCommandCapability {
        @Override
        public String id() {
            return "standard:flying_path_landing";
        }

        @Override
        public boolean start(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (!(mob.getNavigation() instanceof FlyingPathNavigation)) {
                return false;
            }
            if (mob.onGround() || mob.isInWaterOrBubble()) {
                return true;
            }
            Vec3 landing = findSafeLandingPosition(mob);
            if (landing == null) {
                return false;
            }
            runtime.setLandingTarget(landing);
            data.updatePosition(landing);
            return mob.getNavigation().moveTo(landing.x, landing.y, landing.z, 1.0D);
        }

        @Override
        public TickResult tick(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            if (mob.onGround() || mob.isInWaterOrBubble()) {
                mob.getNavigation().stop();
                return TickResult.COMPLETE;
            }
            Vec3 landing = runtime.landingTarget().orElse(null);
            if (!isSafeLandingPosition(mob, landing)) {
                landing = findSafeLandingPosition(mob);
                if (landing == null) {
                    return TickResult.FAILED;
                }
                runtime.setLandingTarget(landing);
                data.updatePosition(landing);
            }
            if (mob.getNavigation().isDone()
                    && !mob.getNavigation().moveTo(landing.x, landing.y, landing.z, 1.0D)) {
                return TickResult.FAILED;
            }
            return TickResult.ACTIVE;
        }

        @Override
        public void cancel(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
            mob.getNavigation().stop();
            runtime.setLandingTarget(null);
        }
    }
}
