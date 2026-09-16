package com.szypxj.tldomesticatemorecreatures.command.pet.executor;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandData;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandRuntimeState;
import com.szypxj.tldomesticatemorecreatures.command.pet.capability.PetCapabilityProfile;
import com.szypxj.tldomesticatemorecreatures.command.pet.capability.PetCapabilityResolver;
import net.minecraft.world.entity.Mob;

public final class MoveCommandExecutor implements PetCommandExecutor {
    @Override
    public boolean supports(Mob mob) {
        return !PetCapabilityResolver.resolve(mob).move().isEmpty();
    }

    @Override
    public boolean start(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
        PetCapabilityProfile profile = PetCapabilityResolver.resolve(mob);
        return PetCommandExecutors.startFirst(mob, data, runtime, profile.move(), 0);
    }

    @Override
    public boolean tick(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
        if (mob.distanceToSqr(data.position()) <= 4.0D) {
            return false;
        }
        PetCapabilityProfile profile = PetCapabilityResolver.resolve(mob);
        return PetCommandExecutors.tickActiveOrFallback(mob, data, runtime, profile.move());
    }

    @Override
    public void cancel(Mob mob, PetCommandData data, PetCommandRuntimeState runtime) {
        PetCommandExecutors.cancelAndRestore(mob, data, runtime);
    }
}
