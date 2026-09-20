package com.szypxj.tldomesticatemorecreatures.game.genetics;

import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.function.Predicate;

/** Transfers TDMC state when another mod replaces a juvenile entity with a new adult entity instance. */
public final class EntityStateTransferScope {
    private static final String TDMC_PREFIX = "tl_domesticate_more_creatures";
    private static final Set<String> TDMC_LEGACY_ROOTS = Set.of(
            "tdmcPetBackpack",
            "tdmcPetEquipment",
            "tdmc_base_attributes_v1"
    );
    private static final ThreadLocal<Deque<Scope>> SCOPES = ThreadLocal.withInitial(ArrayDeque::new);

    private EntityStateTransferScope() {}

    public static void begin(LivingEntity source, Predicate<LivingEntity> targetPredicate) {
        if (source == null || targetPredicate == null) return;
        SCOPES.get().push(new Scope(source, targetPredicate));
    }

    public static boolean tryTransfer(LivingEntity target) {
        Deque<Scope> stack = SCOPES.get();
        if (stack.isEmpty() || target == null) return false;
        Scope scope = stack.peek();
        if (scope.transferred || !scope.targetPredicate.test(target)) return false;

        for (String key : scope.source.getPersistentData().getAllKeys()) {
            if (!key.startsWith(TDMC_PREFIX) && !TDMC_LEGACY_ROOTS.contains(key)) continue;
            Tag value = scope.source.getPersistentData().get(key);
            if (value != null) target.getPersistentData().put(key, value.copy());
        }
        if (scope.source instanceof TamableAnimal from && target instanceof TamableAnimal to && from.isTame()) {
            to.setTame(true);
            if (from.getOwnerUUID() != null) to.setOwnerUUID(from.getOwnerUUID());
        }
        scope.transferred = true;
        return true;
    }

    public static void end() {
        Deque<Scope> stack = SCOPES.get();
        if (stack.isEmpty()) return;
        stack.pop();
        if (stack.isEmpty()) SCOPES.remove();
    }

    private static final class Scope {
        private final LivingEntity source;
        private final Predicate<LivingEntity> targetPredicate;
        private boolean transferred;
        private Scope(LivingEntity source, Predicate<LivingEntity> targetPredicate) {
            this.source = source;
            this.targetPredicate = targetPredicate;
        }
    }
}
