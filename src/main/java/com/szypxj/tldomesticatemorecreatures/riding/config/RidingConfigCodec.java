package com.szypxj.tldomesticatemorecreatures.riding.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.szypxj.tldomesticatemorecreatures.riding.RideMode;
import com.szypxj.tldomesticatemorecreatures.riding.RideMovementMode;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.Set;

public final class RidingConfigCodec {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private RidingConfigCodec() {
    }

    public static RidingSettings readSettings(Path path) throws IOException {
        JsonObject root = readObject(path);
        RideFilterMode mode = enumValue(root, "filterMode", RideFilterMode.class, RideFilterMode.BLACKLIST);
        Set<ResourceLocation> entries = new LinkedHashSet<>();
        JsonElement value = root.get("filterEntries");
        if (value != null && value.isJsonArray()) {
            for (JsonElement element : value.getAsJsonArray()) {
                if (!element.isJsonPrimitive()) {
                    continue;
                }
                ResourceLocation entry = ResourceLocation.tryParse(element.getAsString());
                if (entry == null) {
                    throw new IOException("Invalid filter entity id: " + element.getAsString());
                }
                entries.add(entry);
            }
        }
        return new RidingSettings(mode, entries).validated();
    }

    public static EntityRideProfile readProfile(Path path) throws IOException {
        JsonObject root = readObject(path);
        if (!root.has("entityId")) {
            throw new IOException("Missing entityId in " + path);
        }
        ResourceLocation id = ResourceLocation.tryParse(root.get("entityId").getAsString());
        if (id == null) {
            throw new IOException("Invalid entityId in " + path);
        }
        EntityRideProfile defaults = EntityRideProfile.defaults(id);
        RiderVisualProfile visual = root.has("visual") && root.get("visual").isJsonObject()
                ? readVisual(root.getAsJsonObject("visual"))
                : defaults.visual();
        return new EntityRideProfile(
                id,
                enumValue(root, "mode", RideMode.class, defaults.mode()),
                enumValue(root, "movementMode", RideMovementMode.class, defaults.movementMode()),
                number(root, "groundSpeedMultiplier", defaults.groundSpeedMultiplier()),
                number(root, "turnRateDegrees", defaults.turnRateDegrees()),
                number(root, "acceleration", defaults.acceleration()),
                number(root, "deceleration", defaults.deceleration()),
                number(root, "jumpStrength", defaults.jumpStrength()),
                number(root, "flightSpeedMultiplier", defaults.flightSpeedMultiplier()),
                number(root, "ascentSpeed", defaults.ascentSpeed()),
                number(root, "descentSpeed", defaults.descentSpeed()),
                number(root, "swimSpeedMultiplier", defaults.swimSpeedMultiplier()),
                bool(root, "autoAttackWhileRidden", defaults.autoAttackWhileRidden()),
                bool(root, "visualOverride", defaults.visualOverride()),
                visual
        ).validated();
    }

    public static void writeSettings(Path path, RidingSettings settings) throws IOException {
        RidingSettings value = settings.validated();
        JsonObject root = new JsonObject();
        root.addProperty("filterMode", value.filterMode().name());
        JsonArray entries = new JsonArray();
        value.filterEntries().stream().map(ResourceLocation::toString).sorted().forEach(entries::add);
        root.add("filterEntries", entries);
        writeJson(path, root);
    }

    public static void writeProfile(Path path, EntityRideProfile profile) throws IOException {
        EntityRideProfile value = profile.validated();
        JsonObject root = new JsonObject();
        root.addProperty("entityId", value.entityId().toString());
        root.addProperty("mode", value.mode().name());
        root.addProperty("movementMode", value.movementMode().name());
        root.addProperty("groundSpeedMultiplier", value.groundSpeedMultiplier());
        root.addProperty("turnRateDegrees", value.turnRateDegrees());
        root.addProperty("acceleration", value.acceleration());
        root.addProperty("deceleration", value.deceleration());
        root.addProperty("jumpStrength", value.jumpStrength());
        root.addProperty("flightSpeedMultiplier", value.flightSpeedMultiplier());
        root.addProperty("ascentSpeed", value.ascentSpeed());
        root.addProperty("descentSpeed", value.descentSpeed());
        root.addProperty("swimSpeedMultiplier", value.swimSpeedMultiplier());
        root.addProperty("autoAttackWhileRidden", value.autoAttackWhileRidden());
        root.addProperty("visualOverride", value.visualOverride());
        root.add("visual", writeVisual(value.visual()));
        writeJson(path, root);
    }

