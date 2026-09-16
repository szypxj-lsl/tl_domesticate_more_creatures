package com.szypxj.tldomesticatemorecreatures.api.backpack;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PetBackpackFoodApi {
    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();

    private PetBackpackFoodApi() {
    }

    public static void register(Provider provider) {
        if (provider != null && !PROVIDERS.contains(provider)) {
            PROVIDERS.add(provider);
        }
    }

    public static double foodValue(LivingEntity entity, ItemStack stack) {
        Provider provider = provider(entity);
        return provider == null || stack == null || stack.isEmpty()
                ? 0.0D
                : Math.max(0.0D, provider.foodValue(entity, stack));
    }

    public static double currentFood(LivingEntity entity) {
        Provider provider = provider(entity);
        return provider == null ? 0.0D : Math.max(0.0D, provider.currentFood(entity));
    }

    public static double maxFood(LivingEntity entity) {
        Provider provider = provider(entity);
        return provider == null ? 0.0D : Math.max(0.0D, provider.maxFood(entity));
    }

    public static void addFood(LivingEntity entity, double amount) {
        Provider provider = provider(entity);
        if (provider != null && amount > 0.0D && Double.isFinite(amount)) {
            provider.addFood(entity, amount);
        }
    }

    public static boolean forceUse(Player player, LivingEntity entity, ItemStack stack) {
        Provider provider = provider(entity);
        return provider != null && stack != null && !stack.isEmpty() && provider.forceUse(player, entity, stack);
    }

    public static boolean available(LivingEntity entity) {
        return provider(entity) != null;
    }

    private static Provider provider(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        for (Provider provider : PROVIDERS) {
            try {
                if (provider.supports(entity)) {
                    return provider;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    public interface Provider {
        boolean supports(LivingEntity entity);

        double foodValue(LivingEntity entity, ItemStack stack);

        double currentFood(LivingEntity entity);

        double maxFood(LivingEntity entity);

        void addFood(LivingEntity entity, double amount);

        boolean forceUse(Player player, LivingEntity entity, ItemStack stack);
    }
}
