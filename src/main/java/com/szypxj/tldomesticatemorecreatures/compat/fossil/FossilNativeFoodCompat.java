package com.szypxj.tldomesticatemorecreatures.compat.fossil;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Reads Fossils & Archeology's authoritative Dinopedia food cache without creating a hard dependency.
 *
 * <p>The Dinopedia renders edible entries from
 * {@code FoodMappingsManager.INSTANCE.getItemCache().get(dino.data().diet())}. This bridge intentionally
 * follows that exact source instead of guessing from {@code Animal#isFood}.</p>
 */
public final class FossilNativeFoodCompat {
    private static final String FOOD_MANAGER_CLASS =
            "com.github.teamfossilsarcheology.fossil.food.FoodMappingsManager";

    private FossilNativeFoodCompat() {
    }

    /**
     * @return the Dinopedia-backed item ids, an empty list when the diet genuinely has no item foods,
     *         or {@code null} when the Fossil API/data cannot be read and the caller should use its fallback.
     */
    public static List<ResourceLocation> detectNativeFoodIds(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        try {
            Object data = entity.getClass().getMethod("data").invoke(entity);
            Object diet = data == null ? null : data.getClass().getMethod("diet").invoke(data);
            if (diet == null) {
                return null;
            }

            Class<?> managerClass = Class.forName(FOOD_MANAGER_CLASS);
            Object manager = managerClass.getField("INSTANCE").get(null);
            Object cacheObject = managerClass.getMethod("getItemCache").invoke(manager);
            if (!(cacheObject instanceof Map<?, ?> cache)) {
                return null;
            }

            Object foodSet = cache.get(diet);
            if (foodSet == null) {
                return List.of();
            }
            if (!(foodSet instanceof Iterable<?> foods)) {
                return null;
            }

            LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
            for (Object value : foods) {
                if (!(value instanceof ItemLike itemLike)) {
                    continue;
                }
                Item item = itemLike.asItem();
                if (item == null || item == Items.AIR) {
                    continue;
                }
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
                if (itemId != null) {
                    ids.add(itemId);
                }
            }

            List<ResourceLocation> result = new ArrayList<>(ids);
            result.sort(Comparator.comparing(ResourceLocation::toString));
            return List.copyOf(result);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

}
