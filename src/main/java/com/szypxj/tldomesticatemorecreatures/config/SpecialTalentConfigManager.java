package com.szypxj.tldomesticatemorecreatures.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.szypxj.tldomesticatemorecreatures.talent.SpecialTalentIds;
import com.szypxj.tldomesticatemorecreatures.talent.active.ActiveTalentIds;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SpecialTalentConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve("tl_domesticate_more_creatures")
            .resolve("special_talents.json");

    private static volatile Snapshot snapshot = defaults();

    private SpecialTalentConfigManager() {
    }

    public static synchronized void reload() {
        ensureFile();
        try {
            JsonObject root;
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) {
                    snapshot = defaults();
                    return;
                }
                root = parsed.getAsJsonObject();
            }
            if (upgradeMissingDefaults(root)) {
                writeRoot(root);
            }
            snapshot = parse(root);
        } catch (IOException | RuntimeException exception) {
            snapshot = defaults();
        }
    }

    public static List<SpecialTalentDefinition> all() {
        return snapshot.definitions();
    }

    public static SpecialTalentDefinition byId(String id) {
        return snapshot.byId().get(id);
    }

    public static BloodthirstySettings bloodthirsty() {
        return snapshot.bloodthirsty();
    }

    public static FurySettings fury() {
        return snapshot.fury();
    }

    private static Snapshot parse(JsonObject root) {
        Snapshot defaults = defaults();
        Map<String, SpecialTalentDefinition> definitions = new LinkedHashMap<>();
        for (SpecialTalentDefinition fallback : defaults.definitions()) {
            JsonObject object = root.has(fallback.id()) && root.get(fallback.id()).isJsonObject()
                    ? root.getAsJsonObject(fallback.id())
                    : new JsonObject();
            SpecialTalentDefinition definition = definition(object, fallback);
            definitions.put(definition.id(), definition);
        }

        JsonObject bloodObject = root.has(SpecialTalentIds.BLOODTHIRSTY) && root.get(SpecialTalentIds.BLOODTHIRSTY).isJsonObject()
                ? root.getAsJsonObject(SpecialTalentIds.BLOODTHIRSTY)
                : new JsonObject();
        JsonObject furyObject = root.has(SpecialTalentIds.FURY) && root.get(SpecialTalentIds.FURY).isJsonObject()
                ? root.getAsJsonObject(SpecialTalentIds.FURY)
                : new JsonObject();

        BloodthirstySettings blood = new BloodthirstySettings(
                positiveInt(bloodObject, "bleedDurationSeconds", defaults.bloodthirsty().bleedDurationSeconds()),
                nonNegative(bloodObject, "bleedCurrentHealthPercentPerSecond", defaults.bloodthirsty().bleedCurrentHealthPercentPerSecond())
        );
        FurySettings fury = new FurySettings(
                nonNegative(furyObject, "angerMaxHealthRatio", defaults.fury().angerMaxHealthRatio()),
                positiveInt(furyObject, "calmDelaySeconds", defaults.fury().calmDelaySeconds()),
                nonNegative(furyObject, "calmDecayPercentPerSecond", defaults.fury().calmDecayPercentPerSecond()),
                nonNegative(furyObject, "berserkDecayPercentPerSecond", defaults.fury().berserkDecayPercentPerSecond()),
                nonNegative(furyObject, "berserkDamageRageGainMultiplier", defaults.fury().berserkDamageRageGainMultiplier()),
                nonNegative(furyObject, "normalOutgoingDamageMultiplier", defaults.fury().normalOutgoingDamageMultiplier()),
                nonNegative(furyObject, "berserkOutgoingDamageMultiplier", defaults.fury().berserkOutgoingDamageMultiplier()),
                nonNegative(furyObject, "incomingDamageMultiplier", defaults.fury().incomingDamageMultiplier()),
                nonNegative(furyObject, "targetRadius", defaults.fury().targetRadius())
        );
        return new Snapshot(List.copyOf(definitions.values()), Map.copyOf(definitions), blood, fury);
    }

    private static SpecialTalentDefinition definition(JsonObject object, SpecialTalentDefinition fallback) {
        return new SpecialTalentDefinition(
                fallback.id(),
                string(object, "nameKey", fallback.nameKey()),
                bool(object, "enabled", fallback.enabled()),
                entityIds(object, "allowedEntities", fallback.allowedEntities()),
                chance(object, "spawnChance", fallback.spawnChance()),
                chance(object, "inheritOneParentChance", fallback.inheritOneParentChance()),
                chance(object, "inheritBothParentsChance", fallback.inheritBothParentsChance())
        );
    }

    private static Snapshot defaults() {
        List<SpecialTalentDefinition> definitions = List.of(
                new SpecialTalentDefinition(
                        SpecialTalentIds.BLOODTHIRSTY,
                        "talent.tl_domesticate_more_creatures.special.bloodthirsty",
                        true,
                        Set.of(),
                        0.05D,
                        0.25D,
                        0.50D
                ),
                new SpecialTalentDefinition(
                        SpecialTalentIds.FURY,
                        "talent.tl_domesticate_more_creatures.special.fury",
                        true,
                        Set.of(),
                        0.05D,
                        0.25D,
                        0.50D
                ),
                new SpecialTalentDefinition(
                        ActiveTalentIds.SHADOWSTEP,
                        "talent.tl_domesticate_more_creatures.special.shadowstep",
                        true,
                        Set.of(),
                        1.0D,
                        1.0D,
                        1.0D
                ),
                new SpecialTalentDefinition(
                        ActiveTalentIds.CAMOUFLAGE,
                        "talent.tl_domesticate_more_creatures.special.camouflage",
                        true,
                        Set.of(),
                        1.0D,
                        1.0D,
                        1.0D
                )
        );
        Map<String, SpecialTalentDefinition> byId = new LinkedHashMap<>();
        for (SpecialTalentDefinition definition : definitions) {
            byId.put(definition.id(), definition);
        }
        return new Snapshot(
                definitions,
                Map.copyOf(byId),
                new BloodthirstySettings(5, 0.01D),
                new FurySettings(0.50D, 10, 0.01D, 0.01D, 0.20D, 1.20D, 1.30D, 1.20D, 16.0D)
        );
    }

    private static void ensureFile() {
        if (Files.isRegularFile(CONFIG_PATH)) {
            return;
        }
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            writeRoot(defaultRoot());
        } catch (IOException ignored) {
        }
    }

    private static boolean upgradeMissingDefaults(JsonObject root) {
        boolean changed = false;
        Snapshot defaults = defaults();
        for (SpecialTalentDefinition definition : defaults.definitions()) {
            if (!root.has(definition.id()) || !root.get(definition.id()).isJsonObject()) {
                root.add(definition.id(), definitionObject(definition, defaults));
                changed = true;
            }
        }
        return changed;
    }

    private static JsonObject defaultRoot() {
        Snapshot defaults = defaults();
        JsonObject root = new JsonObject();
        for (SpecialTalentDefinition definition : defaults.definitions()) {
            root.add(definition.id(), definitionObject(definition, defaults));
        }
        return root;
    }

    private static JsonObject definitionObject(SpecialTalentDefinition definition, Snapshot defaults) {
        JsonObject object = new JsonObject();
        object.addProperty("enabled", definition.enabled());
        object.addProperty("nameKey", definition.nameKey());
        JsonArray entities = new JsonArray();
        definition.allowedEntities().forEach(entities::add);
        object.add("allowedEntities", entities);
        object.addProperty("spawnChance", definition.spawnChance());
        object.addProperty("inheritOneParentChance", definition.inheritOneParentChance());
        object.addProperty("inheritBothParentsChance", definition.inheritBothParentsChance());
        if (SpecialTalentIds.BLOODTHIRSTY.equals(definition.id())) {
            object.addProperty("bleedDurationSeconds", defaults.bloodthirsty().bleedDurationSeconds());
            object.addProperty("bleedCurrentHealthPercentPerSecond", defaults.bloodthirsty().bleedCurrentHealthPercentPerSecond());
        } else if (SpecialTalentIds.FURY.equals(definition.id())) {
            FurySettings fury = defaults.fury();
            object.addProperty("angerMaxHealthRatio", fury.angerMaxHealthRatio());
            object.addProperty("calmDelaySeconds", fury.calmDelaySeconds());
            object.addProperty("calmDecayPercentPerSecond", fury.calmDecayPercentPerSecond());
            object.addProperty("berserkDecayPercentPerSecond", fury.berserkDecayPercentPerSecond());
            object.addProperty("berserkDamageRageGainMultiplier", fury.berserkDamageRageGainMultiplier());
            object.addProperty("normalOutgoingDamageMultiplier", fury.normalOutgoingDamageMultiplier());
            object.addProperty("berserkOutgoingDamageMultiplier", fury.berserkOutgoingDamageMultiplier());
            object.addProperty("incomingDamageMultiplier", fury.incomingDamageMultiplier());
            object.addProperty("targetRadius", fury.targetRadius());
        }
        return object;
    }

    private static void writeRoot(JsonObject root) throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : fallback;
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsBoolean() : fallback;
    }

    private static double chance(JsonObject object, String key, double fallback) {
        return Math.max(0.0D, Math.min(1.0D, nonNegative(object, key, fallback)));
    }

    private static double nonNegative(JsonObject object, String key, double fallback) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            return Math.max(0.0D, fallback);
        }
        return Math.max(0.0D, object.get(key).getAsDouble());
    }

    private static int positiveInt(JsonObject object, String key, int fallback) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            return Math.max(1, fallback);
        }
        return Math.max(1, object.get(key).getAsInt());
    }

    private static Set<String> entityIds(JsonObject object, String key, Set<String> fallback) {
        if (!object.has(key) || !object.get(key).isJsonArray()) {
            return fallback;
        }
        Set<String> result = new LinkedHashSet<>();
        for (JsonElement element : object.getAsJsonArray(key)) {
            if (element.isJsonPrimitive()) {
                String value = element.getAsString();
                net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(value);
                if (id != null) {
                    result.add(id.toString());
                }
            }
        }
        return Set.copyOf(result);
    }

    public record BloodthirstySettings(int bleedDurationSeconds, double bleedCurrentHealthPercentPerSecond) {
        public BloodthirstySettings {
            bleedDurationSeconds = Math.max(1, bleedDurationSeconds);
            bleedCurrentHealthPercentPerSecond = Math.max(0.0D, bleedCurrentHealthPercentPerSecond);
        }
    }

    public record FurySettings(
            double angerMaxHealthRatio,
            int calmDelaySeconds,
            double calmDecayPercentPerSecond,
            double berserkDecayPercentPerSecond,
            double berserkDamageRageGainMultiplier,
            double normalOutgoingDamageMultiplier,
            double berserkOutgoingDamageMultiplier,
            double incomingDamageMultiplier,
            double targetRadius
    ) {
        public FurySettings {
            angerMaxHealthRatio = Math.max(0.0D, angerMaxHealthRatio);
            calmDelaySeconds = Math.max(1, calmDelaySeconds);
            calmDecayPercentPerSecond = Math.max(0.0D, calmDecayPercentPerSecond);
            berserkDecayPercentPerSecond = Math.max(0.0D, berserkDecayPercentPerSecond);
            berserkDamageRageGainMultiplier = Math.max(0.0D, berserkDamageRageGainMultiplier);
            normalOutgoingDamageMultiplier = Math.max(0.0D, normalOutgoingDamageMultiplier);
            berserkOutgoingDamageMultiplier = Math.max(0.0D, berserkOutgoingDamageMultiplier);
            incomingDamageMultiplier = Math.max(0.0D, incomingDamageMultiplier);
            targetRadius = Math.max(0.0D, targetRadius);
        }
    }

    private record Snapshot(
            List<SpecialTalentDefinition> definitions,
            Map<String, SpecialTalentDefinition> byId,
            BloodthirstySettings bloodthirsty,
            FurySettings fury
    ) {
    }
}
