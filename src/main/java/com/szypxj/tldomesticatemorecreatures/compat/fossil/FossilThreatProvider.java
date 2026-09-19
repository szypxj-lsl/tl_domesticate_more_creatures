package com.szypxj.tldomesticatemorecreatures.compat.fossil;

import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CreatureThreatProfile;
import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CreatureThreatProvider;
import com.szypxj.tldomesticatemorecreatures.api.creature.threat.CreatureThreatProviderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;

/** Uses Fossil's own age/data records so live juveniles and representative adults are not conflated. */
public final class FossilThreatProvider implements CreatureThreatProvider {
    private static final String MOD_ID = "fossil";
    private static final String INFO_CLASS = "com.github.teamfossilsarcheology.fossil.entity.prehistoric.base.PrehistoricEntityInfo";

    public static void register() { CreatureThreatProviderRegistry.register(new FossilThreatProvider()); }
    private FossilThreatProvider() {}

    @Override public String id() { return "tl_domesticate_more_creatures:threat/fossil"; }
    @Override public int priority() { return 780; }
    @Override public boolean supports(ResourceLocation id) { return id != null && MOD_ID.equals(id.getNamespace()); }

    @Override
    public CreatureThreatProfile liveProfile(LivingEntity entity, CreatureThreatProfile fallback) {
        if (entity == null) return fallback;
        int age = invokeInt(entity, "getAgeInDays", -1);
        int adult = adultAge(entity.getType());
        String form = age < 0 ? "fossil:live" : (adult > 0 && age >= adult ? "fossil:adult" : "fossil:age_" + age);
        return new CreatureThreatProfile(
                fallback.maxHealth(), fallback.movementSpeed(), fallback.meleeDamage(), fallback.combatChannels(), form
        );
    }

    @Override
    public CreatureThreatProfile representativeProfile(EntityType<?> type, ResourceLocation id, CreatureThreatProfile fallback) {
        Object data = findData(type);
        if (data == null) return new CreatureThreatProfile(
                fallback.maxHealth(), fallback.movementSpeed(), fallback.meleeDamage(), fallback.combatChannels(), "fossil:adult"
        );
        try {
            Object attributes = data.getClass().getMethod("attributes").invoke(data);
            double health = invokeDouble(attributes, "maxHealth", fallback.maxHealth());
            double damage = invokeDouble(attributes, "maxDamage", fallback.meleeDamage());
            double ground = invokeDouble(attributes, "maxSpeed", fallback.movementSpeed());
            double swim = invokeDouble(attributes, "maxSwimSpeed", 0.0D);
            return new CreatureThreatProfile(health, Math.max(ground, swim), damage, fallback.combatChannels(), "fossil:adult");
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return fallback;
        }
    }

    private static int adultAge(EntityType<?> type) {
        Object data = findData(type);
        return data == null ? -1 : invokeInt(data, "adultAgeDays", -1);
    }

    private static Object findData(EntityType<?> type) {
        if (type == null) return null;
        try {
            Class<?> infoClass = Class.forName(INFO_CLASS);
            Method entityType = infoClass.getMethod("entityType");
            Method data = infoClass.getMethod("data");
            Object[] constants = infoClass.getEnumConstants();
            if (constants == null) return null;
            for (Object info : constants) {
                if (entityType.invoke(info) == type) return data.invoke(info);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
        return null;
    }

    private static int invokeInt(Object target, String name, int fallback) {
        if (target == null) return fallback;
        try {
            Object value = target.getClass().getMethod(name).invoke(target);
            return value instanceof Number number ? number.intValue() : fallback;
        } catch (ReflectiveOperationException | RuntimeException ignored) { return fallback; }
    }

    private static double invokeDouble(Object target, String name, double fallback) {
        if (target == null) return fallback;
        try {
            Object value = target.getClass().getMethod(name).invoke(target);
            if (value instanceof Number number && Double.isFinite(number.doubleValue())) return Math.max(0.0D, number.doubleValue());
        } catch (ReflectiveOperationException | RuntimeException ignored) { }
        return Math.max(0.0D, fallback);
    }
}