    private static RiderVisualProfile readVisual(JsonObject root) {
        RiderVisualProfile defaults = RiderVisualProfile.defaults();
        return new RiderVisualProfile(
                number(root, "lateralRatio", defaults.lateralRatio()),
                number(root, "verticalRatio", defaults.verticalRatio()),
                number(root, "forwardRatio", defaults.forwardRatio()),
                number(root, "offsetX", defaults.offsetX()),
                number(root, "offsetY", defaults.offsetY()),
                number(root, "offsetZ", defaults.offsetZ()),
                enumValue(root, "posePreset", RiderPosePreset.class, defaults.posePreset()),
                (float) number(root, "bodyPitch", defaults.bodyPitch()),
                (float) number(root, "bodyYawOffset", defaults.bodyYawOffset()),
                readRotation(root, "leftLeg"),
                readRotation(root, "rightLeg"),
                readRotation(root, "leftArm"),
                readRotation(root, "rightArm"),
                (float) number(root, "visualPlayerScale", defaults.visualPlayerScale()),
                number(root, "cameraVerticalOffset", defaults.cameraVerticalOffset()),
                number(root, "cameraBackwardOffset", defaults.cameraBackwardOffset())
        ).validated();
    }

    private static RiderVisualProfile.LimbRotation readRotation(JsonObject root, String key) {
        if (!root.has(key) || !root.get(key).isJsonObject()) {
            return RiderVisualProfile.LimbRotation.ZERO;
        }
        JsonObject value = root.getAsJsonObject(key);
        return new RiderVisualProfile.LimbRotation(
                (float) number(value, "pitch", 0.0D),
                (float) number(value, "yaw", 0.0D),
                (float) number(value, "roll", 0.0D)
        ).validated();
    }

    private static JsonObject writeVisual(RiderVisualProfile visual) {
        RiderVisualProfile value = visual.validated();
        JsonObject root = new JsonObject();
        root.addProperty("lateralRatio", value.lateralRatio());
        root.addProperty("verticalRatio", value.verticalRatio());
        root.addProperty("forwardRatio", value.forwardRatio());
        root.addProperty("offsetX", value.offsetX());
        root.addProperty("offsetY", value.offsetY());
        root.addProperty("offsetZ", value.offsetZ());
        root.addProperty("posePreset", value.posePreset().name());
        root.addProperty("bodyPitch", value.bodyPitch());
        root.addProperty("bodyYawOffset", value.bodyYawOffset());
        root.add("leftLeg", writeRotation(value.leftLeg()));
        root.add("rightLeg", writeRotation(value.rightLeg()));
        root.add("leftArm", writeRotation(value.leftArm()));
        root.add("rightArm", writeRotation(value.rightArm()));
        root.addProperty("visualPlayerScale", value.visualPlayerScale());
        root.addProperty("cameraVerticalOffset", value.cameraVerticalOffset());
        root.addProperty("cameraBackwardOffset", value.cameraBackwardOffset());
        return root;
    }

    private static JsonObject writeRotation(RiderVisualProfile.LimbRotation rotation) {
        JsonObject root = new JsonObject();
        root.addProperty("pitch", rotation.pitch());
        root.addProperty("yaw", rotation.yaw());
        root.addProperty("roll", rotation.roll());
        return root;
    }

    private static JsonObject readObject(Path path) throws IOException {
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new IOException("Expected JSON object: " + path);
            }
            return parsed.getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Invalid JSON: " + path, exception);
        }
    }

    private static void writeJson(Path path, JsonElement value) throws IOException {
        Files.createDirectories(path.getParent());
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temp, GSON.toJson(value), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static double number(JsonObject root, String key, double fallback) {
        try {
            return root.has(key) ? root.get(key).getAsDouble() : fallback;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject root, String key, boolean fallback) {
        try {
            return root.has(key) ? root.get(key).getAsBoolean() : fallback;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static <E extends Enum<E>> E enumValue(JsonObject root, String key, Class<E> type, E fallback) {
        try {
            return root.has(key) ? Enum.valueOf(type, root.get(key).getAsString()) : fallback;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}
