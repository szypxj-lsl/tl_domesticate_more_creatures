package com.szypxj.tldomesticatemorecreatures.game;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.config.StatConfigManager;
import com.szypxj.tldomesticatemorecreatures.config.StatDefinition;
import com.szypxj.tldomesticatemorecreatures.config.StatOperation;
import com.szypxj.tldomesticatemorecreatures.data.EntityRule;
import com.szypxj.tldomesticatemorecreatures.data.EntityRuleManager;
import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.imprint.ImprintService;
import com.szypxj.tldomesticatemorecreatures.torpor.TorporService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AttributeService {
    public static final String DAMAGE_MULTIPLIER_TARGET = "tl_domesticate_more_creatures:damage_multiplier";
    public static final String RESISTANCE_TARGET = "tl_domesticate_more_creatures:resistance";
    public static final String TORPOR_TARGET = "tl_domesticate_more_creatures:torpor";

    private static final Map<ResourceLocation, List<StatDefinition>> DEFINITION_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Set<ResourceLocation>> MANAGED_TARGET_HISTORY = new ConcurrentHashMap<>();
    private static final UUID BOND_HEALTH_MODIFIER_ID = UUID.nameUUIDFromBytes((TlDomesticateMoreCreatures.MOD_ID + ":imprint_bond_health").getBytes(StandardCharsets.UTF_8));

    private AttributeService() {
    }

    public static List<StatDefinition> definitionsFor(LivingEntity entity) {
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return definitionsFor(entityId);
    }

    public static List<StatDefinition> definitionsFor(ResourceLocation entityId) {
        if (entityId == null) {
            return buildDefinitions(EntityRule.empty(""));
        }
        return DEFINITION_CACHE.computeIfAbsent(
                entityId,
                id -> buildDefinitions(EntityRuleManager.INSTANCE.get(id))
        );
    }

    public static void clearDefinitionCache() {
        DEFINITION_CACHE.clear();
    }

    public static boolean isPointAssignable(StatDefinition definition) {
        return definition != null
                && !"torpor".equals(definition.id())
                && !TORPOR_TARGET.equals(definition.target());
    }

    public static List<StatDefinition> pointAssignableDefinitions(LivingEntity entity) {
        return definitionsFor(entity).stream().filter(AttributeService::isPointAssignable).toList();
    }

    public static List<StatDefinition> pointAssignableDefinitions(ResourceLocation entityId) {
        return definitionsFor(entityId).stream().filter(AttributeService::isPointAssignable).toList();
    }

    public static List<StatDefinition> autoAssignableDefinitions(LivingEntity entity) {
        return definitionsFor(entity).stream().filter(AttributeService::isAutoAssignable).toList();
    }

    public static List<StatDefinition> autoAssignableDefinitions(ResourceLocation entityId) {
        return definitionsFor(entityId).stream().filter(AttributeService::isAutoAssignable).toList();
    }

    private static boolean isAutoAssignable(StatDefinition definition) {
        return isPointAssignable(definition)
                && !"swim_speed".equals(definition.id())
                && definition.wildRandom()
                && definition.randomWeight() > 0.0D;
    }

    private static List<StatDefinition> buildDefinitions(EntityRule rule) {
        List<StatDefinition> result = new ArrayList<>();

        for (StatDefinition base : StatConfigManager.all()) {
            StatDefinition definition = base.merge(rule.statOverrides().get(base.id()));
            Double weight = rule.randomWeights().get(base.id());

            if (weight != null) {
                definition = new StatDefinition(
                        definition.id(),
                        definition.nameKey(),
                        definition.icon(),
                        definition.target(),
                        definition.valuePerPoint(),
                        definition.operation(),
                        definition.maxPoints(),
                        definition.wildRandom(),
                        definition.showPlayer(),
                        definition.showMob(),
                        Math.max(0.0D, weight)
                );
            }

            definition = normalizeVirtualTarget(definition);
            rememberManagedTarget(definition);
            result.add(definition);
        }

        return List.copyOf(result);
    }

    private static void rememberManagedTarget(StatDefinition definition) {
        if (DAMAGE_MULTIPLIER_TARGET.equals(definition.target())
                || RESISTANCE_TARGET.equals(definition.target())
                || TORPOR_TARGET.equals(definition.target())) {
            return;
        }

        ResourceLocation targetId = ResourceLocation.tryParse(definition.target());
        if (targetId == null) {
            return;
        }

        Set<ResourceLocation> targets = MANAGED_TARGET_HISTORY
                .computeIfAbsent(definition.id(), ignored -> ConcurrentHashMap.newKeySet());
        targets.add(targetId);
        if ("speed".equals(definition.id())) {
            ResourceLocation flyingSpeedId = ForgeRegistries.ATTRIBUTES.getKey(Attributes.FLYING_SPEED);
            if (flyingSpeedId != null) {
                targets.add(flyingSpeedId);
            }
        }
    }


    private static StatDefinition normalizeVirtualTarget(StatDefinition definition) {
        ResourceLocation targetId = ResourceLocation.tryParse(definition.target());
        if (targetId == null || ForgeRegistries.ATTRIBUTES.getValue(targetId) != null) {
            return definition;
        }

        String normalizedTarget = definition.target();
        if ("damage".equals(definition.id()) && "damage_multiplier".equals(targetId.getPath())) {
            normalizedTarget = DAMAGE_MULTIPLIER_TARGET;
        } else if ("resistance".equals(definition.id()) && "resistance".equals(targetId.getPath())) {
            normalizedTarget = RESISTANCE_TARGET;
        } else if ("torpor".equals(definition.id()) && "torpor".equals(targetId.getPath())) {
            normalizedTarget = TORPOR_TARGET;
        }

        if (normalizedTarget.equals(definition.target())) {
            return definition;
        }

        return new StatDefinition(
                definition.id(),
                definition.nameKey(),
                definition.icon(),
                normalizedTarget,
                definition.valuePerPoint(),
                definition.operation(),
                definition.maxPoints(),
                definition.wildRandom(),
                definition.showPlayer(),
                definition.showMob(),
                definition.randomWeight()
        );
    }

    public static boolean isTargetAvailable(LivingEntity entity, StatDefinition definition) {
        if (TORPOR_TARGET.equals(definition.target())) {
            return TorporService.isEnabled(entity);
        }
        if (DAMAGE_MULTIPLIER_TARGET.equals(definition.target())
                || RESISTANCE_TARGET.equals(definition.target())) {
            return true;
        }
        ResourceLocation id = ResourceLocation.tryParse(definition.target());
        if (id == null) {
            return false;
        }
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(id);
        return attribute != null && entity.getAttribute(attribute) != null;
    }

    public static boolean isVisible(LivingEntity entity, StatDefinition definition) {
        if (TORPOR_TARGET.equals(definition.target()) && !TorporService.isEnabled(entity)) {
            return false;
        }
        return entity instanceof net.minecraft.world.entity.player.Player ? definition.showPlayer() : definition.showMob();
    }

    public static int pointLimit(LivingEntity entity, StatDefinition definition) {
        if (!isPointAssignable(definition)) {
            return 0;
        }
        int limit = definition.maxPoints();
        if (RESISTANCE_TARGET.equals(definition.target()) && definition.valuePerPoint() > 0.0D) {
            EntityRule rule = LevelService.rule(entity);
            double cap = rule.resistanceCap() == null ? Config.RESISTANCE_CAP.get() : rule.resistanceCap();
            int resistanceLimit = cap <= 0.0D
                    ? 0
                    : (int) Math.min(Integer.MAX_VALUE, Math.ceil(cap / definition.valuePerPoint()));
            limit = limit < 0 ? resistanceLimit : Math.min(limit, resistanceLimit);
        }
        return limit;
    }

    public static boolean canReceivePoint(LivingEntity entity, StatDefinition definition, int currentRegularPoints) {
        if (!isPointAssignable(definition)) {
            return false;
        }
        int limit = pointLimit(entity, definition);
        return limit < 0 || currentRegularPoints < limit;
    }

    public static void applyAll(LivingEntity entity) {
        float oldMaxHealth = entity.getMaxHealth();
        float oldHealth = entity.getHealth();
        double healthRatio = oldMaxHealth > 0.0F ? Math.max(0.0D, Math.min(1.0D, oldHealth / oldMaxHealth)) : 1.0D;
        removeManagedModifiers(entity);
        removeBondHealthModifier(entity);
        if (!LevelService.isAffected(entity) || !ProgressData.exists(entity)) {
            restoreHealthRatio(entity, oldHealth, healthRatio);
            return;
        }
        ProgressData data = ProgressData.of(entity);
        for (StatDefinition definition : definitionsFor(entity)) {
            if (!isTargetAvailable(entity, definition)) {
                continue;
            }
            if (DAMAGE_MULTIPLIER_TARGET.equals(definition.target())
                    || RESISTANCE_TARGET.equals(definition.target())
                    || TORPOR_TARGET.equals(definition.target())) {
                continue;
            }
            ProgressData.StatPoints points = data.stat(definition.id());
            if (points.total() <= 0) {
                continue;
            }
            ResourceLocation targetId = ResourceLocation.tryParse(definition.target());
            if (targetId == null) {
                continue;
            }
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(targetId);
            if (attribute == null) {
                continue;
            }
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            double amount = definition.valuePerPoint() * points.total();
            AttributeModifier modifier = new AttributeModifier(
                    modifierId(definition.id()),
                    TlDomesticateMoreCreatures.MOD_ID + ":" + definition.id(),
                    amount,
                    operation(definition.operation())
            );
            instance.addTransientModifier(modifier);
            if ("speed".equals(definition.id())) {
                AttributeInstance flyingSpeed = entity.getAttribute(Attributes.FLYING_SPEED);
                if (flyingSpeed != null && flyingSpeed != instance) {
                    flyingSpeed.addTransientModifier(new AttributeModifier(
                            modifierId(definition.id()),
                            TlDomesticateMoreCreatures.MOD_ID + ":" + definition.id() + "_flying",
                            amount,
                            operation(definition.operation())
                    ));
                }
            }
        }
        double bondHealthMultiplier = ImprintService.bondHealthMultiplier(entity);
        if (bondHealthMultiplier > 1.0D) {
            AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.addTransientModifier(new AttributeModifier(
                        BOND_HEALTH_MODIFIER_ID,
                        TlDomesticateMoreCreatures.MOD_ID + ":imprint_bond_health",
                        bondHealthMultiplier - 1.0D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                ));
            }
        }
        restoreHealthRatio(entity, oldHealth, healthRatio);
    }

    public static double damageMultiplier(LivingEntity entity) {
        if (!LevelService.isAffected(entity) || !ProgressData.exists(entity)) {
            return 1.0D;
        }
        double additive = 0.0D;
        double product = 1.0D;
        ProgressData data = ProgressData.of(entity);
        for (StatDefinition definition : definitionsFor(entity)) {
            if (!DAMAGE_MULTIPLIER_TARGET.equals(definition.target())) {
                continue;
            }
            int points = data.stat(definition.id()).total();
            if (points <= 0) {
                continue;
            }
            double value = definition.valuePerPoint() * points;
            if (definition.operation() == StatOperation.MULTIPLY_TOTAL) {
                product *= 1.0D + value;
            } else {
                additive += value;
            }
        }
        return Math.max(0.0D, (1.0D + additive) * product * ImprintService.bondDamageMultiplier(entity));
    }

    public static double resistance(LivingEntity entity) {
        if (!LevelService.isAffected(entity) || !ProgressData.exists(entity)) {
            return 0.0D;
        }
        double regularValue = 0.0D;
        double talentValue = 0.0D;
        ProgressData data = ProgressData.of(entity);
        for (StatDefinition definition : definitionsFor(entity)) {
            if (!RESISTANCE_TARGET.equals(definition.target())) {
                continue;
            }
            ProgressData.StatPoints points = data.stat(definition.id());
            regularValue += definition.valuePerPoint() * points.regular();
            talentValue += definition.valuePerPoint() * points.talent();
        }
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        EntityRule rule = entityId == null ? EntityRule.empty("") : EntityRuleManager.INSTANCE.get(entityId);
        double cap = rule.resistanceCap() == null ? Config.RESISTANCE_CAP.get() : rule.resistanceCap();
        double cappedRegular = Math.min(Math.max(0.0D, cap), Math.max(0.0D, regularValue));
        return Math.max(0.0D, cappedRegular + Math.max(0.0D, talentValue));
    }

    public static double displayedValue(LivingEntity entity, StatDefinition definition) {
        if (DAMAGE_MULTIPLIER_TARGET.equals(definition.target())) {
            return damageMultiplier(entity);
        }
        if (RESISTANCE_TARGET.equals(definition.target())) {
            return resistance(entity);
        }
        if (TORPOR_TARGET.equals(definition.target())) {
            return TorporService.maxTorpor(entity);
        }
        ResourceLocation targetId = ResourceLocation.tryParse(definition.target());
        if (targetId == null) {
            return 0.0D;
        }
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(targetId);
        if (attribute == null) {
            return 0.0D;
        }
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? 0.0D : instance.getValue();
    }

    private static AttributeModifier.Operation operation(StatOperation operation) {
        return switch (operation) {
            case ADDITION -> AttributeModifier.Operation.ADDITION;
            case MULTIPLY_BASE -> AttributeModifier.Operation.MULTIPLY_BASE;
            case MULTIPLY_TOTAL -> AttributeModifier.Operation.MULTIPLY_TOTAL;
        };
    }

    private static void restoreHealthRatio(LivingEntity entity, float oldHealth, double healthRatio) {
        float newMaxHealth = entity.getMaxHealth();
        if (oldHealth > 0.0F && newMaxHealth > 0.0F) {
            entity.setHealth((float) Math.max(0.0D, Math.min(newMaxHealth, newMaxHealth * healthRatio)));
        } else if (entity.getHealth() > newMaxHealth) {
            entity.setHealth(newMaxHealth);
        }
    }

    private static UUID modifierId(String statId) {
        return UUID.nameUUIDFromBytes((TlDomesticateMoreCreatures.MOD_ID + ":stat:" + statId).getBytes(StandardCharsets.UTF_8));
    }

    static void removeTdmcManagedModifiersForSnapshot(LivingEntity entity) {
        removeManagedModifiers(entity);
        removeBondHealthModifier(entity);
    }

    private static void removeBondHealthModifier(LivingEntity entity) {
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(BOND_HEALTH_MODIFIER_ID);
        }
    }

    private static void removeManagedModifiers(LivingEntity entity) {
        for (Map.Entry<String, Set<ResourceLocation>> entry : MANAGED_TARGET_HISTORY.entrySet()) {
            UUID expectedId = modifierId(entry.getKey());

            for (ResourceLocation targetId : entry.getValue()) {
                Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(targetId);
                if (attribute == null) {
                    continue;
                }

                AttributeInstance instance = entity.getAttribute(attribute);
                if (instance == null) {
                    continue;
                }

                for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
                    if (modifier.getId().equals(expectedId)) {
                        instance.removeModifier(modifier);
                        break;
                    }
                }
            }
        }
    }
}
