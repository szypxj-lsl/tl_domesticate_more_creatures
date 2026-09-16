package com.szypxj.tldomesticatemorecreatures.mixin.riding;

import com.szypxj.tldomesticatemorecreatures.riding.RideSeatPositioner;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class EntityPassengerPositionMixin {
    @Redirect(
            method = "rideTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;positionRider(Lnet/minecraft/world/entity/Entity;)V"
            ),
            require = 1
    )
    private void tdmc$positionConfiguredRider(Entity mount, Entity passenger) {
        RideSeatPositioner.positionOrVanilla(mount, passenger);
    }
}
