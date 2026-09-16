package com.szypxj.tldomesticatemorecreatures.game.genetics;

import net.minecraft.world.entity.Entity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Predicate;

public final class GeneticEntityTransferScope {
    private static final ThreadLocal<Deque<Scope>> SCOPES = ThreadLocal.withInitial(ArrayDeque::new);

    private GeneticEntityTransferScope() {
    }

    public static void begin(HatchGeneticPayload payload, Predicate<Entity> targetPredicate, Runnable consume) {
        if (payload == null || targetPredicate == null) {
            return;
        }
        SCOPES.get().push(new Scope(payload, targetPredicate, consume == null ? () -> { } : consume));
    }

    public static boolean tryTransfer(Entity entity) {
        Deque<Scope> stack = SCOPES.get();
        if (stack.isEmpty()) {
            return false;
        }
        Scope scope = stack.peek();
        if (entity == null || scope.transferred || !scope.targetPredicate.test(entity)) {
            return false;
        }
        GeneticEntityCarrier.set(entity, scope.payload);
        scope.transferred = true;
        return true;
    }

    public static void end() {
        Deque<Scope> stack = SCOPES.get();
        if (stack.isEmpty()) {
            return;
        }
        Scope scope = stack.pop();
        if (scope.transferred) {
            scope.consume.run();
        }
        if (stack.isEmpty()) {
            SCOPES.remove();
        }
    }

    private static final class Scope {
        private final HatchGeneticPayload payload;
        private final Predicate<Entity> targetPredicate;
        private final Runnable consume;
        private boolean transferred;

        private Scope(HatchGeneticPayload payload, Predicate<Entity> targetPredicate, Runnable consume) {
            this.payload = payload;
            this.targetPredicate = targetPredicate;
            this.consume = consume;
        }
    }
}
