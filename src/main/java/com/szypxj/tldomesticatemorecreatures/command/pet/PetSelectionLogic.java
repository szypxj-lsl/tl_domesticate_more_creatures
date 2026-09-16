package com.szypxj.tldomesticatemorecreatures.command.pet;

import java.util.LinkedHashSet;
import java.util.UUID;

public final class PetSelectionLogic {
    private PetSelectionLogic() {
    }

    public static void apply(LinkedHashSet<UUID> selected, UUID target, PetSelectionMode mode) {
        if (mode == PetSelectionMode.MULTI_TOGGLE) {
            if (!selected.add(target)) {
                selected.remove(target);
            }
            return;
        }

        if (selected.size() == 1 && selected.contains(target)) {
            selected.clear();
            return;
        }

        selected.clear();
        selected.add(target);
    }
}
