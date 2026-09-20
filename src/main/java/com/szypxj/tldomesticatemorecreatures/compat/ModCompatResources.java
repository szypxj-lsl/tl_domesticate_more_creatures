package com.szypxj.tldomesticatemorecreatures.compat;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Safe optional-mod resource reader used by compatibility metadata providers. */
public final class ModCompatResources {
    private static final ConcurrentHashMap<String, Optional<String>> UTF8_CACHE = new ConcurrentHashMap<>();

    private ModCompatResources() {
    }

    public static Optional<String> readUtf8(String modId, String resourcePath) {
        if (modId == null || modId.isBlank() || resourcePath == null || resourcePath.isBlank()) {
            return Optional.empty();
        }
        String normalizedModId = modId.trim();
        String normalizedPath = resourcePath.trim();
        return UTF8_CACHE.computeIfAbsent(
                normalizedModId + "\u0000" + normalizedPath,
                ignored -> loadUtf8(normalizedModId, normalizedPath)
        );
    }

    private static Optional<String> loadUtf8(String modId, String resourcePath) {
        try {
            IModFileInfo modFile = ModList.get().getModFileById(modId);
            if (modFile == null) {
                return Optional.empty();
            }
            Path path = modFile.getFile().findResource(resourcePath);
            if (path == null || !Files.isRegularFile(path)) {
                return Optional.empty();
            }
            return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    public static Optional<String> readLocalized(
            String modId,
            String requestedLanguage,
            Function<String, String> pathFactory
    ) {
        if (pathFactory == null) {
            return Optional.empty();
        }
        for (String language : languageCandidates(requestedLanguage)) {
            Optional<String> result = readUtf8(modId, pathFactory.apply(language));
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    private static List<String> languageCandidates(String requestedLanguage) {
        String normalized = normalizeLanguage(requestedLanguage);
        List<String> result = new ArrayList<>(2);
        result.add(normalized);
        if (!"en_us".equals(normalized)) {
            result.add("en_us");
        }
        return result;
    }

    private static String normalizeLanguage(String value) {
        if (value == null || value.isBlank()) {
            return "en_us";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return normalized.matches("[a-z0-9_]+") ? normalized : "en_us";
    }
}
