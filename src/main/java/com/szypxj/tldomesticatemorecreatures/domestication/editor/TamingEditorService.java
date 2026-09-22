package com.szypxj.tldomesticatemorecreatures.domestication.editor;

import com.mojang.logging.LogUtils;
import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.compat.fossil.FossilNativeFoodCompat;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingRule;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingRuleManager;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TamingEditorService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Object LOCK = new Object();
    private static final Path PATH = FMLPaths.CONFIGDIR.get()
            .resolve(TlDomesticateMoreCreatures.MOD_ID)
            .resolve("taming_rules.toml");
    private static final Map<ResourceLocation, NativeFoodsResult> NATIVE_CACHE = new ConcurrentHashMap<>();
    private static volatile List<ResourceLocation> livingEntityIds;
    private static volatile long generation;

    private TamingEditorService() {
    }

    public static boolean mayManage(ServerPlayer player) {
        return player != null && player.hasPermissions(2);
    }

    public static TamingEditorSnapshot snapshot() {
        Map<ResourceLocation, TamingEditorRule> editorRules = new LinkedHashMap<>();
        TamingRuleManager.all().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> editorRules.put(entry.getKey(), TamingEditorRule.from(entry.getValue())));

        List<TamingEditorEntityInfo> entities = new ArrayList<>();
        for (ResourceLocation id : livingEntityIds()) {
            entities.add(new TamingEditorEntityInfo(id, LevelService.isAffectedEntityId(id.toString())));
        }
        return new TamingEditorSnapshot(generation, entities, editorRules);
    }

    public static NativeFoodsResult detectNativeFoods(ServerLevel level, ResourceLocation entityId) {
        if (level == null || entityId == null || !livingEntityIds().contains(entityId)) {
            return NativeFoodsResult.failure();
        }
        NativeFoodsResult cached = NATIVE_CACHE.get(entityId);
        if (cached != null) {
            return cached;
        }
        NativeFoodsResult detected = detectNativeFoodsUncached(level, entityId);
        NativeFoodsResult previous = NATIVE_CACHE.putIfAbsent(entityId, detected);
        return previous == null ? detected : previous;
    }

    public static SaveResult save(
            ServerPlayer actor,
            Map<ResourceLocation, TamingEditorRule> upserts,
            Set<ResourceLocation> deletes
    ) {
        if (!mayManage(actor)) {
            return new SaveResult(false, "msg.tl_domesticate_more_creatures.taming_editor.no_permission", generation);
        }
        Map<ResourceLocation, TamingEditorRule> safeUpserts = upserts == null ? Map.of() : Map.copyOf(upserts);
        Set<ResourceLocation> safeDeletes = deletes == null ? Set.of() : Set.copyOf(deletes);
        ServerLevel level = actor.serverLevel();

        Map<ResourceLocation, TamingEditorRule> merged = new LinkedHashMap<>();
        TamingRuleManager.all().forEach((id, rule) -> merged.put(id, TamingEditorRule.from(rule)));
        for (ResourceLocation id : safeDeletes) {
            merged.remove(id);
        }

        for (Map.Entry<ResourceLocation, TamingEditorRule> entry : safeUpserts.entrySet()) {
            ResourceLocation id = entry.getKey();
            TamingEditorRule rule = entry.getValue();
            if (id == null || rule == null || !id.equals(rule.entityId())) {
                return invalid();
            }
            boolean existing = TamingRuleManager.all().containsKey(id);
            ValidationResult validation = validateRule(level, rule, existing);
            if (!validation.valid()) {
                return new SaveResult(false, validation.messageKey(), generation);
            }
            merged.put(id, rule);
        }

        synchronized (LOCK) {
            try {
                writeRules(merged);
                TamingRuleManager.reload();
                generation++;
                return new SaveResult(true, "msg.tl_domesticate_more_creatures.taming_editor.saved", generation);
            } catch (IOException | RuntimeException exception) {
                LOGGER.error("Failed to save taming editor configuration.", exception);
                return new SaveResult(false, "msg.tl_domesticate_more_creatures.taming_editor.save_failed", generation);
            }
        }
    }

    public static ReloadResult reload(ServerPlayer actor) {
        if (!mayManage(actor)) {
            return new ReloadResult(false, "msg.tl_domesticate_more_creatures.taming_editor.no_permission", generation);
        }
        synchronized (LOCK) {
            try {
                TamingRuleManager.reload();
                generation++;
                return new ReloadResult(true, "msg.tl_domesticate_more_creatures.taming_editor.reloaded", generation);
            } catch (RuntimeException exception) {
                LOGGER.error("Failed to reload taming editor configuration.", exception);
                return new ReloadResult(false, "msg.tl_domesticate_more_creatures.taming_editor.reload_failed", generation);
            }
        }
    }

    public static void clearNativeFoodCache() {
        NATIVE_CACHE.clear();
    }

    private static SaveResult invalid() {
        return new SaveResult(false, "msg.tl_domesticate_more_creatures.taming_editor.invalid_rule", generation);
    }

    private static ValidationResult validateRule(ServerLevel level, TamingEditorRule rule, boolean existing) {
        if (rule.entityId() == null || rule.method() == null || rule.requiredPlayerLevel() < 1) {
            return ValidationResult.fail("msg.tl_domesticate_more_creatures.taming_editor.invalid_rule");
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(rule.entityId());
        if (type == null || !isLivingEntityType(type)) {
            return ValidationResult.fail("msg.tl_domesticate_more_creatures.taming_editor.invalid_entity");
        }
        if (!existing && !LevelService.isAffectedEntityId(rule.entityId().toString())) {
            return ValidationResult.fail("msg.tl_domesticate_more_creatures.taming_editor.filtered_entity");
        }

        NativeFoodsResult nativeResult = detectNativeFoods(level, rule.entityId());
        if (!nativeResult.success()) {
            if (!rule.nativeFoods().isEmpty() || !rule.removedNativeFoods().isEmpty()) {
                return ValidationResult.fail("msg.tl_domesticate_more_creatures.taming_editor.native_detect_failed");
            }
        }
        Set<ResourceLocation> nativeSet = Set.copyOf(nativeResult.items());

        for (Map.Entry<ResourceLocation, Integer> entry : rule.nativeFoods().entrySet()) {
            if (rule.removedNativeFoods().contains(entry.getKey())) {
                continue;
            }
            if (!validItem(entry.getKey()) || entry.getValue() == null || entry.getValue() < 1 || !nativeSet.contains(entry.getKey())) {
                return ValidationResult.fail("msg.tl_domesticate_more_creatures.taming_editor.invalid_food");
            }
        }
        for (ResourceLocation itemId : rule.removedNativeFoods()) {
            if (!validItem(itemId) || !nativeSet.contains(itemId)) {
                return ValidationResult.fail("msg.tl_domesticate_more_creatures.taming_editor.invalid_removed_food");
            }
        }
        for (Map.Entry<ResourceLocation, Integer> entry : rule.extraFoods().entrySet()) {
            if (!validItem(entry.getKey()) || entry.getValue() == null || entry.getValue() < 1) {
                return ValidationResult.fail("msg.tl_domesticate_more_creatures.taming_editor.invalid_food");
            }
            if (nativeSet.contains(entry.getKey())) {
                return ValidationResult.fail("msg.tl_domesticate_more_creatures.taming_editor.food_conflict");
            }
        }
        for (Map.Entry<ResourceLocation, Integer> entry : rule.legacyFoods().entrySet()) {
            if (!validItem(entry.getKey()) || entry.getValue() == null || entry.getValue() < 1) {
                return ValidationResult.fail("msg.tl_domesticate_more_creatures.taming_editor.invalid_food");
            }
        }

        if (!hasEffectiveFood(rule, nativeSet)) {
            return ValidationResult.fail("msg.tl_domesticate_more_creatures.taming_editor.no_effective_food");
        }
        return ValidationResult.ok();
    }

    private static boolean hasEffectiveFood(TamingEditorRule rule, Set<ResourceLocation> nativeSet) {
        for (ResourceLocation itemId : nativeSet) {
            if (rule.removedNativeFoods().contains(itemId)) {
                continue;
            }
            Integer nativeAmount = rule.nativeFoods().get(itemId);
            Integer legacyAmount = rule.legacyFoods().get(itemId);
            if ((nativeAmount != null && nativeAmount > 0) || (legacyAmount != null && legacyAmount > 0)) {
                return true;
            }
        }
        for (Map.Entry<ResourceLocation, Integer> entry : rule.extraFoods().entrySet()) {
            if (!nativeSet.contains(entry.getKey()) && entry.getValue() != null && entry.getValue() > 0) {
                return true;
            }
        }
        for (Map.Entry<ResourceLocation, Integer> entry : rule.legacyFoods().entrySet()) {
            if (!nativeSet.contains(entry.getKey()) && entry.getValue() != null && entry.getValue() > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean validItem(ResourceLocation itemId) {
        return itemId != null && ForgeRegistries.ITEMS.containsKey(itemId);
    }

    private static NativeFoodsResult detectNativeFoodsUncached(ServerLevel level, ResourceLocation entityId) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (type == null || !isLivingEntityType(type)) {
            return NativeFoodsResult.failure();
        }

        try {
            Entity created = type.create(level);
            if (!(created instanceof LivingEntity living)) {
                return NativeFoodsResult.failure();
            }

            // Fossils & Archeology does not expose its Dinopedia diet through Animal#isFood.
            // Read the exact dino.data().diet() + FoodMappingsManager cache used by Dinopedia.
            if ("fossil".equals(entityId.getNamespace())) {
                List<ResourceLocation> fossilFoods = FossilNativeFoodCompat.detectNativeFoodIds(living);
                if (fossilFoods != null) {
                    return new NativeFoodsResult(true, fossilFoods);
                }
            }

            if (!(living instanceof Animal animal)) {
                return new NativeFoodsResult(true, List.of());
            }
            List<ResourceLocation> result = new ArrayList<>();
            for (Item item : ForgeRegistries.ITEMS.getValues()) {
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
                if (itemId != null && animal.isFood(new ItemStack(item))) {
                    result.add(itemId);
                }
            }
            result.sort(Comparator.comparing(ResourceLocation::toString));
            return new NativeFoodsResult(true, result);
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to detect native foods for {}.", entityId, throwable);
            return NativeFoodsResult.failure();
        }
    }

    private static boolean isLivingEntityType(EntityType<?> type) {
        if (type == null || type == EntityType.PLAYER) {
            return false;
        }
        try {
            return DefaultAttributes.hasSupplier(type);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static List<ResourceLocation> livingEntityIds() {
        List<ResourceLocation> cached = livingEntityIds;
        if (cached != null) {
            return cached;
        }
        synchronized (LOCK) {
            if (livingEntityIds != null) {
                return livingEntityIds;
            }
            List<ResourceLocation> result = new ArrayList<>();
            for (ResourceLocation id : ForgeRegistries.ENTITY_TYPES.getKeys()) {
                EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
                if (isLivingEntityType(type)) {
                    result.add(id);
                }
            }
            result.sort(Comparator.comparing(ResourceLocation::toString));
            livingEntityIds = List.copyOf(result);
            return livingEntityIds;
        }
    }

    private static void writeRules(Map<ResourceLocation, TamingEditorRule> rules) throws IOException {
        Files.createDirectories(PATH.getParent());
        Path temp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
        Files.writeString(temp, encodeToml(rules), StandardCharsets.UTF_8);
        try {
            Files.move(temp, PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temp, PATH, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static String encodeToml(Map<ResourceLocation, TamingEditorRule> rules) {
        StringBuilder out = new StringBuilder();
        out.append("# TL Domesticate More Creatures 驯服规则\n");
        out.append("# 此文件可由 /tdmc taming edit 游戏内编辑器维护。\n");
        out.append("# removed_native_foods 只影响 TDMC 驯服，不改变原版 isFood()/繁殖/引诱。\n\n");
        out.append("rules = [\n");
        rules.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> appendRule(out, entry.getValue()));
        out.append("]\n");
        return out.toString();
    }

    private static void appendRule(StringBuilder out, TamingEditorRule rule) {
        out.append("    { entity = \"").append(rule.entityId()).append("\", method = \"")
                .append(rule.method().name()).append("\", required_player_level = ")
                .append(rule.requiredPlayerLevel());
        appendFoods(out, "native_foods", rule.nativeFoods());
        appendFoods(out, "extra_foods", rule.extraFoods());
        appendFoods(out, "foods", rule.legacyFoods());
        if (!rule.removedNativeFoods().isEmpty()) {
            out.append(", removed_native_foods = [");
            boolean first = true;
            for (ResourceLocation id : rule.removedNativeFoods().stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList()) {
                if (!first) {
                    out.append(", ");
                }
                first = false;
                out.append('"').append(id).append('"');
            }
            out.append(']');
        }
        out.append(" },\n");
    }

    private static void appendFoods(StringBuilder out, String key, Map<ResourceLocation, Integer> foods) {
        if (foods.isEmpty()) {
            return;
        }
        out.append(", ").append(key).append(" = [");
        boolean first = true;
        for (Map.Entry<ResourceLocation, Integer> entry : foods.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString))).toList()) {
            if (!first) {
                out.append(", ");
            }
            first = false;
            out.append("{ item = \"").append(entry.getKey()).append("\", amount = ")
                    .append(entry.getValue()).append(" }");
        }
        out.append(']');
    }

    public record NativeFoodsResult(boolean success, List<ResourceLocation> items) {
        public NativeFoodsResult {
            items = List.copyOf(items == null ? List.of() : items);
        }

        public static NativeFoodsResult failure() {
            return new NativeFoodsResult(false, List.of());
        }
    }

    public record SaveResult(boolean success, String messageKey, long generation) {
    }

    public record ReloadResult(boolean success, String messageKey, long generation) {
    }

    private record ValidationResult(boolean valid, String messageKey) {
        private static ValidationResult ok() {
            return new ValidationResult(true, "");
        }

        private static ValidationResult fail(String key) {
            return new ValidationResult(false, key);
        }
    }
}
