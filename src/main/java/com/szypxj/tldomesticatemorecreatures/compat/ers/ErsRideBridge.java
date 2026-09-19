package com.szypxj.tldomesticatemorecreatures.compat.ers;

/** Optional bridge implemented by ERS vehicle mixins without a hard ERS dependency. */
public interface ErsRideBridge {
    void tdmc$defaultAttack();
    void tdmc$specialAttack();
    void tdmc$judgementAttack();
    void tdmc$turnAttack();
    void tdmc$jumpAttack();
    void tdmc$setDiving(boolean active);
    boolean tdmc$isDiving();
}
