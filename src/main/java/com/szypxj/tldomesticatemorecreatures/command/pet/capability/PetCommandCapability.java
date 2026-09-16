package com.szypxj.tldomesticatemorecreatures.command.pet.capability;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandData;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandRuntimeState;
import net.minecraft.world.entity.Mob;

public interface PetCommandCapability {
    String id();

    boolean start(Mob mob, PetCommandData data, PetCommandRuntimeState runtime);

    TickResult tick(Mob mob, PetCommandData data, PetCommandRuntimeState runtime);

    void cancel(Mob mob, PetCommandData data, PetCommandRuntimeState runtime);

    enum TickResult {
        ACTIVE,
        COMPLETE,
        FAILED
    }
}
