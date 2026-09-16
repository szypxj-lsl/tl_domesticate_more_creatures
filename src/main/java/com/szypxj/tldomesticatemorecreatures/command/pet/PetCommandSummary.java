package com.szypxj.tldomesticatemorecreatures.command.pet;

public enum PetCommandSummary {
    NONE,
    MOVE,
    ATTACK,
    FOLLOW,
    DEFEND,
    RETREAT,
    LAND,
    MIXED;

    public static PetCommandSummary of(PetCommand command) {
        return command == null ? NONE : valueOf(command.name());
    }
}
