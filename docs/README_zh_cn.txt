TL Domesticate More Creatures 1.0.0
Mod ID：tl_domesticate_more_creatures
Minecraft 1.20.1 / Forge 47.4.10 / Java 17

一、主要功能
1. 玩家最低 Lv.1，默认最高 Lv.120；Lv.1 为 0 点，每次虚拟等级升级获得 1 个可分配属性点。
2. 野生生物默认在 Lv.1～Lv.150 中随机生成；Lv.N 默认拥有 N-1 个初始属性点。
3. 默认属性：生命、护甲、伤害、速度、游泳速度、抗性、眩晕。
4. 驯服生物可获得驯养等级加成，默认等于初始等级的 50%，对应属性点随机加入初始点，不可重置。
5. 默认可经验升级等级等于初始等级的 75%，驯养加成不会挤占经验升级次数。
6. 天赋在野生生物生成时随机获得 1～3 个，并支持遗传；天赋点不提高等级，并可突破普通属性点额度。
7. 虚拟经验独立于原版经验。玩家获得正数原版经验时也获得同值虚拟经验。满级参与者不会占用击杀经验均分名额。
8. 繁殖保留初始属性遗传、突变和天赋遗传；同一主人的双亲繁殖时子代自动归该主人，但不会获得额外驯养加成。
9. F 键打开属性面板，可在原版按键设置重新绑定。对准允许打开面板的已驯服宠物时打开宠物面板，否则打开玩家面板。
10. 使用原版望远镜观察生物时显示等级、天赋、属性、生命、眩晕；存在自定义驯服规则时还显示驯服方式、食物和驯服进度。
11. 属性重置水晶只返还升级后手动投入的属性点，不影响初始点、天赋点、等级和经验。
12. 宠物经验药水为未满级宠物直接增加配置中的虚拟经验。
13. 可选联动 tl_marking：未安装 tl_marking 时宠物指令系统不启用，其他功能不受影响。

二、模组身份与旧版迁移
新 Mod ID：
tl_domesticate_more_creatures

命令根：
/tdmc

旧版 tl_biological_attribute_panel 的 Progress NBT、属性/天赋配置和旧物品 ID 会进行兼容迁移。新版本只写入 tl_domesticate_more_creatures 命名空间。
FindMe Salvation 兼容已移除，本模组现在使用自己的驯服与主人数据。

三、全局 Forge 配置
首次运行后生成：
config/tl_domesticate_more_creatures-common.toml

配置文件内包含中文注释，主要分组包括：
level.*             等级、最高等级倍率、驯养加成
experience.*        虚拟经验与共享范围
breeding.*          遗传与突变
attributes.*        抗性等全局属性规则
entities.*          整个属性系统的实体黑白名单
panel.*             哪些已驯服实体可以打开宠物属性面板
torpor.*            眩晕基础值、倍率、恢复、麻醉箭、眩晕黑白名单
command.*           宠物指令范围、注意持续时间、防友伤

四、动态属性配置
首次运行后生成：
config/tl_domesticate_more_creatures/attributes.json

默认属性：
生命：minecraft:generic.max_health，每点 +5% 基础生命。
护甲：minecraft:generic.armor，每点 +1。
伤害：tl_domesticate_more_creatures:damage_multiplier，每点 +5% 最终伤害乘区。
速度：minecraft:generic.movement_speed，每点 +1% 基础速度。
游泳速度：forge:swim_speed，每点 +2% 基础游泳速度。
抗性：tl_domesticate_more_creatures:resistance，每点 +1% 减伤，普通点默认最高 20%，天赋点可突破。
眩晕：tl_domesticate_more_creatures:torpor，点数用于最大眩晕值公式。

眩晕最大值：
玩家基础值默认 1000，其他生物基础值默认 500。
最大眩晕值 = 基础眩晕值 × (1 + (眩晕总属性点 + 初始等级) × 0.05)
0.05 可通过 torpor.pointAndInitialLevelRate 配置。

五、眩晕与麻醉
非玩家眩晕系统可通过 torpor.filterMode 与 torpor.entities 使用 BLACKLIST/WHITELIST 管理；玩家始终拥有眩晕值。

