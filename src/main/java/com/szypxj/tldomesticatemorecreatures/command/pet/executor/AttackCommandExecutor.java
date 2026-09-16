package com.szypxj.tldomesticatemorecreatures.command.pet.executor;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandData;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandRuntimeState;
import com.szypxj.tldomesticatemorecreatures.command.pet.capability.PetCapabilityProfile;
import com.szypxj.tldomesticatemorecreatures.command.pet.capability.PetCapabilityResolver;
import com.szypxj.tldomesticatemorecreatures.command.pet.capability.StandardPetCapabilities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public final class AttackCommandExecutor implements PetCommandExecutor {
    @Override
    public boolean supports(Mob mob) {
        return !PetCapabilityResolver.resolve(mob).attack().isEmpty();
    }

    @Override
    public boolean start(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
        LivingEntity target = resolveValidTarget(mob, data);
        if (target == null) {
            return false;
        }
        PetCapabilityProfile profile = PetCapabilityResolver.resolve(mob);
        if (!profile.attack().isEmpty()
                && profile.attack().get(0) == StandardPetCapabilities.combatIntent()) {
            primeGenericCombatIntent(mob, target, data);
        }
        return PetCommandExecutors.startFirst(mob, data, runtime, profile.attack(), 0);
    }

    @Override
    public boolean tick(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
        if (resolveValidTarget(mob, data) == null) {
            return false;
        }
        PetCapabilityProfile profile = PetCapabilityResolver.resolve(mob);
        return PetCommandExecutors.tickActiveOrFallback(mob, data, runtime, profile.attack());
    }

    @Override
    public void cancel(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
        StandardPetCapabilities.clearTargetScopedAttackState(mob, data.targetUuid().orElse(null));
        PetCommandExecutors.cancelAndRestore(mob, data, runtime);
    }

    private static LivingEntity resolveValidTarget(Mob mob, PetCommandData data) {
        LivingEntity target = StandardPetCapabilities.resolveTarget(mob, data.targetUuid().orElse(null));
        if (!StandardPetCapabilities.validAttackTarget(mob, target)
                || !StandardPetCapabilities.withinCommandAggroRange(mob, target)) {
            return null;
        }
        return target;
    }

    private static void primeGenericCombatIntent(Mob mob, LivingEntity target, PetCommandData data) {
        mob.setLastHurtByMob(target);
        data.markCombatIntentPulse(mob.level().getGameTime());
        mob.setTarget(target);
        if (StandardPetCapabilities.isMemoryRegistered(mob, MemoryModuleType.ATTACK_TARGET)) {
            mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        }
    }
}
