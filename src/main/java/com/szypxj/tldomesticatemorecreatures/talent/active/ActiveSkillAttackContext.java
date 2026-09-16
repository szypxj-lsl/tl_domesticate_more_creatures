package com.szypxj.tldomesticatemorecreatures.talent.active;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class ActiveSkillAttackContext {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private ActiveSkillAttackContext() {
    }

    public static boolean isActiveSkillAttack() {
        return DEPTH.get() > 0;
    }

    public static boolean isActive() {
        return isActiveSkillAttack();
    }

    public static <T> T runActiveAttack(Supplier<T> action) {
        DEPTH.set(DEPTH.get() + 1);
        try {
            return action.get();
        } finally {
            exit();
        }
    }

    public static boolean run(BooleanSupplier action) {
        DEPTH.set(DEPTH.get() + 1);
        try {
            return action.getAsBoolean();
        } finally {
            exit();
        }
    }

    private static void exit() {
        int next = DEPTH.get() - 1;
        if (next <= 0) DEPTH.remove();
        else DEPTH.set(next);
    }
}
