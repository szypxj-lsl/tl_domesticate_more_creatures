package com.szypxj.tldomesticatemorecreatures.domestication;

import com.mojang.logging.LogUtils;
import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import com.szypxj.tldomesticatemorecreatures.imprint.ImprintService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TamingRuleManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path PATH = FMLPaths.CONFIGDIR.get()
            .resolve(TlDomesticateMoreCreatures.MOD_ID)
            .resolve("taming_rules.toml");
    private static volatile Map<ResourceLocation, TamingRule> rules = Map.of();
    private static final Map<ResourceLocation, ResolvedFoods> FOOD_CACHE = new ConcurrentHashMap<>();

    private TamingRuleManager() {
    }

    public static synchronized void reload() {
        ensureFile();
        Map<ResourceLocation, TamingRule> loaded = new LinkedHashMap<>();
        Set<ResourceLocation> seen = new HashSet<>();
        Set<ResourceLocation> conflicts = new HashSet<>();
        List<TamingRuleFileParser.ParsedRule> parsedRules = parseRules();

        for (TamingRuleFileParser.ParsedRule parsed : parsedRules) {
            ResourceLocation entityId = ResourceLocation.tryParse(parsed.entity());
            if (entityId == null || ForgeRegistries.ENTITY_TYPES.getValue(entityId) == null) {
                LOGGER.warn("[{}] 忽略无效驯服实体 ID: {}", TlDomesticateMoreCreatures.MOD_ID, parsed.entity());
                continue;
            }

            if (!seen.add(entityId)) {
                loaded.remove(entityId);
                conflicts.add(entityId);
                LOGGER.error("[{}] 实体 {} 被重复配置，自定义驯服规则已禁用。", TlDomesticateMoreCreatures.MOD_ID, entityId);
                continue;
            }
            if (conflicts.contains(entityId)) {
                continue;
            }

            TamingMethod method;
            try {
                method = TamingMethod.valueOf(parsed.method().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("[{}] 实体 {} 的驯服方式无效: {}", TlDomesticateMoreCreatures.MOD_ID, entityId, parsed.method());
                continue;
            }

            int requiredPlayerLevel = Math.max(1, parsed.requiredPlayerLevel());
            List<TamingFood> nativeFoods = parseFoods(entityId, parsed.nativeFoods(), "原生食物");
            List<TamingFood> extraFoods = parseFoods(entityId, parsed.extraFoods(), "额外食物");
            List<TamingFood> legacyFoods = parseFoods(entityId, parsed.legacyFoods(), "旧版食物");
            Set<ResourceLocation> removedNativeFoods = parseRemovedFoods(entityId, parsed.removedNativeFoods());

            loaded.put(entityId, new TamingRule(entityId, method, requiredPlayerLevel, nativeFoods, extraFoods, legacyFoods, removedNativeFoods));
        }
        rules = Map.copyOf(loaded);
        FOOD_CACHE.clear();
        ImprintService.clearFoodCache();
    }

    public static Optional<TamingRule> ruleFor(LivingEntity entity) {
        if (!LevelService.isAffected(entity)) {
            return Optional.empty();
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return Optional.ofNullable(id == null ? null : rules.get(id));
    }

    public static Optional<TamingRule> ruleFor(ResourceLocation entityId) {
        if (entityId == null || !LevelService.isAffectedEntityId(entityId.toString())) {
            return Optional.empty();
        }
        return Optional.ofNullable(rules.get(entityId));
    }

    public static Optional<TamingRule> ruleFor(EntityType<?> type) {
        ResourceLocation id = type == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(type);
        return ruleFor(id);
    }

    public static List<TamingFood> foodsFor(LivingEntity entity) {
        return resolvedFoodsFor(entity).usable();
    }

    public static List<TamingFoodDisplay> displayFoodsFor(LivingEntity entity) {
        return resolvedFoodsFor(entity).display();
    }

    public static List<TamingFoodDisplay> displayFoodsFor(ServerLevel level, EntityType<?> type) {
        if (level == null || type == null) {
            return List.of();
        }
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (entityId == null || ruleFor(entityId).isEmpty()) {
            return List.of();
        }
        ResolvedFoods cached = FOOD_CACHE.get(entityId);
        if (cached != null) {
            return cached.display();
        }
        Entity created = type.create(level);
        if (!(created instanceof LivingEntity living)) {
            return List.of();
        }
        TamingRule rule = rules.get(entityId);
        ResolvedFoods resolved = resolveFoods(living, rule);
        ResolvedFoods previous = FOOD_CACHE.putIfAbsent(entityId, resolved);
        return (previous == null ? resolved : previous).display();
    }

    public static Map<ResourceLocation, TamingRule> all() {
        return rules;
    }

    private static ResolvedFoods resolvedFoodsFor(LivingEntity entity) {
        if (!LevelService.isAffected(entity)) {
            return ResolvedFoods.EMPTY;
        }
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null) {
            return ResolvedFoods.EMPTY;
        }
        TamingRule rule = rules.get(entityId);
        if (rule == null) {
            return ResolvedFoods.EMPTY;
        }
        return FOOD_CACHE.computeIfAbsent(entityId, ignored -> resolveFoods(entity, rule));
    }

    private static ResolvedFoods resolveFoods(LivingEntity entity, TamingRule rule) {
        List<String> nativeItems = detectNativeFoods(entity);
        TamingFoodPlan.Result plan = TamingFoodPlan.resolve(
                nativeItems,
                specs(rule.nativeFoods()),
                specs(rule.extraFoods()),
                specs(rule.legacyFoods()),
                rule.removedNativeFoods().stream().map(ResourceLocation::toString).toList()
        );

        List<TamingFoodDisplay> display = new ArrayList<>();
        for (TamingFoodPlan.Entry entry : plan.display()) {
            ResourceLocation itemId = ResourceLocation.tryParse(entry.item());
            if (itemId == null || ForgeRegistries.ITEMS.getValue(itemId) == null) {
                continue;
            }
            display.add(new TamingFoodDisplay(itemId, entry.amount(), entry.configured()));
        }

        List<TamingFood> usable = new ArrayList<>();
        for (TamingFoodPlan.Entry entry : plan.usable()) {
            ResourceLocation itemId = ResourceLocation.tryParse(entry.item());
            if (itemId == null || ForgeRegistries.ITEMS.getValue(itemId) == null) {
                continue;
            }
            usable.add(new TamingFood(itemId, entry.amount()));
        }
        return new ResolvedFoods(List.copyOf(display), List.copyOf(usable));
    }

    private static List<String> detectNativeFoods(LivingEntity entity) {
        if (!(entity instanceof Animal animal)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId == null) {
                continue;
            }
            ItemStack stack = new ItemStack(item);
            if (animal.isFood(stack)) {
                result.add(itemId.toString());
            }
        }
        return List.copyOf(result);
    }

    private static List<TamingFoodPlan.Spec> specs(List<TamingFood> foods) {
        List<TamingFoodPlan.Spec> result = new ArrayList<>(foods.size());
        for (TamingFood food : foods) {
            result.add(new TamingFoodPlan.Spec(food.itemId().toString(), food.amount()));
        }
        return List.copyOf(result);
    }


    private static Set<ResourceLocation> parseRemovedFoods(
            ResourceLocation entityId,
            List<String> parsedFoods
    ) {
        Set<ResourceLocation> result = new java.util.LinkedHashSet<>();
        for (String raw : parsedFoods) {
            ResourceLocation itemId = ResourceLocation.tryParse(raw);
            if (itemId == null || ForgeRegistries.ITEMS.getValue(itemId) == null) {
                LOGGER.warn("[{}] 忽略实体 {} 的无效原生排除食物 ID: {}", TlDomesticateMoreCreatures.MOD_ID, entityId, raw);
                continue;
            }
            result.add(itemId);
        }
        return Set.copyOf(result);
    }

    private static List<TamingFood> parseFoods(
            ResourceLocation entityId,
            List<TamingRuleFileParser.ParsedFood> parsedFoods,
            String source
    ) {
        Map<ResourceLocation, TamingFood> result = new LinkedHashMap<>();
        for (TamingRuleFileParser.ParsedFood parsedFood : parsedFoods) {
            ResourceLocation itemId = ResourceLocation.tryParse(parsedFood.item());
            if (itemId == null || ForgeRegistries.ITEMS.getValue(itemId) == null) {
                LOGGER.warn("[{}] 忽略实体 {} 的无效{} ID: {}", TlDomesticateMoreCreatures.MOD_ID, entityId, source, parsedFood.item());
                continue;
            }
            if (parsedFood.amount() < 1) {
                LOGGER.warn("[{}] 忽略实体 {} 的无效{}数量: {} -> {}", TlDomesticateMoreCreatures.MOD_ID, entityId, source, parsedFood.item(), parsedFood.amount());
                continue;
            }
            result.put(itemId, new TamingFood(itemId, parsedFood.amount()));
        }
        return List.copyOf(result.values());
    }

    private static List<TamingRuleFileParser.ParsedRule> parseRules() {
        try {
            String content = Files.readString(PATH, StandardCharsets.UTF_8);
            if (TamingRuleFileParser.hasActiveLegacySections(content)) {
                LOGGER.error("[{}] 检测到旧版 taming_rules.toml 格式。当前版本只支持 rules = [{{...}}] 单条完整规则格式。", TlDomesticateMoreCreatures.MOD_ID);
                return List.of();
            }
            return TamingRuleFileParser.parse(content);
        } catch (IOException exception) {
            LOGGER.error("[{}] 读取 taming_rules.toml 失败。", TlDomesticateMoreCreatures.MOD_ID, exception);
            return List.of();
        }
    }

    private static void ensureFile() {
        try {
            if (Files.isRegularFile(PATH)) {
                String current = Files.readString(PATH, StandardCharsets.UTF_8);
                boolean hasLegacyText = current.contains("[[knockout]]")
                        || current.contains("[[feeding]]")
                        || current.contains("[[knockout.foods]]")
                        || current.contains("[[feeding.foods]]");
                if (hasLegacyText && !TamingRuleFileParser.hasActiveLegacySections(current)) {
                    Files.writeString(PATH, DEFAULT_FILE, StandardCharsets.UTF_8);
                }
                return;
            }
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, DEFAULT_FILE, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOGGER.error("[{}] 创建或更新 taming_rules.toml 失败。", TlDomesticateMoreCreatures.MOD_ID, exception);
        }
    }

    private static final String DEFAULT_FILE = """
            # TL Domesticate More Creatures 驯服规则
            #
            # 每一条配置都必须在同一个 { } 内完整写出实体、驯服方式和食物数量规则。
            # method 仅支持 FEED 或 KNOCKOUT。
            # required_player_level：玩家需要达到的 TDMC 等级，未填写时默认为 1。
            # FEED：生物清醒时直接喂食。
            # KNOCKOUT：必须先让生物进入眩晕状态，再喂食。
            # native_foods：为生物自身 isFood() 识别到的原生可食用物品逐项配置驯服数量。
            # 每一种原生食物都拥有独立 amount，不存在整只生物共用的默认数量。
            # 自动检测到但没有配置 amount 的原生食物不会显示在望远镜中，也不会增加驯服进度。
            # extra_foods：额外加入 TDMC 的驯服食物，不会改变原版繁殖、引诱或其他 isFood() 行为。
            # removed_native_foods：仅从 TDMC 驯服食物中排除原生 isFood() 食物，不改变原版繁殖或引诱。
            # foods：旧版兼容写法。属于原生食物时作为数量配置，不属于原生食物时作为额外驯服食物。
            #
            rules = [
                # { entity = "minecraft:cow", method = "FEED", required_player_level = 1, native_foods = [{ item = "minecraft:wheat", amount = 3 }] },
                # { entity = "minecraft:pig", method = "FEED", required_player_level = 10, native_foods = [{ item = "minecraft:carrot", amount = 5 }, { item = "minecraft:potato", amount = 4 }, { item = "minecraft:beetroot", amount = 6 }], extra_foods = [{ item = "minecraft:golden_carrot", amount = 2 }] },
                # { entity = "minecraft:zombie", method = "KNOCKOUT", required_player_level = 20, extra_foods = [{ item = "minecraft:rotten_flesh", amount = 10 }] },
                # { entity = "minecraft:ravager", method = "KNOCKOUT", required_player_level = 40, extra_foods = [{ item = "minecraft:beef", amount = 20 }, { item = "minecraft:golden_apple", amount = 2 }] }
            ]
            """;

    private record ResolvedFoods(List<TamingFoodDisplay> display, List<TamingFood> usable) {
        private static final ResolvedFoods EMPTY = new ResolvedFoods(List.of(), List.of());

        private ResolvedFoods {
            display = List.copyOf(display);
            usable = List.copyOf(usable);
        }
    }
}
