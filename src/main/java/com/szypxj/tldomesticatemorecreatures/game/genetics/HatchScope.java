package com.szypxj.tldomesticatemorecreatures.game.genetics;

import com.szypxj.tldomesticatemorecreatures.game.HatchSeedMath;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Predicate;

public final class HatchScope {
    private static final ThreadLocal<Deque<Scope>> SCOPES = ThreadLocal.withInitial(ArrayDeque::new);

    private HatchScope() {
    }

    public static void begin(HatchGeneticPayload payload, Predicate<LivingEntity> childPredicate, Runnable consume) {
        if (payload == null || childPredicate == null) {
            return;
        }
        SCOPES.get().push(new Scope(payload, childPredicate, consume == null ? () -> { } : consume));
    }

    public static boolean tryApply(LivingEntity child) {
        Deque<Scope> stack = SCOPES.get();
        if (stack.isEmpty()) {
            return false;
        }
        Scope scope = stack.peek();
        if (child == null || !scope.childPredicate.test(child) || !LevelService.isAffected(child)) {
            return false;
        }

        ChildGeneticsResult result = null;
        ResourceLocation actualId = ForgeRegistries.ENTITY_TYPES.getKey(child.getType());
        if (scope.appliedCount == 0
                && scope.payload.result() != null
                && actualId != null
                && actualId.toString().equals(scope.payload.childEntityId())) {
            result = scope.payload.result();
        }
        if (result == null) {
            long seed = HatchSeedMath.childSeed(scope.payload.seed(), scope.appliedCount);
            String childId = actualId == null ? scope.payload.childEntityId() : actualId.toString();
            result = LevelService.resolveInheritedResult(
                    childId,
                    scope.payload.parentA(),
                    scope.payload.parentB(),
                    RandomSource.create(seed)
            );
        }
        boolean applied = result != null && LevelService.applyInheritedResult(child, result);
        if (applied) {
            scope.appliedCount++;
        }
        return applied;
    }

    public static void end() {
        Deque<Scope> stack = SCOPES.get();
        if (stack.isEmpty()) {
            return;
        }
        Scope scope = stack.pop();
        if (scope.appliedCount > 0) {
            scope.consume.run();
        }
        if (stack.isEmpty()) {
            SCOPES.remove();
        }
    }

    private static final class Scope {
        private final HatchGeneticPayload payload;
        private final Predicate<LivingEntity> childPredicate;
        private final Runnable consume;
        private int appliedCount;

        private Scope(HatchGeneticPayload payload, Predicate<LivingEntity> childPredicate, Runnable consume) {
            this.payload = payload;
            this.childPredicate = childPredicate;
            this.consume = consume;
        }
    }
}
