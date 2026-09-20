package com.szypxj.tldomesticatemorecreatures.api.creature.taming;

import com.szypxj.tldomesticatemorecreatures.api.creature.TamingInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class CreatureTamingInfoProviderRegistry {
    private static final List<CreatureTamingInfoProvider> PROVIDERS = new ArrayList<>();

    private CreatureTamingInfoProviderRegistry() {
    }

    public static synchronized void register(CreatureTamingInfoProvider provider) {
        Objects.requireNonNull(provider, "provider");
        if (PROVIDERS.stream().anyMatch(existing -> existing.id().equals(provider.id()))) {
            return;
        }
        PROVIDERS.add(provider);
        PROVIDERS.sort(Comparator.comparingInt(CreatureTamingInfoProvider::priority).reversed());
    }

    public static TamingInfo resolve(LivingEntity entity) {
        if (entity == null) {
            return TamingInfo.NOT_TAMEABLE;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null) {
            return TamingInfo.NOT_TAMEABLE;
        }
        for (CreatureTamingInfoProvider provider : snapshot()) {
            try {
                if (!provider.supports(id)) {
                    continue;
                }
                TamingInfo info = provider.liveInfo(entity);
                if (info != null && info.tameable()) {
                    return info;
                }
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        return TamingInfo.NOT_TAMEABLE;
    }

    public static TamingInfo resolve(ServerLevel level, EntityType<?> type) {
        if (level == null || type == null) {
            return TamingInfo.NOT_TAMEABLE;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id == null) {
            return TamingInfo.NOT_TAMEABLE;
        }
        for (CreatureTamingInfoProvider provider : snapshot()) {
            try {
                if (!provider.supports(id)) {
                    continue;
                }
                TamingInfo info = provider.representativeInfo(level, type, id);
                if (info != null && info.tameable()) {
                    return info;
                }
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        return TamingInfo.NOT_TAMEABLE;
    }

    private static synchronized List<CreatureTamingInfoProvider> snapshot() {
        return List.copyOf(PROVIDERS);
    }
}
