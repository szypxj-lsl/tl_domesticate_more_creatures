package com.szypxj.tldomesticatemorecreatures.riding.config;

import com.mojang.logging.LogUtils;
import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class RidingConfigManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Object LOCK = new Object();
    private static final Path RIDING_DIR = FMLPaths.CONFIGDIR.get()
            .resolve(TlDomesticateMoreCreatures.MOD_ID)
            .resolve("riding");
    private static final Path SETTINGS_PATH = RIDING_DIR.resolve("settings.json");
    private static final Path ENTITIES_DIR = RIDING_DIR.resolve("entities");
    private static volatile RidingSettings settings = RidingSettings.defaults();
    private static volatile Map<ResourceLocation, EntityRideProfile> modifiedProfiles = Map.of();
    private static volatile long generation;

    private RidingConfigManager() {
    }

    public static void initialize() {
        synchronized (LOCK) {
            try {
                Files.createDirectories(ENTITIES_DIR);
                if (!Files.exists(SETTINGS_PATH)) {
                    RidingConfigCodec.writeSettings(SETTINGS_PATH, RidingSettings.defaults());
                }
                applyReload(false);
            } catch (IOException exception) {
                LOGGER.error("Failed to initialize riding configuration.", exception);
            }
        }
    }

    public static RidingSettings settings() {
        return settings;
    }

    public static EntityRideProfile profile(ResourceLocation entityId) {
        Objects.requireNonNull(entityId);
        EntityRideProfile modified = modifiedProfiles.get(entityId);
        return modified == null ? EntityRideProfile.defaults(entityId) : modified;
    }

    public static EntityRideProfile profile(EntityType<?> type) {
        ResourceLocation id = EntityType.getKey(type);
        return profile(id);
    }

    public static Map<ResourceLocation, EntityRideProfile> modifiedProfiles() {
        return modifiedProfiles;
    }

    public static long generation() {
        return generation;
    }

    public static RidingConfigSnapshot snapshot(ServerLevel level) {
        return new RidingConfigSnapshot(generation, settings, modifiedProfiles, RidingConfigScope.configurableEntityIds(level));
    }

    public static SaveResult saveProfile(ServerPlayer actor, EntityRideProfile profile) {
        if (!mayManage(actor)) {
            return new SaveResult(false, "msg.tl_domesticate_more_creatures.riding.no_permission", generation);
        }
        if (profile == null || profile.entityId() == null || !ForgeRegistries.ENTITY_TYPES.containsKey(profile.entityId())) {
            return new SaveResult(false, "msg.tl_domesticate_more_creatures.riding.invalid_profile", generation);
        }
        if (!RidingConfigScope.isConfigurable(actor.serverLevel(), profile.entityId())) {
            return new SaveResult(false, "msg.tl_domesticate_more_creatures.riding.not_configurable", generation);
        }
        EntityRideProfile value = profile.validated();
        synchronized (LOCK) {
            try {
                RidingConfigCodec.writeProfile(profilePath(value.entityId()), value);
                LinkedHashMap<ResourceLocation, EntityRideProfile> updated = new LinkedHashMap<>(modifiedProfiles);
                updated.put(value.entityId(), value);
                modifiedProfiles = Map.copyOf(updated);
                generation++;
                return new SaveResult(true, "msg.tl_domesticate_more_creatures.riding.saved", generation);
            } catch (IOException exception) {
                LOGGER.error("Failed to save riding profile {}.", value.entityId(), exception);
                return new SaveResult(false, "msg.tl_domesticate_more_creatures.riding.save_failed", generation);
            }
        }
    }

    public static SaveResult saveSettings(ServerPlayer actor, RidingSettings newSettings) {
        if (!mayManage(actor)) {
            return new SaveResult(false, "msg.tl_domesticate_more_creatures.riding.no_permission", generation);
        }
        if (newSettings == null) {
            return new SaveResult(false, "msg.tl_domesticate_more_creatures.riding.invalid_settings", generation);
        }
        RidingSettings value = newSettings.validated();
        synchronized (LOCK) {
            try {
                RidingConfigCodec.writeSettings(SETTINGS_PATH, value);
                settings = value;
                generation++;
                return new SaveResult(true, "msg.tl_domesticate_more_creatures.riding.saved", generation);
            } catch (IOException exception) {
                LOGGER.error("Failed to save riding settings.", exception);
                return new SaveResult(false, "msg.tl_domesticate_more_creatures.riding.save_failed", generation);
            }
        }
    }

    public static ReloadResult reload(ServerPlayer actor) {
        if (!mayManage(actor)) {
            return new ReloadResult(false, "msg.tl_domesticate_more_creatures.riding.no_permission", generation);
        }
        synchronized (LOCK) {
            try {
                boolean changed = applyReload(true);
                return new ReloadResult(true,
                        changed ? "msg.tl_domesticate_more_creatures.riding.reloaded" : "msg.tl_domesticate_more_creatures.riding.reload_unchanged",
                        generation);
            } catch (IOException exception) {
                LOGGER.error("Failed to reload riding configuration.", exception);
                return new ReloadResult(false, "msg.tl_domesticate_more_creatures.riding.reload_failed", generation);
            }
        }
    }

    public static boolean mayManage(ServerPlayer actor) {
        return actor != null && actor.hasPermissions(2);
    }

    private static boolean applyReload(boolean incrementWhenChanged) throws IOException {
        Files.createDirectories(ENTITIES_DIR);
        RidingSettings acceptedSettings = settings;
        if (Files.exists(SETTINGS_PATH)) {
            try {
                acceptedSettings = RidingConfigCodec.readSettings(SETTINGS_PATH);
            } catch (IOException exception) {
                LOGGER.error("Invalid riding settings file; retaining previous in-memory settings.", exception);
            }
        }

        LinkedHashMap<ResourceLocation, EntityRideProfile> acceptedProfiles = new LinkedHashMap<>();
        try (Stream<Path> stream = Files.list(ENTITIES_DIR)) {
            for (Path path : stream.filter(Files::isRegularFile)
                    .filter(value -> value.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList()) {
                try {
                    EntityRideProfile profile = RidingConfigCodec.readProfile(path).validated();
                    if (!ForgeRegistries.ENTITY_TYPES.containsKey(profile.entityId())) {
                        LOGGER.warn("Skipping unknown riding entity profile {} for {}.", path.getFileName(), profile.entityId());
                        continue;
                    }
                    acceptedProfiles.put(profile.entityId(), profile);
                } catch (IOException | RuntimeException exception) {
                    LOGGER.error("Skipping invalid riding entity profile {}.", path.getFileName(), exception);
                }
            }
        }

        Map<ResourceLocation, EntityRideProfile> accepted = Map.copyOf(acceptedProfiles);
        boolean changed = !acceptedSettings.equals(settings) || !accepted.equals(modifiedProfiles);
        settings = acceptedSettings;
        modifiedProfiles = accepted;
        if (changed && incrementWhenChanged) {
            generation++;
        }
        return changed;
    }

    private static Path profilePath(ResourceLocation id) {
        String safePath = id.getPath().replace('/', '_').replace('\\', '_');
        return ENTITIES_DIR.resolve(id.getNamespace() + "_" + safePath + ".json");
    }

    public record SaveResult(boolean success, String messageKey, long generation) {
    }

    public record ReloadResult(boolean success, String messageKey, long generation) {
    }
}
