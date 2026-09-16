package com.szypxj.tldomesticatemorecreatures.talent.active;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ActiveTalentConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve("tl_domesticate_more_creatures")
            .resolve("active_talents.json");
    private static volatile ActiveTalentConfig.Snapshot snapshot = ActiveTalentConfig.DEFAULT;

    private ActiveTalentConfigManager() {
    }

    public static synchronized void reload() {
        ensureFile();
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            snapshot = parsed.isJsonObject() ? parse(parsed.getAsJsonObject()) : ActiveTalentConfig.DEFAULT;
        } catch (IOException | RuntimeException exception) {
            snapshot = ActiveTalentConfig.DEFAULT;
        }
    }

    public static ActiveTalentConfig.Snapshot snapshot() {
        return snapshot;
    }

    public static ActiveTalentConfig.ShadowstepConfig shadowstep() {
        return snapshot.shadowstep();
    }

    public static ActiveTalentConfig.CamouflageConfig camouflage() {
        return snapshot.camouflage();
    }

    private static ActiveTalentConfig.Snapshot parse(JsonObject root) {
        JsonObject shadow = object(root, ActiveTalentIds.SHADOWSTEP);
        JsonObject camo = object(root, ActiveTalentIds.CAMOUFLAGE);
        ActiveTalentConfig.ShadowstepConfig sd = ActiveTalentConfig.DEFAULT_SHADOWSTEP;
        ActiveTalentConfig.CamouflageConfig cd = ActiveTalentConfig.DEFAULT_CAMOUFLAGE;
        return new ActiveTalentConfig.Snapshot(
                new ActiveTalentConfig.ShadowstepConfig(
                        secondsToTicks(nonNegativeDouble(shadow, "markingDurationSeconds", sd.markingDurationTicks() / 20.0D), sd.markingDurationTicks()),
                        positiveDouble(shadow, "slowRadius", sd.slowRadius()),
                        boundedMultiplier(shadow, "enemyActionMultiplier", sd.enemyActionMultiplier()),
                        positiveDouble(shadow, "markRange", sd.markRange()),
                        positiveInt(shadow, "maxMarks", sd.maxMarks()),
                        positiveDouble(shadow, "dashBlocksPerTick", sd.dashBlocksPerTick()),
                        secondsToTicks(nonNegativeDouble(shadow, "cooldownSeconds", sd.cooldownSeconds()), sd.cooldownTicks()),
                        boundedMultiplier(shadow, "refundPerUnusedMark", sd.refundPerUnusedMark())
                ),
                new ActiveTalentConfig.CamouflageConfig(
                        secondsToTicks(nonNegativeDouble(camo, "maxDurationSeconds", cd.maxDurationTicks() / 20.0D), cd.maxDurationTicks()),
                        boundedMultiplier(camo, "enemyDetectionMultiplier", cd.enemyDetectionMultiplier()),
                        positiveDouble(camo, "ambushDamageMultiplier", cd.ambushDamageMultiplier()),
                        secondsToTicks(nonNegativeDouble(camo, "cooldownSeconds", cd.cooldownSeconds()), cd.cooldownTicks()),
                        (float) boundedMultiplier(camo, "renderAlpha", cd.renderAlpha())
                )
        );
    }

    private static void ensureFile() {
        if (Files.isRegularFile(CONFIG_PATH)) {
            return;
        }
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject root = new JsonObject();
            JsonObject shadow = new JsonObject();
            ActiveTalentConfig.ShadowstepConfig sd = ActiveTalentConfig.DEFAULT_SHADOWSTEP;
            shadow.addProperty("markingDurationSeconds", sd.markingDurationTicks() / 20.0D);
            shadow.addProperty("slowRadius", sd.slowRadius());
            shadow.addProperty("enemyActionMultiplier", sd.enemyActionMultiplier());
            shadow.addProperty("markRange", sd.markRange());
            shadow.addProperty("maxMarks", sd.maxMarks());
            shadow.addProperty("dashBlocksPerTick", sd.dashBlocksPerTick());
            shadow.addProperty("cooldownSeconds", sd.cooldownSeconds());
            shadow.addProperty("refundPerUnusedMark", sd.refundPerUnusedMark());
            root.add(ActiveTalentIds.SHADOWSTEP, shadow);
            JsonObject camo = new JsonObject();
            ActiveTalentConfig.CamouflageConfig cd = ActiveTalentConfig.DEFAULT_CAMOUFLAGE;
            camo.addProperty("maxDurationSeconds", cd.maxDurationTicks() / 20.0D);
            camo.addProperty("enemyDetectionMultiplier", cd.enemyDetectionMultiplier());
            camo.addProperty("ambushDamageMultiplier", cd.ambushDamageMultiplier());
            camo.addProperty("cooldownSeconds", cd.cooldownSeconds());
            camo.addProperty("renderAlpha", cd.renderAlpha());
            root.add(ActiveTalentIds.CAMOUFLAGE, camo);
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static JsonObject object(JsonObject root, String key) {
        return root.has(key) && root.get(key).isJsonObject() ? root.getAsJsonObject(key) : new JsonObject();
    }

    private static int secondsToTicks(double seconds, int fallback) {
        if (!Double.isFinite(seconds) || seconds < 0.0D) {
            return Math.max(0, fallback);
        }
        return Math.max(0, (int) Math.round(Math.min(Integer.MAX_VALUE / 20.0D, seconds) * 20.0D));
    }

    private static int positiveInt(JsonObject object, String key, int fallback) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) return Math.max(1, fallback);
        int value = object.get(key).getAsInt();
        return value > 0 ? value : Math.max(1, fallback);
    }

    private static double positiveDouble(JsonObject object, String key, double fallback) {
        double value = number(object, key, fallback);
        return Double.isFinite(value) && value > 0.0D ? value : fallback;
    }

    private static double nonNegativeDouble(JsonObject object, String key, double fallback) {
        double value = number(object, key, fallback);
        return Double.isFinite(value) && value >= 0.0D ? value : fallback;
    }

    private static double boundedMultiplier(JsonObject object, String key, double fallback) {
        return Math.max(0.0D, Math.min(1.0D, nonNegativeDouble(object, key, fallback)));
    }

    private static double number(JsonObject object, String key, double fallback) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) return fallback;
        try {
            return object.get(key).getAsDouble();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}