当前眩晕没有达到最大值时实体正常行动；第一次达到最大值后进入昏迷。昏迷后即使眩晕已经低于最大值也不会苏醒，必须恢复到 0 才恢复行动。

没有麻醉效果时，默认每秒降低最大眩晕值的 1%。
麻醉效果默认每级每秒增加 25 点眩晕：麻醉 I=25/s、II=50/s、III=75/s。

麻醉箭 ID：
tl_domesticate_more_creatures:narcotic_arrow

默认：麻醉 I，持续 10 秒；支持弓和弩。
默认配方：1 支箭 + 1 个蜘蛛眼 -> 1 支麻醉箭。

六、自定义驯服规则
首次运行后生成：
config/tl_domesticate_more_creatures/taming_rules.toml

文件自带中文注释，支持两种方式：
[[knockout]]  眩晕驯服
[[feeding]]   喂食驯服

同一实体不能同时出现在两种方式中，否则该实体的自定义规则失效并打印错误。
每种方式都可以配置多个食物及 amount。amount 表示只使用该食物时达到 100% 驯服需要的数量，不同食物可以混合累计统一百分比进度。

眩晕驯服：只有昏迷时能喂食。生物苏醒时驯服进度、驯养者锁定和昏迷期间伤害记录全部清零。
喂食驯服：清醒时直接喂食，进度保留直到驯服成功。
第一次有效喂食的玩家会锁定为该次驯养者。

眩晕驯服时，昏迷后的伤害会降低驯养加成：
驯养效率 = max(0, 1 - 昏迷期间累计伤害 / 进入昏迷时最大生命值)
实际驯养加成 = 默认驯养加成 × 驯养效率
治疗不会恢复已经损失的驯养效率。

七、天赋配置
首次运行后生成：
config/tl_domesticate_more_creatures/talents.json

天赋池可新增、删除和调整权重、等级范围、每一级提供的属性点。天赋点单独记录，不增加等级。

八、单实体数据包规则
监听目录：
data/<任意命名空间>/entity_rules/*.json

示例见：
docs/entity_rule_example.json

单实体规则可以覆盖 enabled、minLevel、maxLevel、fixedLevel、maxLevelMultiplier、killExperienceMultiplier、canGainExperience、canLevelUp、resistanceCap、randomWeights、fixedStats、statOverrides。

九、宠物 AI 与防友伤
本模组自定义驯服的宠物拥有独立主人 UUID。默认跟随主人、协助主人攻击、主人受击时反击。
默认阻止主人与自己宠物、同主人宠物以及可识别的同队友军互相造成伤害，并追踪普通攻击、投射物和可追溯来源的范围伤害。

十、tl_marking 宠物指令
只有安装 tl_marking 时启用。

Shift + 鼠标中键对准自己的宠物：加入/移出多选集合。
存在选中宠物时，命令只作用于选中的宠物；没有选中时作用于同维度、已加载、默认 64 格内的全部自己的宠物。

普通单击中键：
对准方块/地面 -> 前往。
对准可攻击实体 -> 攻击。
对准友方实体不会生成攻击指令。

长按中键继续使用 tl_marking 的六格轮盘：
注意：停止当前攻击并警戒，默认 10 秒。
集合：前往标点并集合，到达后恢复普通 AI。
防守：前往并持续守卫标点区域。
危险：避开标点危险区域。
攻击：集中攻击目标；位置标点会攻击附近敌对目标。
撤退：停止战斗并返回主人。

十一、管理员命令
/tdmc reload
/tdmc info [target]
/tdmc level set <level> [target]
/tdmc xp add <amount> [target]
/tdmc points add <amount> [target]
/tdmc stat set <stat> <points> [target]
/tdmc reset [target]

/tdmc reload 会重新读取 attributes.json、talents.json、taming_rules.toml 和数据包实体规则，并重新应用已加载实体。

十二、模组物品
创造模式有独立的“驯养更多生物”页签。

属性重置水晶：
tl_domesticate_more_creatures:attribute_reset_crystal

宠物经验药水：
tl_domesticate_more_creatures:pet_experience_potion

麻醉箭：
tl_domesticate_more_creatures:narcotic_arrow
