package com.szypxj.tldomesticatemorecreatures.domestication;

import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.api.backpack.PetBackpackFoodApi;
import com.szypxj.tldomesticatemorecreatures.backpack.PetBackpackService;
import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import com.szypxj.tldomesticatemorecreatures.elite.EliteService;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.torpor.TorporService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

public final class TamingService {
    private TamingService() {
    }

    public static InteractionResult tryInteract(Player player, LivingEntity entity, ItemStack stack) {
        if (!LevelService.isAffected(entity)) {
            return InteractionResult.PASS;
        }
        LevelService.initializeIfNeeded(entity);
        if (EliteService.isElite(entity) && !EliteService.canBeTamed(entity)) {
            return InteractionResult.FAIL;
        }
        Optional<TamingRule> optionalRule = TamingRuleManager.ruleFor(entity);
        if (optionalRule.isEmpty() || PetOwnershipService.isTamed(entity)) {
            return InteractionResult.PASS;
        }

        TamingRule rule = optionalRule.get();
        Optional<TamingFood> optionalFood = matchingFood(entity, stack);
        if (optionalFood.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (!meetsPlayerLevelRequirement(player, rule)) {
            return InteractionResult.FAIL;
        }

        if (rule.method() == TamingMethod.KNOCKOUT && !TorporService.isUnconscious(entity)) {
            return InteractionResult.FAIL;
        }

        DomesticationData data = DomesticationData.of(entity);
        if (data.tamingPlayerUuid().isPresent() && !data.tamingPlayerUuid().get().equals(player.getUUID())) {
            return InteractionResult.FAIL;
        }
        if (data.tamingPlayerUuid().isEmpty()) {
            data.tamingPlayerUuid(player.getUUID());
        }

        TamingFood food = optionalFood.get();
        double nextProgress = Math.min(1.0D, data.tamingProgress() + TamingMath.foodProgress(1, food.amount()));
        data.tamingProgress(nextProgress);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        if (nextProgress >= 1.0D) {
            completeTaming(player, entity, rule);
        }
        return InteractionResult.sidedSuccess(entity.level().isClientSide);
    }

    public static boolean claimKnockoutSession(Player player, LivingEntity entity) {
        if (player == null || entity == null || PetOwnershipService.isTamed(entity) || !TorporService.isUnconscious(entity)) {
            return false;
        }
        Optional<TamingRule> optionalRule = TamingRuleManager.ruleFor(entity);
        if (optionalRule.isEmpty() || optionalRule.get().method() != TamingMethod.KNOCKOUT
                || !meetsPlayerLevelRequirement(player, optionalRule.get())) {
            return false;
        }
        DomesticationData data = DomesticationData.of(entity);
        if (data.tamingPlayerUuid().isPresent() && !data.tamingPlayerUuid().get().equals(player.getUUID())) {
            return false;
        }
        if (data.tamingPlayerUuid().isEmpty()) {
            data.tamingPlayerUuid(player.getUUID());
        }
        return true;
    }

    public static boolean consumeBackpackTamingFood(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide || PetOwnershipService.isTamed(entity)
                || !TorporService.isUnconscious(entity) || !PetBackpackFoodApi.available(entity)) {
            return false;
        }
        Optional<TamingRule> optionalRule = TamingRuleManager.ruleFor(entity);
        if (optionalRule.isEmpty() || optionalRule.get().method() != TamingMethod.KNOCKOUT) {
            return false;
        }
        DomesticationData data = DomesticationData.of(entity);
        if (data.tamingPlayerUuid().isEmpty() || !(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(data.tamingPlayerUuid().get());
        if (player == null || !meetsPlayerLevelRequirement(player, optionalRule.get())) {
            return false;
        }

        double maxFood = PetBackpackFoodApi.maxFood(entity);
        double currentFood = PetBackpackFoodApi.currentFood(entity);
        double missingFood = Math.max(0.0D, maxFood - currentFood);
        if (maxFood <= 0.0D || missingFood <= 1.0E-6D) {
            return false;
        }

        int bestSlot = -1;
        TamingFood bestFood = null;
        double bestValue = -1.0D;
        for (int slot = 0; slot < PetBackpackService.BACKPACK_SIZE; slot++) {
            ItemStack stack = PetBackpackService.getItem(entity, slot);
            Optional<TamingFood> food = matchingFood(entity, stack);
            if (food.isEmpty()) {
                continue;
            }
            double foodValue = PetBackpackFoodApi.foodValue(entity, stack);
            if (foodValue <= 0.0D || foodValue > missingFood + 1.0E-6D || foodValue <= bestValue) {
                continue;
            }
            bestSlot = slot;
            bestFood = food.get();
            bestValue = foodValue;
        }
        if (bestSlot < 0 || bestFood == null) {
            return false;
        }

        ItemStack stack = PetBackpackService.getItem(entity, bestSlot);
        stack.shrink(1);
        PetBackpackService.setItem(entity, bestSlot, stack);
        PetBackpackFoodApi.addFood(entity, bestValue);

        double nextProgress = Math.min(1.0D, data.tamingProgress() + TamingMath.foodProgress(1, bestFood.amount()));
        data.tamingProgress(nextProgress);
        if (nextProgress >= 1.0D) {
            completeTaming(player, entity, optionalRule.get());
        }
        return true;
    }

    public static void onKnockoutStart(LivingEntity entity) {
        if (!LevelService.isAffected(entity)) {
            return;
        }
        Optional<TamingRule> rule = TamingRuleManager.ruleFor(entity);
        if (rule.isEmpty() || rule.get().method() != TamingMethod.KNOCKOUT || PetOwnershipService.isTamed(entity)) {
            return;
        }
        DomesticationData data = DomesticationData.of(entity);
        data.resetTamingSession();
        data.knockoutMaxHealth(entity.getMaxHealth());
    }

    public static void onWake(LivingEntity entity) {
        if (!LevelService.isAffected(entity)) {
            return;
        }
        Optional<TamingRule> rule = TamingRuleManager.ruleFor(entity);
        if (rule.isPresent() && rule.get().method() == TamingMethod.KNOCKOUT && !PetOwnershipService.isTamed(entity)) {
            DomesticationData.of(entity).resetTamingSession();
        }
    }

    public static void recordUnconsciousDamage(LivingEntity entity, double amount) {
        if (!LevelService.isAffected(entity) || amount <= 0.0D || !TorporService.isUnconscious(entity)) {
            return;
        }
        Optional<TamingRule> rule = TamingRuleManager.ruleFor(entity);
        if (rule.isEmpty() || rule.get().method() != TamingMethod.KNOCKOUT || PetOwnershipService.isTamed(entity)) {
            return;
        }
        DomesticationData data = DomesticationData.of(entity);
        data.knockoutDamage(data.knockoutDamage() + amount);
    }


    public static boolean meetsPlayerLevelRequirement(Player player, TamingRule rule) {
        if (player == null || rule == null) {
            return false;
        }
        LevelService.initializeIfNeeded(player);
        return ProgressData.of(player).level() >= rule.requiredPlayerLevel();
    }

    public static double tamingProgress(LivingEntity entity) {
        return DomesticationData.of(entity).tamingProgress();
    }

    public static double tamingEfficiency(LivingEntity entity) {
        DomesticationData data = DomesticationData.of(entity);
        return TamingMath.efficiency(data.knockoutDamage(), data.knockoutMaxHealth());
    }

    private static Optional<TamingFood> matchingFood(LivingEntity entity, ItemStack stack) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) {
            return Optional.empty();
        }
        return TamingRuleManager.foodsFor(entity).stream()
                .filter(food -> food.itemId().equals(itemId))
                .findFirst();
    }

    private static void completeTaming(Player player, LivingEntity entity, TamingRule rule) {
        if (EliteService.isElite(entity)) {
            if (!EliteService.canBeTamed(entity)) {
                return;
            }
            LevelService.normalizeEliteForTaming(entity);
        }
        double bonusRate = Config.TAMING_BONUS_RATE.get();
        if (rule.method() == TamingMethod.KNOCKOUT) {
            DomesticationData data = DomesticationData.of(entity);
            bonusRate = TamingMath.actualBonusRate(
                    bonusRate,
                    data.knockoutDamage(),
                    data.knockoutMaxHealth()
            );
        }

        LevelService.initializeIfNeeded(entity);
        PetOwnershipService.setCustomOwner(entity, player);
        LevelService.markTamed(entity, bonusRate);
        TorporService.clear(entity);
        PetAiService.register(entity);
    }
}
