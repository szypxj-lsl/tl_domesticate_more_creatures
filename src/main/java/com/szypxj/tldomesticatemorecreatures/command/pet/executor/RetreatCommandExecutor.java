package com.szypxj.tldomesticatemorecreatures.command.pet.executor;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandData;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandRuntimeState;
import com.szypxj.tldomesticatemorecreatures.command.pet.capability.PetCapabilityProfile;
import com.szypxj.tldomesticatemorecreatures.command.pet.capability.PetCapabilityResolver;
import com.szypxj.tldomesticatemorecreatures.command.pet.capability.StandardPetCapabilities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public final class RetreatCommandExecutor implements PetCommandExecutor {
    @Override
    public boolean supports(Mob mob) {
        return !PetCapabilityResolver.resolve(mob).retreat().isEmpty();
    }

    @Override
    public boolean start(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
        LivingEntity owner = StandardPetCapabilities.resolveCommandOwner(mob, data);
        if (owner == null) {
            return false;
        }
        data.updatePosition(owner.position());
        PetCapabilityProfile profile = PetCapabilityResolver.resolve(mob);
        return PetCommandExecutors.startFirst(mob, data, runtime, profile.retreat(), 0);
    }

    @Override
    public boolean tick(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
        LivingEntity owner = StandardPetCapabilities.resolveCommandOwner(mob, data);
        if (owner == null || mob.distanceToSqr(owner) <= 16.0D) {
            return false;
        }
        data.updatePosition(owner.position());
        PetCapabilityProfile profile = PetCapabilityResolver.resolve(mob);
        return PetCommandExecutors.tickActiveOrFallback(mob, data, runtime, profile.retreat());
    }

    @Override
    public void cancel(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
        PetCommandExecutors.cancelAndRestore(mob, data, runtime);
    }
}
