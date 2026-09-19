package com.szypxj.tldomesticatemorecreatures.imprint;

import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommand;
import com.szypxj.tldomesticatemorecreatures.command.pet.executor.PetCommandExecutor;
import com.szypxj.tldomesticatemorecreatures.command.pet.executor.PetCommandExecutors;
import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingFood;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingRuleManager;
import com.szypxj.tldomesticatemorecreatures.game.AttributeService;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.network.ImprintSnapshot;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class ImprintService {
    public static final double BOND_HEALTH_BONUS = 0.30D;
    public static final double BOND_DAMAGE_BONUS = 0.30D;
    private static final long NEXT_NEED_DELAY_TICKS = 30L * 20L;
    private static final String PENDING_OFFSPRING_KEY = "tl_domesticate_more_creatures_imprint_pending_offspring";
    private static final Set<LivingEntity> ACTIVE = Collections.newSetFromMap(new WeakHashMap<>());
    private static final ConcurrentHashMap<ResourceLocation, List<ResourceLocation>> FOOD_CANDIDATE_CACHE = new ConcurrentHashMap<>();

    private ImprintService() {
    }

    public static void markOffspring(LivingEntity child) {
        if (child == null || child instanceof Player || child.level().isClientSide) {
            return;
        }
        child.getPersistentData().putBoolean(PENDING_OFFSPRING_KEY, true);
    }

    public static void startMarkedOffspring(LivingEntity child) {
        if (child == null || child.level().isClientSide || !child.getPersistentData().getBoolean(PENDING_OFFSPRING_KEY)) {
            return;
        }
        child.getPersistentData().remove(PENDING_OFFSPRING_KEY);
        startIfEligible(child);
    }

    public static void startIfEligible(LivingEntity child) {
        if (child == null || child instanceof Player || child.level().isClientSide) {
            return;
        }
        if (!LevelService.isAffected(child) || !ProgressData.exists(child) || ImprintData.exists(child)) {
            return;
        }
        if (!child.isBaby()) {
            return;
        }

        long now = child.level().getGameTime();
        long configuredTicks = Math.max(0L, (long) Config.IMPRINT_DURATION_MINUTES.get() * 60L * 20L);
        long growthTicks = remainingGrowthTicks(child);
        long window = ImprintMath.effectiveWindowTicks(configuredTicks, growthTicks);
        ImprintData data = ImprintData.of(child);
        data.initialized(true);
        data.active(window > 0L);
        data.finished(false);
        data.startedAt(now);
        data.endsAt(saturatingAdd(now, window));
        data.completed(0);
        data.finalPercent(0);
        data.levelCapBonus(0);
        data.bonded(false);
        data.startedAsBaby(child.isBaby());
        data.lastCareAt(Long.MIN_VALUE);

        List<ResourceLocation> foods = foodCandidates(child);
        boolean canWalk = supportsWalkNeed(child);
        RandomSource random = child.getRandom();
        for (int i = 0; i < ImprintData.CARE_COUNT; i++) {
            data.needAt(i, Long.MAX_VALUE);
            ImprintNeedType type = chooseNeed(random, !foods.isEmpty(), canWalk);
            data.needType(i, type);
            data.needCompleted(i, false);
            if (type == ImprintNeedType.FEED && !foods.isEmpty()) {
                data.needFood(i, foods.get(random.nextInt(foods.size())).toString());
            } else {
                data.needFood(i, "");
            }
        }
        data.needAt(0, now);

        if (window <= 0L) {
            finish(child, data);
        } else {
            ACTIVE.add(child);
            NetworkHandler.sendImprintState(child);
        }
    }

    public static void clearFoodCache() {
        FOOD_CANDIDATE_CACHE.clear();
    }

    public static void registerIfActive(LivingEntity entity) {
        if (entity != null && ImprintData.exists(entity) && ImprintData.of(entity).active()) {
            ACTIVE.add(entity);
        }
    }

    public static void unregister(LivingEntity entity) {
        ACTIVE.remove(entity);
    }

    public static void tickAll() {
        if (ACTIVE.isEmpty()) {
            return;
        }
        ACTIVE.removeIf(entity -> !tick(entity));
    }

    private static boolean tick(LivingEntity entity) {
        if (entity == null || entity.isRemoved() || entity.level().isClientSide || !ImprintData.exists(entity)) {
            return false;
        }
        ImprintData data = ImprintData.of(entity);
        if (!data.active()) {
            return false;
        }
        long now = entity.level().getGameTime();
        if (data.completed() >= ImprintData.CARE_COUNT
                || now >= data.endsAt()
                || (data.startedAsBaby() && !entity.isBaby())) {
            finish(entity, data);
            return false;
        }
        return true;
    }

    public static boolean tryFeed(ServerPlayer owner, LivingEntity child, ItemStack stack) {
        if (owner == null || child == null || stack == null || stack.isEmpty() || !isOwner(owner, child)) {
            return false;
        }
        ImprintData data = activeData(child);
        if (data == null) {
            return false;
        }
        long now = child.level().getGameTime();
        int index = data.currentNeedIndex(now);
        if (index < 0 || data.needType(index) != ImprintNeedType.FEED) {
            return false;
        }
        ResourceLocation required = ResourceLocation.tryParse(data.needFood(index));
        ResourceLocation held = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (required == null || !required.equals(held)) {
            return false;
        }
        if (!completeNeed(owner, child, data, index, now)) {
            return false;
        }
        if (!owner.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return true;
    }

    public static boolean tryPet(ServerPlayer owner, LivingEntity child) {
        if (owner == null || child == null || !isOwner(owner, child)) {
            return false;
        }
        ImprintData data = activeData(child);
        if (data == null) {
            return false;
        }
        long now = child.level().getGameTime();
        int index = data.currentNeedIndex(now);
        if (index < 0 || data.needType(index) != ImprintNeedType.PET) {
            return false;
        }
        return completeNeed(owner, child, data, index, now);
    }

    public static void onFollowCommand(ServerPlayer owner, LivingEntity child) {
        if (owner == null || child == null || !isOwner(owner, child)) {
            return;
        }
        ImprintData data = activeData(child);
        if (data == null) {
            return;
        }
        long now = child.level().getGameTime();
        int index = data.currentNeedIndex(now);
        if (index >= 0 && data.needType(index) == ImprintNeedType.WALK) {
            completeNeed(owner, child, data, index, now);
        }
    }

    public static double bondHealthMultiplier(LivingEntity entity) {
        return isBonded(entity) ? 1.0D + BOND_HEALTH_BONUS : 1.0D;
    }

    public static double bondDamageMultiplier(LivingEntity entity) {
        return isBonded(entity) ? 1.0D + BOND_DAMAGE_BONUS : 1.0D;
    }

    public static boolean isBonded(LivingEntity entity) {
        return entity != null && ImprintData.exists(entity) && ImprintData.of(entity).bonded();
    }

    public static ImprintSnapshot snapshot(LivingEntity entity) {
        if (entity == null || !ImprintData.exists(entity)) {
            return ImprintSnapshot.NONE;
        }
        ImprintData data = ImprintData.of(entity);
        long now = entity.level().getGameTime();
        int percent = data.finished()
                ? data.finalPercent()
                : ImprintMath.percent(data.completed(), ImprintData.CARE_COUNT);
        long remaining = data.active() ? Math.max(0L, data.endsAt() - now) : 0L;
        int current = data.active() ? data.currentNeedIndex(now) : -1;
        int displayNeed = current;
        String needType = "";
        String foodNameKey = "";
        long nextNeedTicks = 0L;
        if (current < 0 && data.active()) {
            long next = Long.MAX_VALUE;
            for (int i = 0; i < ImprintData.CARE_COUNT; i++) {
                long needAt = data.needAt(i);
                if (!data.needCompleted(i) && needAt > now && needAt < next) {
                    next = needAt;
                    displayNeed = i;
                }
            }
            if (displayNeed >= 0 && next != Long.MAX_VALUE) {
                nextNeedTicks = next - now;
            }
        }
        if (displayNeed >= 0) {
            needType = data.needType(displayNeed).name();
            if (data.needType(displayNeed) == ImprintNeedType.FEED) {
                ResourceLocation foodId = ResourceLocation.tryParse(data.needFood(displayNeed));
                Item item = foodId == null ? null : ForgeRegistries.ITEMS.getValue(foodId);
                if (item != null) {
                    foodNameKey = item.getDescriptionId();
                }
            }
        }
        return new ImprintSnapshot(
                true,
                data.active(),
                data.finished(),
                percent,
                remaining,
                needType,
                foodNameKey,
                nextNeedTicks,
                data.levelCapBonus(),
                data.bonded()
        );
    }

    private static ImprintData activeData(LivingEntity entity) {
        if (!ImprintData.exists(entity)) {
            return null;
        }
        ImprintData data = ImprintData.of(entity);
        return data.active() ? data : null;
    }

    private static boolean completeNeed(ServerPlayer owner, LivingEntity child, ImprintData data, int index, long now) {
        if (data.needCompleted(index) || data.lastCareAt() == now) {
            return false;
        }
        Component completedNeed = completedNeedLabel(data, index);
        data.lastCareAt(now);
        data.needCompleted(index, true);
        data.completed(data.completed() + 1);
        int completed = data.completed();
        if (completed >= ImprintData.CARE_COUNT) {
            finish(child, data);
            owner.displayClientMessage(Component.translatable(
                    "message.tl_domesticate_more_creatures.imprint_completed",
                    completedNeed,
                    completed,
                    ImprintData.CARE_COUNT
            ), true);
        } else {
            if (index + 1 < ImprintData.CARE_COUNT) {
                data.needAt(index + 1, saturatingAdd(now, NEXT_NEED_DELAY_TICKS));
            }
            NetworkHandler.sendImprintState(child);
            owner.displayClientMessage(Component.translatable(
                    "message.tl_domesticate_more_creatures.imprint_need_completed",
                    completedNeed,
                    completed,
                    ImprintData.CARE_COUNT
            ), true);
        }
        return true;
    }

    private static Component completedNeedLabel(ImprintData data, int index) {
        ImprintNeedType type = data.needType(index);
        return switch (type) {
            case PET -> Component.translatable("message.tl_domesticate_more_creatures.imprint_need_pet_label");
            case WALK -> Component.translatable("message.tl_domesticate_more_creatures.imprint_need_walk_label");
            case FEED -> {
                ResourceLocation foodId = ResourceLocation.tryParse(data.needFood(index));
                Item item = foodId == null ? null : ForgeRegistries.ITEMS.getValue(foodId);
                Component food = item == null
                        ? Component.translatable("message.tl_domesticate_more_creatures.imprint_food_unknown")
                        : Component.translatable(item.getDescriptionId());
                yield Component.translatable("message.tl_domesticate_more_creatures.imprint_need_feed_label", food);
            }
        };
    }

    private static void finish(LivingEntity entity, ImprintData data) {
        if (data.finished()) {
            data.active(false);
            return;
        }
        int percent = ImprintMath.percent(data.completed(), ImprintData.CARE_COUNT);
        int initialLevel = ProgressData.exists(entity) ? ProgressData.of(entity).initialLevel() : 1;
        data.finalPercent(percent);
        data.levelCapBonus(ImprintMath.levelCapBonus(initialLevel, percent));
        data.bonded(percent >= 100);
        data.active(false);
        data.finished(true);
        NetworkHandler.sendImprintState(entity);
        if (ProgressData.exists(entity)) {
            ProgressData progress = ProgressData.of(entity);
            progress.maxLevel(Math.max(progress.level(), LevelService.calculateMaxLevel(entity, progress.initialLevel())));
        }
        AttributeService.applyAll(entity);
        LevelService.syncLevel(entity);
    }

    private static boolean isOwner(ServerPlayer owner, LivingEntity child) {
        return PetOwnershipService.isOwnedBy(child, owner);
    }

    private static long remainingGrowthTicks(LivingEntity entity) {
        if (entity instanceof AgeableMob ageable) {
            int age = ageable.getAge();
            if (age < 0) {
                return -(long) age;
            }
            return ageable.isBaby() ? -1L : 0L;
        }
        return entity.isBaby() ? -1L : 0L;
    }

    private static boolean supportsWalkNeed(LivingEntity child) {
        if (!(child instanceof Mob mob)) {
            return false;
        }
        PetCommandExecutor follow = PetCommandExecutors.get(PetCommand.FOLLOW);
        return follow != null && follow.supports(mob);
    }

    private static ImprintNeedType chooseNeed(RandomSource random, boolean canFeed, boolean canWalk) {
        List<ImprintNeedType> candidates = new ArrayList<>(3);
        candidates.add(ImprintNeedType.PET);
        if (canFeed) {
            candidates.add(ImprintNeedType.FEED);
        }
        if (canWalk) {
            candidates.add(ImprintNeedType.WALK);
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private static List<ResourceLocation> foodCandidates(LivingEntity entity) {
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null) {
            return resolveFoodCandidates(entity);
        }
        return FOOD_CANDIDATE_CACHE.computeIfAbsent(entityId, ignored -> resolveFoodCandidates(entity));
    }

    private static List<ResourceLocation> resolveFoodCandidates(LivingEntity entity) {
        LinkedHashSet<ResourceLocation> result = new LinkedHashSet<>();
        if (entity instanceof Animal animal) {
            for (Item item : ForgeRegistries.ITEMS.getValues()) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                if (id != null && animal.isFood(new ItemStack(item))) {
                    result.add(id);
                }
            }
        }
        for (TamingFood food : TamingRuleManager.foodsFor(entity)) {
            result.add(food.itemId());
        }
        return List.copyOf(result);
    }

    private static long saturatingAdd(long a, long b) {
        if (b > 0L && a > Long.MAX_VALUE - b) {
            return Long.MAX_VALUE;
        }
        return a + b;
    }
}
