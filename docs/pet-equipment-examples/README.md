# 宠物装备配置示例

运行游戏后，TDMC 会创建：

- `config/tl_domesticate_more_creatures/pet_equipment/entities/`
- `config/tl_domesticate_more_creatures/pet_equipment/items/`

`entity-profile.json` 展示如何给某个 EntityType 定义动态装备槽。将实际实体 ID 填入 `entityId` 后，把文件放进 `entities`。

`item-rule.json` 展示如何把一个普通 Item 声明为宠物装备，并限制可装备实体、槽位以及附加属性。替换实际物品/实体 ID 后，把文件放进 `items`。

标准槽的 `vanillaSlot` 可使用 `HEAD`、`CHEST`、`LEGS`、`FEET`。自定义槽将 `vanillaSlot` 留空。

`allowAnyItem=true` 会允许任意物品进入该槽；通常建议保持 `false`，再通过 item rule 精确声明宠物装备。

支持的额外属性操作：`ADDITION`、`MULTIPLY_BASE`、`MULTIPLY_TOTAL`。
