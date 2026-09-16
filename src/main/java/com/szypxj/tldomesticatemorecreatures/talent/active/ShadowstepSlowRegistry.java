package com.szypxj.tldomesticatemorecreatures.talent.active;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShadowstepSlowRegistry {
    private static final UUID SLOW_MODIFIER_ID = UUID.fromString("7c76208d-3538-4a2b-9c34-c3206623b035");
    private static final Map<UUID, Map<UUID, Double>> SOURCES = new ConcurrentHashMap<>();

    private ShadowstepSlowRegistry() {
    }

    public static void add(Mob mob, UUID sourceId, double actionMultiplier) {
        if (mob == null || sourceId == null || mob.level().isClientSide) return;
        double multiplier = Math.max(0.01D, Math.min(1.0D, actionMultiplier));
        SOURCES.computeIfAbsent(mob.getUUID(), ignored -> new ConcurrentHashMap<>()).put(sourceId, multiplier);
        applyMovementModifier(mob, effectiveMultiplier(mob.getUUID()));
    }

    public static void remove(Mob mob, UUID sourceId) {
        if (mob == null || sourceId == null) return;
        Map<UUID, Double> sources = SOURCES.get(mob.getUUID());
        if (sources == null) return;
        sources.remove(sourceId);
        if (sources.isEmpty()) {
            SOURCES.remove(mob.getUUID(), sources);
            removeMovementModifier(mob);
        } else {
            applyMovementModifier(mob, effectiveMultiplier(mob.getUUID()));
        }
    }

    public static boolean shouldRunActionThisTick(Mob mob) {
        if (mob == null) return true;
        double multiplier = effectiveMultiplier(mob.getUUID());
        if (multiplier >= 0.999D) return true;
        int interval = Math.max(1, (int) Math.round(1.0D / Math.max(0.01D, multiplier)));
        return mob.tickCount % interval == 0;
    }

    public static void clear(Mob mob) {
        if (mob == null) return;
        SOURCES.remove(mob.getUUID());
        removeMovementModifier(mob);
    }

    private static double effectiveMultiplier(UUID mobUuid) {
        Map<UUID, Double> sources = SOURCES.get(mobUuid);
        if (sources == null || sources.isEmpty()) return 1.0D;
        double result = 1.0D;
        for (double value : sources.values()) result = Math.min(result, value);
        return result;
    }

    private static void applyMovementModifier(Mob mob, double multiplier) {
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        speed.removeModifier(SLOW_MODIFIER_ID);
        speed.addTransientModifier(new AttributeModifier(
                SLOW_MODIFIER_ID,
                "tdmc_shadowstep_local_slow",
                Math.max(-0.99D, multiplier - 1.0D),
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));
    }

    private static void removeMovementModifier(Mob mob) {
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SLOW_MODIFIER_ID);
    }
}
