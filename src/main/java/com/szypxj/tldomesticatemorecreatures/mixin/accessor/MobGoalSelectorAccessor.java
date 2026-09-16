package com.szypxj.tldomesticatemorecreatures.mixin.accessor;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mob.class)
public interface MobGoalSelectorAccessor {
    @Accessor("goalSelector")
    GoalSelector tdmc$getGoalSelector();
}
