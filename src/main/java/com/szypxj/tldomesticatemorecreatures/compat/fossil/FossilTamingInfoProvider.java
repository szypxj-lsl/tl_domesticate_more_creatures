package com.szypxj.tldomesticatemorecreatures.compat.fossil;

import com.szypxj.tldomesticatemorecreatures.api.creature.TamingInfo;
import com.szypxj.tldomesticatemorecreatures.api.creature.taming.CreatureTamingInfoProvider;
import com.szypxj.tldomesticatemorecreatures.api.creature.taming.CreatureTamingInfoProviderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.List;

/** Uses Fossil's authoritative AI.taming value instead of guessing from TamableAnimal inheritance. */
public final class FossilTamingInfoProvider implements CreatureTamingInfoProvider {
    private static final String INFO_CLASS =
            "com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.PrehistoricEntityInfo";

    public static void register() {
        CreatureTamingInfoProviderRegistry.register(new FossilTamingInfoProvider());
    }

    private FossilTamingInfoProvider() {
    }

    @Override
    public String id() {
        return "tl_domesticate_more_creatures:taming/fossil";
    }

    @Override
    public int priority() {
        return 800;
    }

    @Override
    public boolean supports(ResourceLocation entityTypeId) {
        return entityTypeId != null && "fossil".equals(entityTypeId.getNamespace());
    }

    @Override
    public TamingInfo liveInfo(LivingEntity entity) {
        return entity == null ? TamingInfo.NOT_TAMEABLE : infoFor(entity.getType());
    }

    @Override
    public TamingInfo representativeInfo(ServerLevel level, EntityType<?> type, ResourceLocation entityTypeId) {
        return infoFor(type);
    }

    private static TamingInfo infoFor(EntityType<?> type) {
        Object data = findData(type);
        Object ai = invoke(data, "ai");
        Object taming = invoke(ai, "taming");
        String method = taming instanceof Enum<?> enumValue ? enumValue.name() : "";
        if (method.isBlank() || "NONE".equals(method)) {
            return TamingInfo.NOT_TAMEABLE;
        }
        return new TamingInfo(true, "FOSSIL_" + method, 1, List.of());
    }

    private static Object findData(EntityType<?> type) {
        if (type == null) {
            return null;
        }
        try {
            Class<?> infoClass = Class.forName(INFO_CLASS);
            Method entityType = infoClass.getMethod("entityType");
            Method data = infoClass.getMethod("data");
            Object[] values = infoClass.getEnumConstants();
            if (values == null) {
                return null;
            }
            for (Object value : values) {
                if (entityType.invoke(value) == type) {
                    return data.invoke(value);
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
        return null;
    }

    private static Object invoke(Object target, String name) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(name).invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }
}
