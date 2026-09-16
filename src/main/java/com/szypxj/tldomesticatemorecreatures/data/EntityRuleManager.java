package com.szypxj.tldomesticatemorecreatures.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.szypxj.tldomesticatemorecreatures.config.StatConfigManager;
import com.szypxj.tldomesticatemorecreatures.config.StatOperation;
import com.szypxj.tldomesticatemorecreatures.config.StatOverride;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EntityRuleManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final EntityRuleManager INSTANCE = new EntityRuleManager();
    private volatile Map<ResourceLocation, EntityRule> rules = Map.of();

    private EntityRuleManager() {
        super(GSON, "entity_rules");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, EntityRule> loaded = new LinkedHashMap<>();
        for (JsonElement element : objects.values()) {
            if (!element.isJsonObject()) {
                continue;
            }
            EntityRule rule = parse(element.getAsJsonObject());
            ResourceLocation entityId = ResourceLocation.tryParse(rule.entity());
            if (entityId != null) {
                loaded.put(entityId, rule);
            }
        }
        rules = Collections.unmodifiableMap(loaded);
        StatConfigManager.reload();
    }

    public EntityRule get(ResourceLocation entityId) {
        EntityRule rule = rules.get(entityId);
        return rule == null ? EntityRule.empty(entityId.toString()) : rule;
    }

    public Map<ResourceLocation, EntityRule> all() {
        return rules;
    }

    private static EntityRule parse(JsonObject object) {
        String entity = string(object, "entity", "");
        Boolean enabled = nullableBoolean(object, "enabled");
        Integer minLevel = nullableInt(object, "minLevel");
        Integer maxLevel = nullableInt(object, "maxLevel");
        Integer fixedLevel = nullableInt(object, "fixedLevel");
        Double maxLevelMultiplier = nullableDouble(object, "maxLevelMultiplier");
        Double killExperienceMultiplier = nullableDouble(object, "killExperienceMultiplier");
        Boolean canGainExperience = nullableBoolean(object, "canGainExperience");
        Boolean canLevelUp = nullableBoolean(object, "canLevelUp");
        Double resistanceCap = nullableDouble(object, "resistanceCap");
        Map<String, Double> randomWeights = doubleMap(object, "randomWeights");
        Map<String, Integer> fixedStats = intMap(object, "fixedStats");
        Map<String, StatOverride> statOverrides = overrideMap(object, "statOverrides");
        return new EntityRule(entity, enabled, minLevel, maxLevel, fixedLevel, maxLevelMultiplier, killExperienceMultiplier, canGainExperience, canLevelUp, resistanceCap, randomWeights, fixedStats, statOverrides);
    }

    private static Map<String, Double> doubleMap(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonObject()) {
            return Map.of();
        }
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject(key).entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                result.put(entry.getKey(), entry.getValue().getAsDouble());
            }
        }
        return Map.copyOf(result);
    }

    private static Map<String, Integer> intMap(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonObject()) {
            return Map.of();
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject(key).entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                result.put(entry.getKey(), Math.max(0, entry.getValue().getAsInt()));
            }
        }
        return Map.copyOf(result);
    }

    private static Map<String, StatOverride> overrideMap(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonObject()) {
            return Map.of();
        }
        Map<String, StatOverride> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject(key).entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject value = entry.getValue().getAsJsonObject();
            StatOperation operation = null;
            if (value.has("operation")) {
                try {
                    operation = StatOperation.valueOf(value.get("operation").getAsString().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }
            result.put(entry.getKey(), new StatOverride(
                    nullableString(value, "nameKey"),
                    nullableString(value, "icon"),
                    nullableString(value, "target"),
                    nullableDouble(value, "valuePerPoint"),
                    operation,
                    nullableInt(value, "maxPoints"),
                    nullableBoolean(value, "wildRandom"),
                    nullableBoolean(value, "showPlayer"),
                    nullableBoolean(value, "showMob"),
                    nullableDouble(value, "randomWeight")
            ));
        }
        return Map.copyOf(result);
    }

    private static String string(JsonObject object, String key, String fallback) {
        String value = nullableString(object, key);
        return value == null ? fallback : value;
    }

    private static String nullableString(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : null;
    }

    private static Integer nullableInt(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsInt() : null;
    }

    private static Double nullableDouble(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsDouble() : null;
    }

    private static Boolean nullableBoolean(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsBoolean() : null;
    }
}
