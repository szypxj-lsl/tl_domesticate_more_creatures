package com.szypxj.tldomesticatemorecreatures.command.pet.executor;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandData;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandRuntimeState;
import com.szypxj.tldomesticatemorecreatures.command.pet.capability.PetCapabilityProfile;
import com.szypxj.tldomesticatemorecreatures.command.pet.capability.PetCapabilityResolver;
import com.szypxj.tldomesticatemorecreatures.command.pet.capability.StandardPetCapabilities;
import com.szypxj.tldomesticatemorecreatures.domestication.FriendlyFireService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class DefendCommandExecutor implements PetCommandExecutor {
    private static final double DEFEND_RADIUS = 10.0D;
    private static final String PHASE_COMBAT = "defend:combat";
    private static final String PHASE_ANCHOR = "defend:anchor";

    @Override
    public boolean supports(Mob mob) {
        PetCapabilityProfile profile = PetCapabilityResolver.resolve(mob);
        return !profile.defend().isEmpty() || !profile.move().isEmpty();
    }

    @Override
    public boolean start(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
        LivingEntity target = findDefendTarget(mob, data.position());
        if (target != null) {
            return startCombat(mob, data, runtime, target);
        }
        data.updateTargetUuid(null);
        if (mob.distanceToSqr(data.position()) > 4.0D) {
            return startAnchorMovement(mob, data, runtime);
        }
        return true;
    }

    @Override
    public boolean tick(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
        Vec3 anchor = data.position();
        LivingEntity current = StandardPetCapabilities.resolveTarget(mob, data.targetUuid().orElse(null));
        if (validDefendTarget(mob, current, anchor)) {
            if (!PHASE_COMBAT.equals(runtime.executionPhase())) {
                PetCommandExecutors.cancelActiveOnly(mob, data, runtime);
                if (!startCombat(mob, data, runtime, current)) {
                    data.updateTargetUuid(null);
                }
            } else {
                PetCapabilityProfile profile = PetCapabilityResolver.resolve(mob);
                if (!PetCommandExecutors.tickActiveOrFallback(mob, data, runtime, profile.defend())) {
                    PetCommandExecutors.cancelActiveOnly(mob, data, runtime);
                    data.updateTargetUuid(null);
                }
            }
            return true;
        }

        if (PHASE_COMBAT.equals(runtime.executionPhase())) {
            PetCommandExecutors.cancelActiveOnly(mob, data, runtime);
        }
        data.updateTargetUuid(null);

        if (mob.tickCount % 20 == 0) {
            LivingEntity replacement = findDefendTarget(mob, anchor);
            if (replacement != null) {
                startCombat(mob, data, runtime, replacement);
                return true;
            }
        }

        if (mob.distanceToSqr(anchor) > 4.0D) {
            if (!PHASE_ANCHOR.equals(runtime.executionPhase())) {
                PetCommandExecutors.cancelActiveOnly(mob, data, runtime);
                startAnchorMovement(mob, data, runtime);
            } else {
                PetCapabilityProfile profile = PetCapabilityResolver.resolve(mob);
                if (!PetCommandExecutors.tickActiveOrFallback(mob, data, runtime, profile.move())) {
                    PetCommandExecutors.cancelActiveOnly(mob, data, runtime);
                }
            }
        } else if (PHASE_ANCHOR.equals(runtime.executionPhase())) {
            PetCommandExecutors.cancelActiveOnly(mob, data, runtime);
        }
        return true;
    }

    @Override
    public void cancel(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
        StandardPetCapabilities.clearTargetScopedAttackState(mob, data.targetUuid().orElse(null));
        PetCommandExecutors.cancelAndRestore(mob, data, runtime);
    }

    private static boolean startCombat(Mob mob, PetCommandData data, PetCommandRuntimeState runtime, LivingEntity target) {
        data.updateTargetUuid(target.getUUID());
        PetCapabilityProfile profile = PetCapabilityResolver.resolve(mob);
        if (PetCommandExecutors.startFirst(mob, data, runtime, profile.defend(), 0)) {
            runtime.setExecutionPhase(PHASE_COMBAT);
            return true;
        }
        return false;
    }

    private static boolean startAnchorMovement(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
        PetCapabilityProfile profile = PetCapabilityResolver.resolve(mob);
        if (PetCommandExecutors.startFirst(mob, data, runtime, profile.move(), 0)) {
            runtime.setExecutionPhase(PHASE_ANCHOR);
            return true;
        }
        return false;
    }

    private static LivingEntity findDefendTarget(Mob mob, Vec3 anchor) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return null;
        }
        List<LivingEntity> enemies = level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(anchor, anchor).inflate(DEFEND_RADIUS),
                candidate -> candidate instanceof Enemy
                        && candidate.isAlive()
                        && !FriendlyFireService.areFriendly(mob, candidate)
        );
        return enemies.stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(mob), b.distanceToSqr(mob)))
                .orElse(null);
    }

    private static boolean validDefendTarget(Mob mob, LivingEntity target, Vec3 anchor) {
        return StandardPetCapabilities.validAttackTarget(mob, target)
                && target.distanceToSqr(anchor) <= DEFEND_RADIUS * DEFEND_RADIUS;
    }
}
