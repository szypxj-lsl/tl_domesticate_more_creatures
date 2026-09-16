package com.szypxj.tldomesticatemorecreatures.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.RandomSource;
import net.minecraftforge.fml.loading.FMLPaths;

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
import java.util.Set;

public final class TalentConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("tl_domesticate_more_creatures").resolve("talents.json");
    private static volatile List<TalentDefinition> definitions = defaultDefinitions();
    private static volatile Map<String, TalentDefinition> byId = index(definitions);

    private TalentConfigManager() {
    }

    public static synchronized void reload() {
        ensureFile();
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonArray()) {
                useDefaults();
                return;
            }
            Map<String, TalentDefinition> loaded = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                TalentDefinition definition = parse(element.getAsJsonObject());
                if (definition != null) {
                    loaded.put(definition.id(), definition);
                }
            }
            boolean upgraded = upgradeLegacyDefinitions(loaded);
            definitions = List.copyOf(loaded.values());
            byId = Map.copyOf(loaded);
            if (upgraded) {
                writeDefinitions(definitions);
            }
        } catch (IOException | RuntimeException exception) {
            useDefaults();
        }
    }

    public static List<TalentDefinition> all() {
        return definitions;
    }

    public static TalentDefinition byId(String id) {
        return byId.get(id);
    }

    public static TalentDefinition weightedRandom(RandomSource random, Set<String> excluded) {
        double totalWeight = 0.0D;
        for (TalentDefinition definition : definitions) {
            if (!excluded.contains(definition.id()) && definition.weight() > 0.0D) {
                totalWeight += definition.weight();
            }
        }
        if (totalWeight <= 0.0D) {
            return null;
        }
        double roll = random.nextDouble() * totalWeight;
        TalentDefinition fallback = null;
        for (TalentDefinition definition : definitions) {
            if (excluded.contains(definition.id()) || definition.weight() <= 0.0D) {
                continue;
            }
            fallback = definition;
            roll -= definition.weight();
            if (roll <= 0.0D) {
                return definition;
            }
        }
        return fallback;
    }

    public static int randomLevel(TalentDefinition definition, RandomSource random) {
        int min = definition.minLevel();
        int max = definition.maxLevel();
        if (max <= min) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }

    private static TalentDefinition parse(JsonObject object) {
        String id = string(object, "id", "");
        if (id.isBlank()) {
            return null;
        }
        String nameKey = string(object, "nameKey", "talent.tl_domesticate_more_creatures." + id);
        double weight = Math.max(0.0D, number(object, "weight", 1.0D));
        int minLevel = Math.max(1, integer(object, "minLevel", 1));
        int maxLevel = Math.max(minLevel, integer(object, "maxLevel", 3));
        Map<Integer, Map<String, Integer>> levels = parseLevels(object, minLevel, maxLevel);
        return new TalentDefinition(id, nameKey, weight, minLevel, maxLevel, levels);
    }

    private static Map<Integer, Map<String, Integer>> parseLevels(JsonObject object, int minLevel, int maxLevel) {
        if (!object.has("levels") || !object.get("levels").isJsonObject()) {
            return Map.of();
        }
        Map<Integer, Map<String, Integer>> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> levelEntry : object.getAsJsonObject("levels").entrySet()) {
            int level;
            try {
                level = Integer.parseInt(levelEntry.getKey());
            } catch (NumberFormatException exception) {
                continue;
            }
            if (level < minLevel || level > maxLevel || !levelEntry.getValue().isJsonObject()) {
                continue;
            }
            Map<String, Integer> effects = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> effect : levelEntry.getValue().getAsJsonObject().entrySet()) {
                if (effect.getValue().isJsonPrimitive()) {
                    int points = Math.max(0, effect.getValue().getAsInt());
                    if (points > 0) {
                        String statId = "armor".equals(effect.getKey()) ? "resistance" : effect.getKey();
                        effects.merge(statId, points, Integer::sum);
                    }
                }
            }
            result.put(level, Map.copyOf(effects));
        }
        return Map.copyOf(result);
    }

    private static void ensureFile() {
        if (Files.isRegularFile(CONFIG_PATH)) {
            return;
        }
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            writeDefinitions(defaultDefinitions());
        } catch (IOException ignored) {
        }
    }

    private static List<TalentDefinition> defaultDefinitions() {
        List<TalentDefinition> result = new ArrayList<>();
        result.add(definition("vitality", "talent.tl_domesticate_more_creatures.vitality", 1.0D, Map.of("health", 4)));
        result.add(definition("warbreaker", "talent.tl_domesticate_more_creatures.warbreaker", 1.0D, Map.of("damage", 4)));
        result.add(definition("windchaser", "talent.tl_domesticate_more_creatures.windchaser", 1.0D, Map.of("speed", 8)));
        result.add(definition("wavewalker", "talent.tl_domesticate_more_creatures.wavewalker", 1.0D, Map.of("swim_speed", 6)));
        result.add(definition("tenacity", "talent.tl_domesticate_more_creatures.tenacity", 1.0D, Map.of("resistance", 4)));
        result.add(definition("valor", "talent.tl_domesticate_more_creatures.valor", 1.0D, Map.of("health", 2, "damage", 2)));
        result.add(definition("windhunter", "talent.tl_domesticate_more_creatures.windhunter", 1.0D, Map.of("damage", 2, "speed", 4)));
        result.add(definition("tideborn", "talent.tl_domesticate_more_creatures.tideborn", 1.0D, Map.of("speed", 4, "swim_speed", 3)));
        result.add(definition("ironheart", "talent.tl_domesticate_more_creatures.ironheart", 1.0D, Map.of("health", 2, "resistance", 2)));
        result.add(definition("seaguard", "talent.tl_domesticate_more_creatures.seaguard", 1.0D, Map.of("swim_speed", 3, "resistance", 2)));
        return List.copyOf(result);
    }

    private static boolean upgradeLegacyDefinitions(Map<String, TalentDefinition> loaded) {
        Set<String> legacy = Set.of("fierce", "defender", "relentless", "shadowless", "peerless", "traceless");
        boolean changed = false;
        for (String id : legacy) {
            changed |= loaded.remove(id) != null;
        }
        for (TalentDefinition definition : defaultDefinitions()) {
            if (!loaded.containsKey(definition.id())) {
                loaded.put(definition.id(), definition);
                changed = true;
            }
        }
        return changed;
    }

    private static void writeDefinitions(List<TalentDefinition> values) throws IOException {
        Files.createDirectories(CONFIG_PATH.getParent());
        JsonArray array = new JsonArray();
        for (TalentDefinition definition : values) {
            JsonObject object = new JsonObject();
            object.addProperty("id", definition.id());
            object.addProperty("nameKey", definition.nameKey());
            object.addProperty("weight", definition.weight());
            object.addProperty("minLevel", definition.minLevel());
            object.addProperty("maxLevel", definition.maxLevel());
            JsonObject levels = new JsonObject();
            definition.levels().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        JsonObject effects = new JsonObject();
                        entry.getValue().forEach((key, value) -> effects.addProperty(key, value));
                        levels.add(Integer.toString(entry.getKey()), effects);
                    });
            object.add("levels", levels);
            array.add(object);
        }
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
            GSON.toJson(array, writer);
        }
    }

    private static TalentDefinition definition(String id, String nameKey, double weight, Map<String, Integer> perLevel) {
        Map<Integer, Map<String, Integer>> levels = new LinkedHashMap<>();
        for (int level = 1; level <= 3; level++) {
            Map<String, Integer> effects = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> entry : perLevel.entrySet()) {
                effects.put(entry.getKey(), entry.getValue() * level);
            }
            levels.put(level, Map.copyOf(effects));
        }
        return new TalentDefinition(id, nameKey, weight, 1, 3, Map.copyOf(levels));
    }

    private static Map<String, TalentDefinition> index(List<TalentDefinition> list) {
        Map<String, TalentDefinition> result = new LinkedHashMap<>();
        for (TalentDefinition definition : list) {
            result.put(definition.id(), definition);
        }
        return Map.copyOf(result);
    }

    private static void useDefaults() {
        definitions = defaultDefinitions();
        byId = index(definitions);
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
}
