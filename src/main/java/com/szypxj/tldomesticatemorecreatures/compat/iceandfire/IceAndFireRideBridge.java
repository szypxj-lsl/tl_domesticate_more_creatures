package com.szypxj.tldomesticatemorecreatures.compat.iceandfire;

import net.minecraft.world.entity.player.Player;

/**
 * TDMC-side bridge implemented on Ice and Fire dragons by an optional @Pseudo mixin.
 * No Ice and Fire classes are referenced here so the integration remains optional.
 */
public interface IceAndFireRideBridge {
    void tdmc$setGoingUp(boolean active);

    void tdmc$setGoingDown(boolean active);

    void tdmc$setAttacking(boolean active);

    void tdmc$setStriking(boolean active);

    void tdmc$roar();

    boolean tdmc$isGoingUp();

    boolean tdmc$isGoingDown();

    boolean tdmc$isAttacking();

    boolean tdmc$isStriking();

    boolean tdmc$isBreathingFire();

    int tdmc$getDragonStage();

    byte tdmc$getControlState();

    boolean tdmc$isRidingPlayer(Player player);

    default void tdmc$clearUnifiedControls() {
        tdmc$setGoingUp(false);
        tdmc$setGoingDown(false);
        tdmc$setAttacking(false);
        tdmc$setStriking(false);
    }
}
