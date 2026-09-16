package com.szypxj.tldomesticatemorecreatures;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class LegacyMigrationService {
    public static final String LEGACY_MOD_ID = "tl_biological_attribute_panel";

    private LegacyMigrationService() {
    }

    public static void migrateConfigFiles() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        copyIfMissing(
                configDir.resolve(LEGACY_MOD_ID + "-common.toml"),
                configDir.resolve(TlDomesticateMoreCreatures.MOD_ID + "-common.toml")
        );

        Path oldDir = configDir.resolve(LEGACY_MOD_ID);
        Path newDir = configDir.resolve(TlDomesticateMoreCreatures.MOD_ID);
        migrateNamespacedFile(oldDir.resolve("attributes.json"), newDir.resolve("attributes.json"));
        migrateNamespacedFile(oldDir.resolve("talents.json"), newDir.resolve("talents.json"));
        rewriteLegacyNamespace(newDir.resolve("attributes.json"));
        rewriteLegacyNamespace(newDir.resolve("talents.json"));
    }

    private static void migrateNamespacedFile(Path source, Path target) {
        if (!Files.isRegularFile(source) || Files.exists(target)) {
            return;
        }
        try {
            Files.createDirectories(target.getParent());
            String content = Files.readString(source, StandardCharsets.UTF_8)
                    .replace(LEGACY_MOD_ID, TlDomesticateMoreCreatures.MOD_ID);
            Files.writeString(target, content, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static void rewriteLegacyNamespace(Path target) {
        if (!Files.isRegularFile(target)) {
            return;
        }
        try {
            String content = Files.readString(target, StandardCharsets.UTF_8);
            String migrated = content.replace(LEGACY_MOD_ID, TlDomesticateMoreCreatures.MOD_ID);
            if (!migrated.equals(content)) {
                Files.writeString(target, migrated, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }
    }

    private static void copyIfMissing(Path source, Path target) {
        if (!Files.isRegularFile(source) || Files.exists(target)) {
            return;
        }
        try {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException ignored) {
        }
    }
}
