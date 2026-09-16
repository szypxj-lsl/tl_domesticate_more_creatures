package com.szypxj.tldomesticatemorecreatures.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;
import com.szypxj.tldomesticatemorecreatures.game.AttributeService;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StatConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("tl_domesticate_more_creatures").resolve("attributes.json");
    private static volatile List<StatDefinition> definitions = defaultDefinitions();

    private StatConfigManager() {
    }

    public static synchronized void reload() {
        ensureFile();
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonArray()) {
                definitions = defaultDefinitions();
                return;
            }
            Map<String, StatDefinition> loaded = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                StatDefinition definition = parse(element.getAsJsonObject());
                if (definition != null) {
                    loaded.put(definition.id(), definition);
                }
            }
            boolean upgraded = ensureRequiredDefinitions(loaded);
            definitions = loaded.isEmpty() ? defaultDefinitions() : List.copyOf(loaded.values());
            if (upgraded) {
                writeDefinitions(definitions);
            }
        } catch (IOException | RuntimeException exception) {
            definitions = defaultDefinitions();
        } finally {
            AttributeService.clearDefinitionCache();
        }
    }

    public static List<StatDefinition> all() {
        return definitions;
    }

    public static StatDefinition byId(String id) {
        for (StatDefinition definition : definitions) {
            if (definition.id().equals(id)) {
                return definition;
            }
        }
        return null;
    }

    private static boolean ensureRequiredDefinitions(Map<String, StatDefinition> loaded) {
        boolean changed = loaded.remove("armor") != null;

        StatDefinition torpor = loaded.get("torpor");
        if (torpor == null) {
            for (StatDefinition definition : defaultDefinitions()) {
                if ("torpor".equals(definition.id())) {
                    loaded.put(definition.id(), definition);
                    changed = true;
                    break;
                }
            }
        } else {
            StatDefinition canonical = canonicalTorporDefinition(torpor);
            if (!canonical.equals(torpor)) {
                loaded.put("torpor", canonical);
                changed = true;
            }
        }

        StatDefinition swimSpeed = loaded.get("swim_speed");
        if (swimSpeed != null) {
            StatDefinition canonical = canonicalSwimSpeedDefinition(swimSpeed);
            if (!canonical.equals(swimSpeed)) {
                loaded.put("swim_speed", canonical);
                changed = true;
            }
        }
        return changed;
    }

    private static StatDefinition canonicalTorporDefinition(StatDefinition definition) {
        return new StatDefinition(
                "torpor",
                definition.nameKey(),
                definition.icon(),
                AttributeService.TORPOR_TARGET,
                0.0D,
                StatOperation.ADDITION,
                0,
                false,
                definition.showPlayer(),
                definition.showMob(),
                0.0D
        );
    }


    private static StatDefinition canonicalSwimSpeedDefinition(StatDefinition definition) {
        return new StatDefinition(
                definition.id(),
                definition.nameKey(),
                definition.icon(),
                definition.target(),
                definition.valuePerPoint(),
                definition.operation(),
                definition.maxPoints(),
                false,
                definition.showPlayer(),
                definition.showMob(),
                0.0D
        );
    }

    private static void writeDefinitions(List<StatDefinition> values) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonArray array = new JsonArray();
            for (StatDefinition definition : values) {
                JsonObject object = new JsonObject();
                object.addProperty("id", definition.id());
                object.addProperty("nameKey", definition.nameKey());
                object.addProperty("icon", definition.icon());
                object.addProperty("target", definition.target());
                object.addProperty("valuePerPoint", definition.valuePerPoint());
                object.addProperty("operation", definition.operation().name());
                object.addProperty("maxPoints", definition.maxPoints());
                object.addProperty("wildRandom", definition.wildRandom());
                object.addProperty("showPlayer", definition.showPlayer());
                object.addProperty("showMob", definition.showMob());
                object.addProperty("randomWeight", definition.randomWeight());
                array.add(object);
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(array, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static StatDefinition parse(JsonObject object) {
        String id = string(object, "id", "");
        String nameKey = string(object, "nameKey", "stat.tl_domesticate_more_creatures." + id);
        String icon = string(object, "icon", "minecraft:stone");
        String target = string(object, "target", "");
        double valuePerPoint = number(object, "valuePerPoint", 0.0D);
        StatOperation operation;
        try {
            operation = StatOperation.valueOf(string(object, "operation", "ADDITION").toUpperCase());
        } catch (IllegalArgumentException exception) {
            operation = StatOperation.ADDITION;
        }
        int maxPoints = integer(object, "maxPoints", -1);
        boolean wildRandom = bool(object, "wildRandom", true);
        boolean showPlayer = bool(object, "showPlayer", true);
        boolean showMob = bool(object, "showMob", true);
        double randomWeight = Math.max(0.0D, number(object, "randomWeight", 1.0D));
        if (id.isBlank() || target.isBlank()) {
            return null;
        }
        return new StatDefinition(id, nameKey, icon, target, valuePerPoint, operation, maxPoints, wildRandom, showPlayer, showMob, randomWeight);
    }

    private static void ensureFile() {
        if (!Files.isRegularFile(CONFIG_PATH)) {
            writeDefinitions(defaultDefinitions());
        }
    }

    private static List<StatDefinition> defaultDefinitions() {
        List<StatDefinition> result = new ArrayList<>();
        result.add(new StatDefinition("health", "stat.tl_domesticate_more_creatures.health", "minecraft:red_dye", "minecraft:generic.max_health", 0.05D, StatOperation.MULTIPLY_BASE, -1, true, true, true, 1.0D));
        result.add(new StatDefinition("damage", "stat.tl_domesticate_more_creatures.damage", "minecraft:iron_sword", "tl_domesticate_more_creatures:damage_multiplier", 0.05D, StatOperation.MULTIPLY_TOTAL, -1, true, true, true, 1.0D));
        result.add(new StatDefinition("speed", "stat.tl_domesticate_more_creatures.speed", "minecraft:sugar", "minecraft:generic.movement_speed", 0.01D, StatOperation.MULTIPLY_BASE, -1, true, true, true, 1.0D));
        result.add(new StatDefinition("swim_speed", "stat.tl_domesticate_more_creatures.swim_speed", "minecraft:heart_of_the_sea", "forge:swim_speed", 0.02D, StatOperation.MULTIPLY_BASE, -1, false, true, true, 0.0D));
        result.add(new StatDefinition("resistance", "stat.tl_domesticate_more_creatures.resistance", "minecraft:shield", "tl_domesticate_more_creatures:resistance", 0.01D, StatOperation.ADDITION, -1, true, true, true, 1.0D));
        result.add(new StatDefinition("torpor", "stat.tl_domesticate_more_creatures.torpor", "minecraft:fermented_spider_eye", "tl_domesticate_more_creatures:torpor", 0.0D, StatOperation.ADDITION, 0, false, true, true, 0.0D));
        return List.copyOf(result);
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : fallback;
    }

    private static double number(JsonObject object, String key, double fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsDouble() : fallback;
    }

    private static int integer(JsonObject object, String key, int fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsInt() : fallback;
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsBoolean() : fallback;
    }
}
