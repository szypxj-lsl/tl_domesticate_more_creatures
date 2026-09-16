package com.szypxj.tldomesticatemorecreatures.command.pet.executor;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandData;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandRuntimeState;
import net.minecraft.world.entity.Mob;

public interface PetCommandExecutor {
    boolean supports(Mob mob);

    boolean start(Mob mob, PetCommandData data, PetCommandRuntimeState runtime);

    boolean tick(Mob mob, PetCommandData data, PetCommandRuntimeState runtime);

    void cancel(Mob mob, PetCommandData data, PetCommandRuntimeState runtime);
}
