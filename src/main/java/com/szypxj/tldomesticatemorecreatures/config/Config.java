package com.szypxj.tldomesticatemorecreatures.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue WILD_MIN_LEVEL = BUILDER
            .comment("野生生物随机生成时的最低初始等级。")
            .defineInRange("level.wildMinLevel", 1, 1, 1_000_000);

    public static final ForgeConfigSpec.IntValue WILD_MAX_LEVEL = BUILDER
            .comment("野生生物随机生成时的最高初始等级。普通遗传后代的初始等级也会受到该上限限制，突变规则除外。")
            .defineInRange("level.wildMaxLevel", 150, 1, 1_000_000);

    public static final ForgeConfigSpec.DoubleValue MAX_LEVEL_MULTIPLIER = BUILDER
            .comment("宠物可通过经验升级的等级倍率。实际可升级等级 = 初始等级 × (该值 - 1)。默认 1.75 表示可额外升级初始等级的 75%。")
            .defineInRange("level.maxLevelMultiplier", 1.75D, 1.0D, 100.0D);

    public static final ForgeConfigSpec.DoubleValue TAMING_BONUS_RATE = BUILDER
            .comment("宠物被驯服时立即获得的驯养加成比例。默认 0.50 表示获得初始等级 50% 的额外等级，并随机获得等量初始属性点。")
            .defineInRange("level.tamingBonusRate", 0.50D, 0.0D, 100.0D);

    public static final ForgeConfigSpec.IntValue PLAYER_MAX_LEVEL = BUILDER
            .comment("玩家能够达到的最高等级。玩家不使用驯养加成。")
            .defineInRange("level.playerMaxLevel", 120, 1, 1_000_000);

    public static final ForgeConfigSpec.DoubleValue KILL_BASE_XP = BUILDER
            .comment("击杀生物时计算虚拟经验使用的基础经验值。")
            .defineInRange("experience.killBaseExperience", 1.0D, 0.0D, 1_000_000.0D);

    public static final ForgeConfigSpec.DoubleValue KILL_XP_MULTIPLIER = BUILDER
            .comment("击杀生物获得虚拟经验的全局倍率。")
            .defineInRange("experience.killExperienceMultiplier", 1.0D, 0.0D, 1_000_000.0D);

    public static final ForgeConfigSpec.DoubleValue LEVEL_BASE_XP = BUILDER
            .comment("玩家与宠物升级所需虚拟经验的基础值。")
            .defineInRange("experience.levelBaseExperience", 30.0D, 0.001D, 1_000_000.0D);

    public static final ForgeConfigSpec.DoubleValue LEVEL_RATE = BUILDER
            .comment("升级经验需求倍率。数值越高，每一级需要的虚拟经验越多。")
            .defineInRange("experience.levelRate", 1.0D, 0.001D, 1_000_000.0D);

    public static final ForgeConfigSpec.IntValue COMBAT_PARTICIPATION_SECONDS = BUILDER
            .comment("宠物或玩家对目标造成伤害后，在多少秒内目标死亡仍视为参与击杀。")
            .defineInRange("experience.combatParticipationSeconds", 10, 1, 3600);

    public static final ForgeConfigSpec.DoubleValue PET_EXPERIENCE_SHARE_RANGE = BUILDER
            .comment("宠物击杀或参与击杀时，与主人共享虚拟经验的最大距离，单位为方块。")
            .defineInRange("experience.petExperienceShareRange", 32.0D, 0.0D, 4096.0D);

    public static final ForgeConfigSpec.IntValue PET_EXPERIENCE_POTION_AMOUNT = BUILDER
            .comment("宠物经验药水成功使用后直接提供的虚拟经验数量。")
            .defineInRange("experience.petExperiencePotionAmount", 2000, 1, 1_000_000_000);

    public static final ForgeConfigSpec.DoubleValue ELITE_SPAWN_CHANCE = BUILDER
            .comment("非繁殖生物首次初始化时成为精英变种的默认概率。0.05 表示 5%。拥有 forge:bosses 实体标签的 Boss 始终为精英，不受该概率影响。")
            .defineInRange("elite.spawnChance", 0.05D, 0.0D, 1.0D);


    public static final ForgeConfigSpec.DoubleValue ELITE_KILL_BASE_XP = BUILDER
            .comment("击杀精英生物时使用的基础虚拟经验。仍会继续乘以生物等级、全局倍率和实体规则经验倍率。")
            .defineInRange("elite.killBaseExperience", 5.0D, 0.0D, 1_000_000.0D);

    public static final ForgeConfigSpec.ConfigValue<String> ELITE_FILTER_MODE = BUILDER
            .comment("精英随机生成过滤模式。BLACKLIST 表示名单中的实体不会随机成为精英；WHITELIST 表示只有名单中的实体会随机成为精英。forge:bosses 不受该过滤影响。")
            .defineInList("elite.filterMode", "BLACKLIST", List.of("BLACKLIST", "WHITELIST"));

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ELITE_ENTITIES = BUILDER
            .comment("精英随机生成实体名单，填写完整实体 ID，例如 minecraft:zombie。forge:bosses 不受该名单影响。")
            .defineListAllowEmpty("elite.entities", List.of(), value -> value instanceof String);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ELITE_ENTITY_CHANCES = BUILDER
            .comment("按实体覆盖精英生成概率，格式为 实体ID=概率，例如 minecraft:zombie=0.10。未配置的实体使用 elite.spawnChance。forge:bosses 始终为 100%。")
            .defineListAllowEmpty("elite.entityChances", List.of(), value -> value instanceof String);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ELITE_TAMEABLE_ENTITIES = BUILDER
            .comment("精英可驯服白名单。名单内实体即使生成成精英也允许使用其原有驯服方式；成功驯服后会移除精英状态与精英初始属性加成。")
            .defineListAllowEmpty("elite.tameableEntities", List.of(), value -> value instanceof String);

    public static final ForgeConfigSpec.IntValue ELITE_TALENT_LEVEL_ROLLS = BUILDER
            .comment("精英生成天赋等级时的随机次数，最终取最高结果。1 表示与普通生物相同，默认 2 会更容易获得高等级天赋。")
            .defineInRange("elite.talentLevelRolls", 2, 1, 100);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ELITE_COMBAT_STATS = BUILDER
            .comment("精英初始属性点分配时提高权重的战斗属性 ID。")
            .defineListAllowEmpty("elite.combatStats", List.of("health", "damage", "resistance"), value -> value instanceof String);

    public static final ForgeConfigSpec.DoubleValue ELITE_COMBAT_STAT_WEIGHT_MULTIPLIER = BUILDER
            .comment("精英初始属性点分配到 elite.combatStats 中属性时的随机权重倍率。默认 2.0。")
            .defineInRange("elite.combatStatWeightMultiplier", 2.0D, 0.0D, 1000.0D);

    public static final ForgeConfigSpec.DoubleValue INHERIT_HIGHER_STAT_CHANCE = BUILDER
            .comment("普通遗传时，每项属性优先继承父母双方较高初始点的概率。0.55 表示 55%。")
            .defineInRange("breeding.inheritHigherStatChance", 0.55D, 0.0D, 1.0D);

    public static final ForgeConfigSpec.DoubleValue MUTATION_CHANCE = BUILDER
            .comment("后代发生遗传突变的概率。0.01 表示 1%。")
            .defineInRange("breeding.mutationChance", 0.01D, 0.0D, 1.0D);

    public static final ForgeConfigSpec.IntValue IMPRINT_DURATION_MINUTES = BUILDER
            .comment("繁殖子代出生后的留痕最长持续时间，单位为分钟。龙蛋阶段不计时；子代成年或完成全部三次需求时会提前结束。")
            .defineInRange("breeding.imprintDurationMinutes", 20, 1, 1440);

    public static final ForgeConfigSpec.DoubleValue RESISTANCE_CAP = BUILDER
            .comment("普通属性点能够提供的抗性减伤上限。0.20 表示 20%。天赋点提供的抗性可以突破该上限。")
            .defineInRange("attributes.defaultResistanceCap", 0.20D, 0.0D, 1.0D);

    public static final ForgeConfigSpec.DoubleValue ARS_NOUVEAU_MANA_PER_POINT = BUILDER
            .comment("安装新生魔艺时，玩家每分配 1 点 TDMC 魔源属性所增加的基础魔源。只增加 TDMC 自己的基础贡献，不覆盖新生魔艺的装备、法术书或其他魔源加成。")
            .defineInRange("compat.arsNouveau.manaPerPoint", 10.0D, 0.1D, 1_000_000.0D);

    public static final ForgeConfigSpec.ConfigValue<String> ENTITY_FILTER_MODE = BUILDER
            .comment("实体过滤模式。BLACKLIST 表示名单中的实体不受系统影响；WHITELIST 表示只有名单中的实体受系统影响。")
            .define("entities.filterMode", "BLACKLIST");

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_FILTER = BUILDER
            .comment("实体过滤名单，填写完整实体 ID，例如 minecraft:armor_stand。BLACKLIST 模式下该单实体黑名单优先级最高，即使实体位于整模组黑名单例外名单中也不会重新启用。")
            .defineListAllowEmpty("entities.filter", List.of(
                    "minecraft:armor_stand",
                    "minecraft:allay",
                    "minecraft:axolotl",
                    "minecraft:bat",
                    "minecraft:bee",
                    "minecraft:chicken",
                    "minecraft:cow",
                    "minecraft:donkey",
                    "minecraft:goat",
                    "minecraft:fox",
                    "minecraft:frog",
                    "minecraft:glow_squid",
                    "minecraft:llama",
                    "minecraft:mooshroom",
                    "minecraft:parrot",
                    "minecraft:pig",
                    "minecraft:pufferfish",
                    "minecraft:rabbit",
                    "minecraft:salmon",
                    "minecraft:sheep",
                    "minecraft:sniffer",
                    "minecraft:squid",
                    "minecraft:tadpole",
                    "minecraft:trader_llama",
                    "minecraft:tropical_fish",
                    "minecraft:turtle",
                    "minecraft:wandering_trader",
                    "minecraft:villager"
            ), value -> value instanceof String);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_MOD_BLACKLIST = BUILDER
            .comment("整模组实体黑名单，只填写 modid，例如 examplemod。名单内模组的非玩家生物默认完全不受本模组系统影响。")
            .defineListAllowEmpty("entities.modBlacklist", List.of(), value -> value instanceof String);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENTITY_MOD_BLACKLIST_EXCEPTIONS = BUILDER
            .comment("整模组实体黑名单例外名单，填写完整实体 ID，例如 examplemod:dragon。例外实体会重新进入普通实体过滤规则；如果它同时位于 entities.filter 的 BLACKLIST 中，仍然保持禁用。")
            .defineListAllowEmpty("entities.modBlacklistExceptions", List.of(), value -> value instanceof String);

    public static final ForgeConfigSpec.ConfigValue<String> PANEL_TAMEABLE_ENTITY_MODE = BUILDER
            .comment("宠物面板实体筛选模式。BLACKLIST 表示名单中的实体不能作为本模组宠物使用；WHITELIST 表示只有名单中的实体可以作为本模组宠物使用。")
            .defineInList("panel.tameableEntityMode", "BLACKLIST", List.of("BLACKLIST", "WHITELIST"));

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PANEL_TAMEABLE_ENTITIES = BUILDER
            .comment("宠物面板实体名单，填写完整实体 ID，例如 minecraft:wolf。使用 WHITELIST 时只有名单中的已驯服实体可以打开宠物属性面板并在面板中加点；使用 BLACKLIST 时名单中的实体不会开放宠物属性面板。该名单只控制面板权限，不影响驯服、宠物 AI、经验共享或宠物道具。")
            .defineListAllowEmpty("panel.tameableEntities", List.of(), value -> value instanceof String);


    public static final ForgeConfigSpec.DoubleValue INSPECT_HANDHELD_RANGE = BUILDER
            .comment("仅手持望远镜但未使用时，可以查看生物属性面板的最大距离，单位为方块。")
            .defineInRange("inspect.handheldRange", 8.0D, 1.0D, 256.0D);

    public static final ForgeConfigSpec.DoubleValue INSPECT_SPYGLASS_RANGE = BUILDER
            .comment("正在使用望远镜时，可以查看生物属性面板的最大距离，单位为方块。")
            .defineInRange("inspect.spyglassRange", 64.0D, 1.0D, 1024.0D);

    public static final ForgeConfigSpec.DoubleValue TORPOR_MOB_HEALTH_MULTIPLIER = BUILDER
            .comment("玩家和非玩家生物的最终最大生命值转化为最大眩晕值的倍率。默认 5.0。公式：最终最大生命值 × 该值。")
            .defineInRange("torpor.mobHealthMultiplier", 5.0D, 0.0D, 1_000_000.0D);

    public static final ForgeConfigSpec.DoubleValue TORPOR_RECOVERY_BASE_DURATION_SECONDS = BUILDER
            .comment("眩晕自然下降的基础完整苏醒时间，单位为秒。默认 120。完整苏醒时间 = 该值 + recoveryDurationScaleSeconds × sqrt(最大眩晕值 / 500)。")
            .defineInRange("torpor.recoveryBaseDurationSeconds", 120.0D, 1.0D, 86_400.0D);

    public static final ForgeConfigSpec.DoubleValue TORPOR_RECOVERY_DURATION_SCALE_SECONDS = BUILDER
            .comment("最大眩晕值越高时额外增加的完整苏醒时间缩放值，单位为秒。默认 20。完整苏醒时间 = recoveryBaseDurationSeconds + 该值 × sqrt(最大眩晕值 / 500)。")
            .defineInRange("torpor.recoveryDurationScaleSeconds", 20.0D, 0.0D, 86_400.0D);

    public static final ForgeConfigSpec.IntValue TORPOR_RECOVERY_DELAY_SECONDS = BUILDER
            .comment("最后一次增加眩晕值后，需要连续多少秒没有再次增加眩晕，才开始自然下降。任何正向眩晕来源都会重新计时。")
            .defineInRange("torpor.recoveryDelaySeconds", 10, 0, 3600);

    public static final ForgeConfigSpec.DoubleValue TORPOR_DAMAGE_WAKE_MULTIPLIER = BUILDER
            .comment("眩晕状态下每受到 1 点最终实际伤害会降低多少眩晕值。默认 2.0 表示受到 10 点伤害降低 20 点眩晕。若本次伤害足以致死，则眩晕立即清零并保留 1 点生命。")
            .defineInRange("torpor.damageWakeMultiplier", 2.0D, 0.0D, 1_000_000.0D);

    public static final ForgeConfigSpec.ConfigValue<String> TORPOR_FILTER_MODE = BUILDER
            .comment("非玩家生物眩晕系统过滤模式。BLACKLIST 表示默认全部启用并排除名单；WHITELIST 表示只启用名单中的实体。玩家始终启用眩晕系统。")
            .defineInList("torpor.filterMode", "BLACKLIST", List.of("BLACKLIST", "WHITELIST"));

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TORPOR_ENTITIES = BUILDER
            .comment("非玩家生物眩晕系统实体名单，填写完整实体 ID，例如 minecraft:zombie。")
            .defineListAllowEmpty("torpor.entities", List.of(), value -> value instanceof String);

    public static final ForgeConfigSpec.DoubleValue NARCOTIC_TORPOR_PER_LEVEL = BUILDER
            .comment("麻醉效果每一级每秒增加的眩晕值。默认 25，因此麻醉 I=25/秒、II=50/秒、III=75/秒。")
            .defineInRange("torpor.narcoticPerLevelPerSecond", 25.0D, 0.0D, 1_000_000.0D);

    public static final ForgeConfigSpec.IntValue NARCOTIC_ARROW_DURATION_SECONDS = BUILDER
            .comment("麻醉箭命中后麻醉效果持续时间，单位为秒。")
            .defineInRange("torpor.narcoticArrowDurationSeconds", 5, 1, 3600);

    public static final ForgeConfigSpec.DoubleValue CLUB_DAMAGE = BUILDER
            .comment("大棒的基础攻击伤害。默认 15。")
            .defineInRange("torporWeapons.club.damage", 15.0D, 0.0D, 1_000_000.0D);

    public static final ForgeConfigSpec.DoubleValue CLUB_FIXED_TORPOR = BUILDER
            .comment("大棒命中后，在本次最终实际伤害之外额外增加的固定眩晕值。默认 75。")
            .defineInRange("torporWeapons.club.fixedTorpor", 75.0D, 0.0D, 1_000_000.0D);

    public static final ForgeConfigSpec.IntValue PET_MANAGEMENT_BASE_STORAGE_CAPACITY = BUILDER
            .comment("宠物空间基础容量。实际容量 = 该值 + 玩家TDMC等级 × 每级容量。")
            .defineInRange("petManagement.baseStorageCapacity", 10, 0, 1_000_000);

    public static final ForgeConfigSpec.IntValue PET_MANAGEMENT_STORAGE_CAPACITY_PER_PLAYER_LEVEL = BUILDER
            .comment("玩家每 1 个 TDMC 等级为宠物空间增加的容量。")
            .defineInRange("petManagement.storageCapacityPerPlayerLevel", 1, 0, 1_000_000);

    public static final ForgeConfigSpec.IntValue PET_MANAGEMENT_REMOTE_STORE_COMBAT_COOLDOWN_SECONDS = BUILDER
            .comment("宠物最近受伤后多少秒内禁止远程存入宠物空间。")
            .defineInRange("petManagement.remoteStoreCombatCooldownSeconds", 10, 0, 3600);

    public static final ForgeConfigSpec.IntValue PET_MANAGEMENT_NEARBY_SUMMON_RANGE = BUILDER
            .comment("同维度宠物在该距离内时直接从真实位置赶来，不执行远距离安全入场。")
            .defineInRange("petManagement.nearbySummonRange", 64, 1, 4096);

    public static final ForgeConfigSpec.IntValue PET_MANAGEMENT_ENTRY_MIN_DISTANCE = BUILDER
            .comment("远距离召唤时宠物在玩家视角后方安全入场的最小距离。")
            .defineInRange("petManagement.entryMinDistance", 10, 4, 18);

    public static final ForgeConfigSpec.IntValue PET_MANAGEMENT_ENTRY_MAX_DISTANCE = BUILDER
            .comment("远距离召唤时宠物在玩家视角后方安全入场的最大距离。")
            .defineInRange("petManagement.entryMaxDistance", 18, 4, 18);

    public static final ForgeConfigSpec.IntValue PET_COMMAND_RANGE = BUILDER
            .comment("宠物指令影响范围，单位为方块。只有同维度、已加载且属于当前玩家的宠物会响应。")
            .defineInRange("command.petCommandRange", 64, 1, 1024);

    public static final ForgeConfigSpec.IntValue CAUTION_DURATION_SECONDS = BUILDER
            .comment("注意指令持续时间，单位为秒。到期后宠物恢复普通行为。")
            .defineInRange("command.cautionDurationSeconds", 10, 1, 3600);

    public static final ForgeConfigSpec.BooleanValue PREVENT_PET_FRIENDLY_FIRE = BUILDER
            .comment("是否阻止主人、同主人宠物以及同队友军之间的友方伤害。包括可追溯攻击来源的投射物和范围伤害。")
            .define("command.preventPetFriendlyFire", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
