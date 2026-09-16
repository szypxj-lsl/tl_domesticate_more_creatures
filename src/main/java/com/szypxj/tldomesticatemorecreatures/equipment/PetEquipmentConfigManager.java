package com.szypxj.tldomesticatemorecreatures.equipment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class PetEquipmentConfigManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Object LOCK = new Object();
    private static final Path ROOT = FMLPaths.CONFIGDIR.get()
            .resolve(TlDomesticateMoreCreatures.MOD_ID)
            .resolve("pet_equipment");
    private static final Path ENTITIES_DIR = ROOT.resolve("entities");
    private static final Path ITEMS_DIR = ROOT.resolve("items");
    private static volatile Map<ResourceLocation, PetEquipmentEntityProfile> entityProfiles = Map.of();
    private static volatile Map<ResourceLocation, PetEquipmentItemRule> itemRules = Map.of();
    private static volatile Set<ResourceLocation> referencedAttributes = Set.of();
    private static volatile long generation;

    private PetEquipmentConfigManager() {
    }

    public static void initialize() {
        synchronized (LOCK) {
            try {
                Files.createDirectories(ENTITIES_DIR);
                Files.createDirectories(ITEMS_DIR);
                reloadInternal();
            } catch (IOException exception) {
                LOGGER.error("Failed to initialize pet equipment configuration.", exception);
            }
        }
    }

    public static void reload() {
        synchronized (LOCK) {
            try {
                reloadInternal();
            } catch (IOException exception) {
                LOGGER.error("Failed to reload pet equipment configuration.", exception);
            }
        }
    }

    public static long generation() {
        return generation;
    }

    public static PetEquipmentEntityProfile profile(EntityType<?> type) {
        ResourceLocation id = EntityType.getKey(type);
        PetEquipmentEntityProfile profile = entityProfiles.get(id);
        return profile == null ? PetEquipmentEntityProfile.defaults(id) : profile;
    }

    public static PetEquipmentItemRule rule(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? null : itemRules.get(id);
    }

    public static Set<ResourceLocation> referencedAttributes() {
        return referencedAttributes;
    }

    private static void reloadInternal() throws IOException {
        Files.createDirectories(ENTITIES_DIR);
        Files.createDirectories(ITEMS_DIR);

        LinkedHashMap<ResourceLocation, PetEquipmentEntityProfile> profiles = new LinkedHashMap<>();
        for (Path path : jsonFiles(ENTITIES_DIR)) {
            try {
                PetEquipmentEntityProfile profile = readEntityProfile(path);
                if (profile.entityId() == null || !ForgeRegistries.ENTITY_TYPES.containsKey(profile.entityId())) {
                    LOGGER.warn("Skipping unknown pet equipment entity profile {}.", path.getFileName());
                    continue;
                }
                profiles.put(profile.entityId(), profile.validated());
            } catch (IOException | RuntimeException exception) {
                LOGGER.error("Skipping invalid pet equipment entity profile {}.", path.getFileName(), exception);
            }
        }

        LinkedHashMap<ResourceLocation, PetEquipmentItemRule> rules = new LinkedHashMap<>();
        LinkedHashSet<ResourceLocation> attributes = new LinkedHashSet<>();
        for (Path path : jsonFiles(ITEMS_DIR)) {
            try {
                PetEquipmentItemRule rule = readItemRule(path).validated();
                if (rule.itemId() == null || !ForgeRegistries.ITEMS.containsKey(rule.itemId())) {
                    LOGGER.warn("Skipping unknown pet equipment item rule {}.", path.getFileName());
                    continue;
                }
                rules.put(rule.itemId(), rule);
                for (PetEquipmentItemRule.AttributeBonus bonus : rule.attributeBonuses()) {
                    if (bonus.attributeId() != null && ForgeRegistries.ATTRIBUTES.containsKey(bonus.attributeId())) {
                        attributes.add(bonus.attributeId());
                    }
                }
            } catch (IOException | RuntimeException exception) {
                LOGGER.error("Skipping invalid pet equipment item rule {}.", path.getFileName(), exception);
            }
        }

        entityProfiles = Map.copyOf(profiles);
        itemRules = Map.copyOf(rules);
        referencedAttributes = Set.copyOf(attributes);
        generation++;
    }

    private static List<Path> jsonFiles(Path directory) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted()
                    .toList();
        }
    }

    private static PetEquipmentEntityProfile readEntityProfile(Path path) throws IOException {
        JsonObject root = readObject(path);
        ResourceLocation entityId = parseId(root, "entityId");
        JsonArray slotsNode = root.has("slots") && root.get("slots").isJsonArray()
                ? root.getAsJsonArray("slots")
                : new JsonArray();
        List<PetEquipmentSlotDefinition> slots = new ArrayList<>();
        for (JsonElement element : slotsNode) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject slot = element.getAsJsonObject();
            String id = string(slot, "id", "equipment");
            String nameKey = string(slot, "nameKey", "gui.tl_domesticate_more_creatures.pet_equipment.slot." + PetEquipmentSlotDefinition.normalizeId(id));
            EquipmentSlot vanillaSlot = parseEquipmentSlot(string(slot, "vanillaSlot", ""));
            boolean allowAny = bool(slot, "allowAnyItem", false);
            slots.add(new PetEquipmentSlotDefinition(id, nameKey, vanillaSlot, allowAny));
        }
        return new PetEquipmentEntityProfile(entityId, slots.isEmpty()
                ? PetEquipmentEntityProfile.defaults(entityId).slots()
                : slots).validated();
    }

    private static PetEquipmentItemRule readItemRule(Path path) throws IOException {
        JsonObject root = readObject(path);
        ResourceLocation itemId = parseId(root, "itemId");
        Set<ResourceLocation> entities = parseIdSet(root, "allowedEntities");
        Set<String> slots = parseStringSet(root, "allowedSlots");
        List<PetEquipmentItemRule.AttributeBonus> bonuses = new ArrayList<>();
        if (root.has("attributeBonuses") && root.get("attributeBonuses").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("attributeBonuses")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject bonus = element.getAsJsonObject();
                ResourceLocation attributeId = parseOptionalId(string(bonus, "attribute", ""));
                if (attributeId == null || !ForgeRegistries.ATTRIBUTES.containsKey(attributeId)) {
                    continue;
                }
                double amount = number(bonus, "amount", 0.0D);
                PetEquipmentItemRule.Operation operation = PetEquipmentItemRule.Operation.parse(string(bonus, "operation", "ADDITION"));
                bonuses.add(new PetEquipmentItemRule.AttributeBonus(attributeId, amount, operation));
            }
        }
        return new PetEquipmentItemRule(itemId, entities, slots, bonuses).validated();
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

    private static ResourceLocation parseId(JsonObject root, String key) throws IOException {
        ResourceLocation id = parseOptionalId(string(root, key, ""));
        if (id == null) {
            throw new IOException("Missing or invalid " + key);
        }
        return id;
    }

    private static ResourceLocation parseOptionalId(String value) {
        return value == null || value.isBlank() ? null : ResourceLocation.tryParse(value.trim());
    }

    private static Set<ResourceLocation> parseIdSet(JsonObject root, String key) {
        LinkedHashSet<ResourceLocation> result = new LinkedHashSet<>();
        if (!root.has(key) || !root.get(key).isJsonArray()) {
            return result;
        }
        for (JsonElement element : root.getAsJsonArray(key)) {
            if (!element.isJsonPrimitive()) {
                continue;
            }
            ResourceLocation id = parseOptionalId(element.getAsString());
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    private static Set<String> parseStringSet(JsonObject root, String key) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (!root.has(key) || !root.get(key).isJsonArray()) {
            return result;
        }
        for (JsonElement element : root.getAsJsonArray(key)) {
            if (element.isJsonPrimitive()) {
                result.add(PetEquipmentSlotDefinition.normalizeId(element.getAsString()));
            }
        }
        return result;
    }

    private static EquipmentSlot parseEquipmentSlot(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            EquipmentSlot slot = EquipmentSlot.valueOf(value.trim().toUpperCase(Locale.ROOT));
            return slot.getType() == EquipmentSlot.Type.ARMOR ? slot : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String string(JsonObject root, String key, String fallback) {
        try {
            return root.has(key) ? root.get(key).getAsString() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject root, String key, boolean fallback) {
        try {
            return root.has(key) ? root.get(key).getAsBoolean() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double number(JsonObject root, String key, double fallback) {
        try {
            return root.has(key) ? root.get(key).getAsDouble() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
