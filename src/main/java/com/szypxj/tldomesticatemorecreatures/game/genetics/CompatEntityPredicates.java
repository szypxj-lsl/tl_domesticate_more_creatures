package com.szypxj.tldomesticatemorecreatures.game.genetics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.registries.ForgeRegistries;

public final class CompatEntityPredicates {
    private CompatEntityPredicates() {
    }

    public static boolean namespace(Entity entity, String namespace) {
        ResourceLocation id = id(entity);
        return id != null && CompatEntityIdPolicy.matchesNamespace(id.toString(), namespace);
    }

    public static boolean anyNamespace(Entity entity, String... namespaces) {
        ResourceLocation id = id(entity);
        return id != null && CompatEntityIdPolicy.matchesAnyNamespace(id.toString(), namespaces);
    }

    public static boolean exact(Entity entity, String expectedId) {
        ResourceLocation id = id(entity);
        return id != null && CompatEntityIdPolicy.matchesExact(id.toString(), expectedId);
    }

    private static ResourceLocation id(Entity entity) {
        return entity == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
    }
}
