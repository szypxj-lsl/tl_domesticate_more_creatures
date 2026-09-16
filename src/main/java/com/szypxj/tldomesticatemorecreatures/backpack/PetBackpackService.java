package com.szypxj.tldomesticatemorecreatures.backpack;

import com.szypxj.tldomesticatemorecreatures.api.backpack.PetBackpackFoodApi;
import com.szypxj.tldomesticatemorecreatures.api.compat.ErsCompatApi;
import com.szypxj.tldomesticatemorecreatures.domestication.DomesticationData;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingMethod;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingRule;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingRuleManager;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingService;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.torpor.TorporService;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.WeakHashMap;

public final class PetBackpackService {
    public static final int BACKPACK_SIZE = 18;
    private static final Map<LivingEntity, NonNullList<ItemStack>> CACHE = new WeakHashMap<>();

    private PetBackpackService() {
    }

    public static boolean isKnockoutBackpackTarget(LivingEntity entity) {
        if (entity == null || PetOwnershipService.isTamed(entity) || !TorporService.isUnconscious(entity)) {
            return false;
        }
        Optional<TamingRule> rule = TamingRuleManager.ruleFor(entity);
        return rule.isPresent() && rule.get().method() == TamingMethod.KNOCKOUT;
    }

    public static boolean canOpenPanel(ServerPlayer viewer, LivingEntity entity) {
        if (viewer == null || entity == null || !PetOwnershipService.isPanelEntityAllowed(entity)) {
            return false;
        }
        return PetOwnershipService.canUsePanel(entity) || canOpenKnockoutPanel(viewer, entity);
    }

    public static boolean canOpenKnockoutPanel(ServerPlayer viewer, LivingEntity entity) {
        if (viewer == null || entity == null || !isKnockoutBackpackTarget(entity)) {
            return false;
        }
        Optional<TamingRule> rule = TamingRuleManager.ruleFor(entity);
        if (rule.isEmpty() || !TamingService.meetsPlayerLevelRequirement(viewer, rule.get())) {
            return false;
        }
        Optional<UUID> claimed = DomesticationData.of(entity).tamingPlayerUuid();
        return claimed.isEmpty() || claimed.get().equals(viewer.getUUID());
    }

    public static boolean available(Player viewer, LivingEntity entity) {
        if (viewer == null || entity == null || entity instanceof Player || ErsCompatApi.isErsEntity(entity)) {
            return false;
        }
        return PetOwnershipService.isTamed(entity) || isKnockoutBackpackTarget(entity);
    }

    public static boolean editable(Player viewer, LivingEntity entity) {
        if (!available(viewer, entity)) {
            return false;
        }
        if (PetOwnershipService.isTamed(entity)) {
            return PetOwnershipService.isOwnedBy(entity, viewer);
        }
        Optional<UUID> claimed = DomesticationData.of(entity).tamingPlayerUuid();
        return claimed.isPresent() && claimed.get().equals(viewer.getUUID());
    }

    public static ItemStack getItem(LivingEntity entity, int slot) {
        if (entity == null || slot < 0 || slot >= BACKPACK_SIZE) {
            return ItemStack.EMPTY;
        }
        return items(entity).get(slot);
    }

    public static void setItem(LivingEntity entity, int slot, ItemStack stack) {
        if (entity == null || slot < 0 || slot >= BACKPACK_SIZE) {
            return;
        }
        NonNullList<ItemStack> items = items(entity);
        items.set(slot, stack == null ? ItemStack.EMPTY : stack.copy());
        PetBackpackData.save(entity, items);
    }

    public static NonNullList<ItemStack> contents(LivingEntity entity) {
        NonNullList<ItemStack> copy = NonNullList.withSize(BACKPACK_SIZE, ItemStack.EMPTY);
        NonNullList<ItemStack> source = items(entity);
        for (int i = 0; i < BACKPACK_SIZE; i++) {
            copy.set(i, source.get(i).copy());
        }
        return copy;
    }

    public static void saveContents(LivingEntity entity, NonNullList<ItemStack> contents) {
        if (entity == null || contents == null) {
            return;
        }
        NonNullList<ItemStack> target = NonNullList.withSize(BACKPACK_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(BACKPACK_SIZE, contents.size()); i++) {
            target.set(i, contents.get(i).copy());
        }
        synchronized (CACHE) {
            CACHE.put(entity, target);
        }
        PetBackpackData.save(entity, target);
    }

    public static void clear(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        synchronized (CACHE) {
            CACHE.remove(entity);
        }
        PetBackpackData.clear(entity);
    }

    private static NonNullList<ItemStack> items(LivingEntity entity) {
        if (entity == null) {
            return NonNullList.withSize(BACKPACK_SIZE, ItemStack.EMPTY);
        }
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(entity, PetBackpackData::load);
        }
    }

    public static boolean forceUse(Player player, LivingEntity entity, int slot) {
        if (player == null || entity == null || slot < 0 || slot >= BACKPACK_SIZE || !editable(player, entity)) {
            return false;
        }
        ItemStack stack = getItem(entity, slot);
        if (stack.isEmpty() || !PetBackpackFoodApi.forceUse(player, entity, stack)) {
            return false;
        }
        setItem(entity, slot, stack);
        return true;
    }
}
