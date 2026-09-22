package com.szypxj.tldomesticatemorecreatures.client.taming;

import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcButton;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcConfirmScreen;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcEditBox;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcUiTheme;
import com.szypxj.tldomesticatemorecreatures.domestication.TamingMethod;
import com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorEntityInfo;
import com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorRule;
import com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorService;
import com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorSnapshot;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TamingEditorScreen extends Screen {
    private static final int LEFT_WIDTH = 216;
    private static final int LIST_TOP = 88;
    private static final int ENTITY_ROW_HEIGHT = 30;
    private static final int FOOD_ROW_HEIGHT = 42;
    private static final int HEADER_ROW_HEIGHT = 22;
    private static final int SCROLLBAR_WIDTH = 7;

    private final TamingEditorModel model;
    private TdmcEditBox searchBox;
    private TdmcEditBox levelBox;
    private final List<AmountBoxBinding> amountBoxes = new ArrayList<>();
    private List<ResourceLocation> visibleEntities = List.of();
    private List<FoodRow> foodRows = List.of();
    private String searchText = "";
    private ListFilter filter = ListFilter.ALL;
    private int entityScroll;
    private int foodScroll;
    private int maxEntityScroll;
    private int maxFoodScroll;
    private boolean draggingEntityScrollbar;
    private boolean draggingFoodScrollbar;
    private Component statusMessage = Component.empty();
    private boolean pendingCloseAfterSave;
    private boolean pendingSave;
    private boolean pendingReload;

    public TamingEditorScreen(TamingEditorSnapshot snapshot) {
        this(new TamingEditorModel(snapshot));
    }

    private TamingEditorScreen(TamingEditorModel model) {
        super(Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.title"));
        this.model = model;
    }

    TamingEditorModel model() {
        return model;
    }

    @Override
    protected void init() {
        amountBoxes.clear();
        int top = 8;
        int right = width - 8;
        addRenderableWidget(TdmcButton.create(right - 216, top, 68, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.save"),
                button -> saveAll(false)));
        addRenderableWidget(TdmcButton.create(right - 144, top, 68, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.reload"),
                button -> requestReload()));
        addRenderableWidget(TdmcButton.create(right - 72, top, 68, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.done"),
                button -> onClose()));

        searchBox = new TdmcEditBox(font, 8, 36, LEFT_WIDTH - 16, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.search"));
        searchBox.setValue(searchText);
        searchBox.setResponder(value -> {
            searchText = value;
            entityScroll = 0;
            rebuildVisibleEntities();
        });
        addRenderableWidget(searchBox);

        addRenderableWidget(TdmcButton.create(8, 62, LEFT_WIDTH - 16, 20,
                Component.translatable(filter.key),
                button -> {
                    filter = filter == ListFilter.ALL ? ListFilter.CONFIGURED : ListFilter.ALL;
                    entityScroll = 0;
                    rebuildEditorWidgets();
                }));

        rebuildVisibleEntities();
        if (model.selected() == null && !visibleEntities.isEmpty()) {
            selectEntity(visibleEntities.get(0));
        }
        buildRightWidgets();
    }

    private void rebuildEditorWidgets() {
        if (searchBox != null) {
            searchText = searchBox.getValue();
        }
        clearWidgets();
        init();
    }

    private void rebuildVisibleEntities() {
        String query = searchText.trim().toLowerCase(Locale.ROOT);
        List<ResourceLocation> result = new ArrayList<>();
        for (TamingEditorEntityInfo info : model.entities()) {
            ResourceLocation id = info.entityId();
            if (filter == ListFilter.CONFIGURED && !model.hasRule(id)) {
                continue;
            }
            String name = entityDisplayName(id);
            if (!query.isEmpty()
                    && !id.toString().toLowerCase(Locale.ROOT).contains(query)
                    && !name.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            result.add(id);
        }
        result.sort(Comparator
                .comparing((ResourceLocation id) -> !model.hasRule(id))
                .thenComparing(this::entityDisplayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ResourceLocation::toString));
        visibleEntities = List.copyOf(result);
        maxEntityScroll = Math.max(0, visibleEntities.size() * ENTITY_ROW_HEIGHT - Math.max(1, height - LIST_TOP - 10));
        entityScroll = Mth.clamp(entityScroll, 0, maxEntityScroll);
    }

    private void buildRightWidgets() {
        ResourceLocation selected = model.selected();
        if (selected == null) {
            return;
        }
        int rightX = LEFT_WIDTH + 14;
        int rightWidth = Math.max(180, width - rightX - 12);
        int controlY = 88;
        TamingEditorRule rule = model.rule(selected);
        if (rule == null) {
            TdmcButton enable = TdmcButton.create(rightX, controlY, Math.min(150, rightWidth), 20,
                    Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.enable"),
                    button -> {
                        model.enable(selected);
                        foodScroll = 0;
                        rebuildEditorWidgets();
                    });
            enable.active = model.affected(selected);
            addRenderableWidget(enable);
            ensureNativeFoods(selected);
            return;
        }

        addRenderableWidget(TdmcButton.create(rightX, controlY, 150, 20,
                Component.translatable(rule.method() == TamingMethod.FEED
                        ? "gui.tl_domesticate_more_creatures.taming_editor.method.feed"
                        : "gui.tl_domesticate_more_creatures.taming_editor.method.knockout"),
                button -> {
                    model.cycleMethod(selected);
                    rebuildEditorWidgets();
                }));

        int levelX = rightX + 160;
        levelBox = new TdmcEditBox(font, levelX, controlY, 62, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.level"));
        levelBox.setMaxLength(5);
        levelBox.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        levelBox.setValue(Integer.toString(rule.requiredPlayerLevel()));
        levelBox.setResponder(value -> {
            if (!value.isEmpty()) {
                try {
                    model.setRequiredLevel(selected, Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                }
            }
        });
        addRenderableWidget(levelBox);

        addRenderableWidget(TdmcButton.create(rightX + Math.max(230, rightWidth - 150), controlY, 150, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.delete_rule"),
                button -> {
                    model.delete(selected);
                    foodScroll = 0;
                    rebuildEditorWidgets();
                }));

        ensureNativeFoods(selected);
        rebuildFoodRows();
        buildAmountBoxes();
    }

    private void ensureNativeFoods(ResourceLocation id) {
        if (id == null || model.hasNativeFoods(id) || model.nativePending(id)) {
            return;
        }
        model.markNativePending(id);
        NetworkHandler.requestTamingNativeFoods(id);
    }

    private void rebuildFoodRows() {
        ResourceLocation selected = model.selected();
        TamingEditorRule rule = model.rule(selected);
        if (selected == null || rule == null) {
            foodRows = List.of();
            maxFoodScroll = 0;
            return;
        }
        List<FoodRow> rows = new ArrayList<>();
        rows.add(FoodRow.header("gui.tl_domesticate_more_creatures.taming_editor.section.native"));
        for (ResourceLocation itemId : model.nativeItems(selected)) {
            if (!rule.removedNativeFoods().contains(itemId)) {
                rows.add(new FoodRow(FoodRowKind.NATIVE, itemId, null));
            }
        }
        rows.add(FoodRow.header("gui.tl_domesticate_more_creatures.taming_editor.section.extra"));
        rule.extraFoods().keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(id -> rows.add(new FoodRow(FoodRowKind.EXTRA, id, null)));
        rows.add(new FoodRow(FoodRowKind.ADD_EXTRA, null, null));
        rows.add(FoodRow.header("gui.tl_domesticate_more_creatures.taming_editor.section.removed"));
        rule.removedNativeFoods().stream().sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(id -> rows.add(new FoodRow(FoodRowKind.REMOVED, id, null)));
        foodRows = List.copyOf(rows);
        maxFoodScroll = Math.max(0, totalFoodHeight() - Math.max(1, height - foodListTop() - 14));
        foodScroll = Mth.clamp(foodScroll, 0, maxFoodScroll);
    }

    private void buildAmountBoxes() {
        ResourceLocation entityId = model.selected();
        TamingEditorRule rule = model.rule(entityId);
        if (entityId == null || rule == null) {
            return;
        }
        int areaLeft = LEFT_WIDTH + 14;
        int areaRight = width - 22;
        int top = foodListTop();
        int bottom = height - 12;
        int y = top - foodScroll;
        for (FoodRow row : foodRows) {
            int heightForRow = row.height();
            if ((row.kind == FoodRowKind.NATIVE || row.kind == FoodRowKind.EXTRA)
                    && y + heightForRow > top && y < bottom) {
                int rowRight = areaRight - SCROLLBAR_WIDTH - 4;
                int boxX = Math.max(areaLeft + 180, rowRight - 164);
                TdmcEditBox box = new TdmcEditBox(font, boxX, y + 12, 64, 20,
                        Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.amount"));
                box.setMaxLength(6);
                box.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
                int amount = row.kind == FoodRowKind.NATIVE
                        ? rule.nativeFoods().getOrDefault(row.itemId, 0)
                        : rule.extraFoods().getOrDefault(row.itemId, 0);
                box.setValue(amount > 0 ? Integer.toString(amount) : "");
                ResourceLocation itemId = row.itemId;
                FoodRowKind kind = row.kind;
                box.setResponder(value -> {
                    int parsed = 0;
                    if (!value.isEmpty()) {
                        try {
                            parsed = Integer.parseInt(value);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    if (kind == FoodRowKind.NATIVE) {
                        model.setNativeAmount(entityId, itemId, parsed);
                    } else {
                        model.setExtraAmount(entityId, itemId, parsed);
                    }
                });
                addRenderableWidget(box);
                amountBoxes.add(new AmountBoxBinding(box, kind, itemId));
            }
            y += heightForRow;
        }
    }

    private void rebuildRightWidgetsOnly() {
        rebuildEditorWidgets();
    }

    public void onNativeFoods(ResourceLocation entityId, TamingEditorService.NativeFoodsResult result) {
        model.applyNativeFoods(entityId, result);
        if (entityId != null && entityId.equals(model.selected())) {
            foodScroll = 0;
            rebuildEditorWidgets();
        }
    }

    public void onOperationResult(boolean saveOperation, boolean success, String messageKey) {
        if (success) {
            return;
        }
        if (saveOperation) {
            pendingSave = false;
            pendingCloseAfterSave = false;
        } else {
            pendingReload = false;
        }
        statusMessage = messageKey == null || messageKey.isBlank()
                ? Component.translatable(saveOperation
                        ? "msg.tl_domesticate_more_creatures.taming_editor.save_failed"
                        : "msg.tl_domesticate_more_creatures.taming_editor.reload_failed")
                : Component.translatable(messageKey);
    }

    public void onServerSnapshot(TamingEditorSnapshot snapshot) {
        model.applySnapshot(snapshot);
        if (pendingSave) {
            statusMessage = Component.translatable("msg.tl_domesticate_more_creatures.taming_editor.saved");
        } else if (pendingReload) {
            statusMessage = Component.translatable("msg.tl_domesticate_more_creatures.taming_editor.reloaded");
        } else {
            statusMessage = Component.empty();
        }
        pendingSave = false;
        pendingReload = false;
        if (pendingCloseAfterSave && minecraft != null) {
            pendingCloseAfterSave = false;
            minecraft.setScreen(null);
            return;
        }
        rebuildEditorWidgets();
    }

    void addExtraFood(ResourceLocation itemId) {
        ResourceLocation selected = model.selected();
        if (selected == null || itemId == null) {
            return;
        }
        model.addExtra(selected, itemId);
        rebuildEditorWidgets();
        scrollToExtra(itemId);
    }

    Set<ResourceLocation> blockedPickerItems() {
        ResourceLocation selected = model.selected();
        TamingEditorRule rule = model.rule(selected);
        Set<ResourceLocation> blocked = new LinkedHashSet<>(model.nativeItems(selected));
        if (rule != null) {
            blocked.addAll(rule.extraFoods().keySet());
            blocked.addAll(rule.removedNativeFoods());
        }
        return Set.copyOf(blocked);
    }

    private void scrollToExtra(ResourceLocation itemId) {
        int y = 0;
        for (FoodRow row : foodRows) {
            if (row.kind == FoodRowKind.EXTRA && itemId.equals(row.itemId)) {
                foodScroll = Mth.clamp(y - 18, 0, maxFoodScroll);
                rebuildEditorWidgets();
                return;
            }
            y += row.height();
        }
    }

    private void saveAll(boolean closeAfter) {
        if (!model.anyDirty()) {
            if (closeAfter && minecraft != null) {
                minecraft.setScreen(null);
            }
            return;
        }
        TamingEditorModel.ValidationIssue validation = model.validateAllDirtyIssue();
        if (!validation.valid()) {
            statusMessage = entityValidationMessage(validation.entityId(), validation.messageKey());
            return;
        }
        pendingCloseAfterSave = closeAfter;
        pendingSave = true;
        pendingReload = false;
        statusMessage = Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.saving");
        NetworkHandler.saveTamingEditor(model.dirtyUpserts(), model.deletes());
    }


    private Component entityValidationMessage(ResourceLocation entityId, String validationKey) {
        if (entityId == null || validationKey == null || validationKey.isBlank()) {
            return Component.translatable("msg.tl_domesticate_more_creatures.taming_editor.invalid_rule");
        }
        Component entityName = entityDisplayComponent(entityId);
        return Component.translatable(
                "msg.tl_domesticate_more_creatures.taming_editor.entity_error",
                entityName,
                entityId.toString(),
                Component.translatable(validationKey)
        );
    }

    private void requestReload() {
        if (minecraft == null) {
            return;
        }
        if (!model.anyDirty()) {
            pendingReload = true;
            pendingSave = false;
            NetworkHandler.reloadTamingEditor();
            return;
        }
        minecraft.setScreen(new TdmcConfirmScreen(
                accepted -> {
                    if (accepted) {
                        pendingReload = true;
                        pendingSave = false;
                        NetworkHandler.reloadTamingEditor();
                    }
                    if (minecraft != null) {
                        minecraft.setScreen(this);
                    }
                },
                Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.reload_confirm_title"),
                Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.reload_confirm_message"),
                Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.reload_discard"),
                Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.cancel")
        ));
    }

    @Override
    public void onClose() {
        if (minecraft == null) {
            return;
        }
        if (!model.anyDirty()) {
            minecraft.setScreen(null);
            return;
        }
        minecraft.setScreen(new TamingEditorExitConfirmScreen(choice -> {
            if (minecraft == null) {
                return;
            }
            switch (choice) {
                case SAVE -> {
                    minecraft.setScreen(this);
                    saveAll(true);
                }
                case DISCARD -> minecraft.setScreen(null);
                case CANCEL -> minecraft.setScreen(this);
            }
        }));
    }

    private void selectEntity(ResourceLocation id) {
        model.select(id);
        foodScroll = 0;
        ensureNativeFoods(id);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        TdmcUiTheme.fillPanel(graphics, 4, 4, width - 8, height - 8);
        TdmcUiTheme.fillHeader(graphics, 5, 5, width - 10, 28);
        graphics.drawString(font, title, 12, 14, TdmcUiTheme.TEXT_PRIMARY, false);

        renderEntityList(graphics, mouseX, mouseY);
        renderRightPanel(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderManualPlaceholders(graphics);
    }

    private void renderManualPlaceholders(GuiGraphics graphics) {
        if (searchBox != null && searchBox.getValue().isEmpty() && !searchBox.isFocused()) {
            graphics.drawString(font,
                    Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.search"),
                    searchBox.getX() + 4,
                    searchBox.getY() + (searchBox.getHeight() - 8) / 2,
                    TdmcUiTheme.TEXT_MUTED,
                    false);
        }
        for (AmountBoxBinding binding : amountBoxes) {
            if (binding.box.getValue().isEmpty() && !binding.box.isFocused()) {
                graphics.drawString(font,
                        Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.unconfigured"),
                        binding.box.getX() + 4,
                        binding.box.getY() + (binding.box.getHeight() - 8) / 2,
                        TdmcUiTheme.TEXT_MUTED,
                        false);
            }
        }
    }

    private void renderEntityList(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = 8;
        int right = LEFT_WIDTH - 8;
        int bottom = height - 10;
        TdmcUiTheme.fillSection(graphics, left, LIST_TOP, right - left, bottom - LIST_TOP);
        graphics.enableScissor(left + 1, LIST_TOP + 1, right - 9, bottom - 1);
        int y = LIST_TOP + 4 - entityScroll;
        for (ResourceLocation id : visibleEntities) {
            if (y + ENTITY_ROW_HEIGHT >= LIST_TOP && y <= bottom) {
                boolean selected = id.equals(model.selected());
                boolean hovered = mouseX >= left + 4 && mouseX < right - 10 && mouseY >= y && mouseY < y + ENTITY_ROW_HEIGHT - 2;
                int background = selected ? TdmcUiTheme.ROW_SELECTED : hovered ? TdmcUiTheme.ROW_HOVERED
                        : model.hasRule(id) ? TdmcUiTheme.ROW_MODIFIED : TdmcUiTheme.ROW_BACKGROUND;
                graphics.fill(left + 4, y, right - 10, y + ENTITY_ROW_HEIGHT - 2, background);
                graphics.drawString(font, entityDisplayName(id), left + 9, y + 5,
                        model.affected(id) ? TdmcUiTheme.TEXT_PRIMARY : TdmcUiTheme.TEXT_MUTED, false);
                graphics.drawString(font, id.toString(), left + 9, y + 16, TdmcUiTheme.TEXT_MUTED, false);
            }
            y += ENTITY_ROW_HEIGHT;
        }
        graphics.disableScissor();
        renderScrollbar(graphics, right - SCROLLBAR_WIDTH - 1, LIST_TOP + 2, bottom - LIST_TOP - 4,
                entityScroll, maxEntityScroll);
    }

    private void renderRightPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = LEFT_WIDTH + 6;
        int top = 36;
        int right = width - 8;
        int bottom = height - 10;
        TdmcUiTheme.fillSection(graphics, left, top, right - left, bottom - top);
        ResourceLocation selected = model.selected();
        if (selected == null) {
            graphics.drawCenteredString(font,
                    Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.no_selection"),
                    (left + right) / 2, top + 28, TdmcUiTheme.TEXT_MUTED);
            return;
        }
        graphics.drawString(font, entityDisplayName(selected), left + 8, top + 8, TdmcUiTheme.TEXT_PRIMARY, false);
        graphics.drawString(font, selected.toString(), left + 8, top + 20, TdmcUiTheme.TEXT_MUTED, false);
        if (!model.affected(selected)) {
            graphics.drawString(font,
                    Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.filtered"),
                    left + 8, top + 34, 0xFFFF7070, false);
        }

        TamingEditorRule rule = model.rule(selected);
        if (rule == null) {
            int y = 118;
            if (model.nativePending(selected)) {
                graphics.drawString(font,
                        Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.detecting_native"),
                        left + 8, y, TdmcUiTheme.TEXT_MUTED, false);
            } else {
                TamingEditorService.NativeFoodsResult nativeResult = model.nativeFoods(selected);
                if (nativeResult != null && nativeResult.success()) {
                    graphics.drawString(font,
                            Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.native_count", nativeResult.items().size()),
                            left + 8, y, TdmcUiTheme.TEXT_PRIMARY, false);
                } else if (nativeResult != null) {
                    graphics.drawString(font,
                            Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.native_detect_failed"),
                            left + 8, y, 0xFFFF7070, false);
                }
            }
            return;
        }

        graphics.drawString(font,
                Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.method"),
                left + 8, 76, TdmcUiTheme.TEXT_MUTED, false);
        graphics.drawString(font,
                Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.level"),
                left + 168, 76, TdmcUiTheme.TEXT_MUTED, false);

        if (model.nativePending(selected)) {
            graphics.drawString(font,
                    Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.detecting_native"),
                    left + 8, 116, TdmcUiTheme.TEXT_MUTED, false);
            return;
        }
        TamingEditorService.NativeFoodsResult nativeResult = model.nativeFoods(selected);
        if (nativeResult != null && !nativeResult.success()) {
            graphics.drawString(font,
                    Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.native_detect_failed"),
                    left + 8, 116, 0xFFFF7070, false);
        }
        renderFoodRows(graphics, mouseX, mouseY);
        String validationKey = model.validationKey(selected);
        Component message = !statusMessage.getString().isEmpty() ? statusMessage
                : validationKey.isEmpty() ? Component.empty() : Component.translatable(validationKey);
        if (!message.getString().isEmpty()) {
            graphics.drawString(font, message, left + 8, bottom - 13,
                    validationKey.isEmpty() ? TdmcUiTheme.TEXT_ACCENT : 0xFFFF7070, false);
        }
    }

    private void renderFoodRows(GuiGraphics graphics, int mouseX, int mouseY) {
        ResourceLocation selected = model.selected();
        TamingEditorRule rule = model.rule(selected);
        if (rule == null) {
            return;
        }
        int left = LEFT_WIDTH + 14;
        int right = width - 22;
        int top = foodListTop();
        int bottom = height - 28;
        graphics.enableScissor(left, top, right - SCROLLBAR_WIDTH - 2, bottom);
        int y = top - foodScroll;
        for (FoodRow row : foodRows) {
            int h = row.height();
            if (y + h >= top && y <= bottom) {
                if (row.kind == FoodRowKind.HEADER) {
                    graphics.fill(left, y, right - SCROLLBAR_WIDTH - 4, y + h - 2, TdmcUiTheme.SECTION_BACKGROUND);
                    graphics.drawString(font, Component.translatable(row.headerKey), left + 5, y + 7, TdmcUiTheme.TEXT_ACCENT, false);
                } else if (row.kind == FoodRowKind.ADD_EXTRA) {
                    renderActionBox(graphics, left + 5, y + 3, 132, 20,
                            Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.add_food"),
                            mouseX, mouseY);
                } else {
                    renderFoodRow(graphics, row, left, right - SCROLLBAR_WIDTH - 4, y, h, mouseX, mouseY, rule);
                }
            }
            y += h;
        }
        graphics.disableScissor();
        renderScrollbar(graphics, right - SCROLLBAR_WIDTH, top, bottom - top, foodScroll, maxFoodScroll);
    }

    private void renderFoodRow(
            GuiGraphics graphics,
            FoodRow row,
            int left,
            int right,
            int y,
            int h,
            int mouseX,
            int mouseY,
            TamingEditorRule rule
    ) {
        boolean hovered = mouseX >= left && mouseX < right && mouseY >= y && mouseY < y + h - 2;
        graphics.fill(left, y, right, y + h - 2, hovered ? TdmcUiTheme.ROW_HOVERED : TdmcUiTheme.ROW_BACKGROUND);
        Item item = ForgeRegistries.ITEMS.getValue(row.itemId);
        if (item != null) {
            ItemStack stack = new ItemStack(item);
            graphics.renderItem(stack, left + 6, y + 5);
            graphics.drawString(font, stack.getHoverName(), left + 28, y + 5, TdmcUiTheme.TEXT_PRIMARY, false);
        } else {
            graphics.drawString(font, row.itemId.toString(), left + 28, y + 5, TdmcUiTheme.TEXT_PRIMARY, false);
        }
        graphics.drawString(font, row.itemId.toString(), left + 28, y + 18, TdmcUiTheme.TEXT_MUTED, false);

        if (row.kind == FoodRowKind.NATIVE || row.kind == FoodRowKind.EXTRA) {
            int amount = row.kind == FoodRowKind.NATIVE
                    ? rule.nativeFoods().getOrDefault(row.itemId, 0)
                    : rule.extraFoods().getOrDefault(row.itemId, 0);
            int actionX = right - 58;
            int minusX = right - 188;
            int plusX = right - 86;
            renderActionBox(graphics, minusX, y + 12, 20, 20, Component.literal("-"), mouseX, mouseY);
            renderActionBox(graphics, plusX, y + 12, 20, 20, Component.literal("+"), mouseX, mouseY);
            renderActionBox(graphics, actionX, y + 12, 54, 20,
                    Component.translatable(row.kind == FoodRowKind.NATIVE
                            ? "gui.tl_domesticate_more_creatures.taming_editor.exclude"
                            : "gui.tl_domesticate_more_creatures.taming_editor.remove"), mouseX, mouseY);
            if (amount < 1) {
                graphics.drawString(font,
                        Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.unconfigured"),
                        right - 252, y + 4, 0xFFFFC060, false);
            }
        } else if (row.kind == FoodRowKind.REMOVED) {
            renderActionBox(graphics, right - 76, y + 12, 72, 20,
                    Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.restore"), mouseX, mouseY);
        }
    }

    private void renderActionBox(GuiGraphics graphics, int x, int y, int w, int h, Component text, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        TdmcUiTheme.fillControl(graphics, x, y, w, h, true, hovered, false);
        graphics.drawCenteredString(font, text, x + w / 2, y + 6, TdmcUiTheme.TEXT_PRIMARY);
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y, int height, int scroll, int maxScroll) {
        graphics.fill(x, y, x + SCROLLBAR_WIDTH, y + height, TdmcUiTheme.SLIDER_TRACK);
        if (maxScroll <= 0) {
            return;
        }
        int thumbHeight = Math.max(18, height * height / (height + maxScroll));
        int travel = Math.max(1, height - thumbHeight);
        int thumbY = y + Math.round(travel * (scroll / (float) maxScroll));
        graphics.fill(x + 1, thumbY, x + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, TdmcUiTheme.SLIDER_THUMB);
    }

    private String entityDisplayName(ResourceLocation id) {
        return entityDisplayComponent(id).getString();
    }

    private Component entityDisplayComponent(ResourceLocation id) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (type != null) {
            String translationKey = type.getDescriptionId();
            if (I18n.exists(translationKey)) {
                return type.getDescription();
            }
        }
        Component compatibilityName = compatibilityEntityName(id);
        return compatibilityName != null
                ? compatibilityName
                : Component.literal(fallbackRegistryName(id));
    }

    private static Component compatibilityEntityName(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        String namespace = id.getNamespace();
        String path = id.getPath();
        String[] candidateKeys = {
                "gui." + namespace + "." + path + ".name",
                "entity." + namespace + "." + path + ".temperate",
                "entity." + namespace + "." + path + ".default",
                "entity." + namespace + "." + path + ".normal"
        };
        for (String key : candidateKeys) {
            if (I18n.exists(key)) {
                return Component.translatable(key);
            }
        }
        return null;
    }

    private static String fallbackRegistryName(ResourceLocation id) {
        if (id == null) {
            return "";
        }
        String path = id.getPath().replace('_', ' ').replace('-', ' ').trim();
        if (path.isEmpty()) {
            return id.toString();
        }
        StringBuilder result = new StringBuilder(path.length());
        boolean capitalize = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (Character.isWhitespace(c)) {
                if (result.length() > 0 && result.charAt(result.length() - 1) != ' ') {
                    result.append(' ');
                }
                capitalize = true;
            } else {
                result.append(capitalize ? Character.toUpperCase(c) : c);
                capitalize = false;
            }
        }
        return result.toString();
    }

    private int foodListTop() {
        return 116;
    }

    private int totalFoodHeight() {
        int total = 0;
        for (FoodRow row : foodRows) {
            total += row.height();
        }
        return total;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < LEFT_WIDTH) {
            entityScroll = Mth.clamp(entityScroll - (int) Math.signum(delta) * ENTITY_ROW_HEIGHT, 0, maxEntityScroll);
            return true;
        }
        if (mouseY >= foodListTop()) {
            foodScroll = Mth.clamp(foodScroll - (int) Math.signum(delta) * 30, 0, maxFoodScroll);
            rebuildEditorWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int entityBarX = LEFT_WIDTH - 8 - SCROLLBAR_WIDTH - 1;
            if (mouseX >= entityBarX && mouseX < entityBarX + SCROLLBAR_WIDTH && mouseY >= LIST_TOP && mouseY < height - 10) {
                if (maxEntityScroll > 0) {
                    draggingEntityScrollbar = true;
                    entityScroll = scrollFromMouse(mouseY, LIST_TOP + 2, height - LIST_TOP - 14, maxEntityScroll);
                    return true;
                }
            }
            int foodBarX = width - 22 - SCROLLBAR_WIDTH;
            if (mouseX >= foodBarX && mouseX < foodBarX + SCROLLBAR_WIDTH && mouseY >= foodListTop() && mouseY < height - 28) {
                if (maxFoodScroll > 0) {
                    draggingFoodScrollbar = true;
                    foodScroll = scrollFromMouse(mouseY, foodListTop(), height - 28 - foodListTop(), maxFoodScroll);
                    rebuildEditorWidgets();
                    return true;
                }
            }
            if (mouseX >= 12 && mouseX < LEFT_WIDTH - 18 && mouseY >= LIST_TOP && mouseY < height - 10) {
                int index = (int) ((mouseY - LIST_TOP - 4 + entityScroll) / ENTITY_ROW_HEIGHT);
                if (index >= 0 && index < visibleEntities.size()) {
                    selectEntity(visibleEntities.get(index));
                    rebuildEditorWidgets();
                    return true;
                }
            }
            if (handleFoodClick(mouseX, mouseY)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleFoodClick(double mouseX, double mouseY) {
        ResourceLocation selected = model.selected();
        TamingEditorRule rule = model.rule(selected);
        if (selected == null || rule == null || mouseY < foodListTop() || mouseY >= height - 28) {
            return false;
        }
        int left = LEFT_WIDTH + 14;
        int right = width - 22 - SCROLLBAR_WIDTH - 4;
        if (mouseX < left || mouseX >= right) {
            return false;
        }
        int y = foodListTop() - foodScroll;
        for (FoodRow row : foodRows) {
            int h = row.height();
            if (mouseY >= y && mouseY < y + h) {
                if (row.kind == FoodRowKind.ADD_EXTRA) {
                    if (minecraft != null) {
                        minecraft.setScreen(new TamingItemPickerScreen(this, selected, blockedPickerItems()));
                    }
                    return true;
                }
                if (row.kind == FoodRowKind.NATIVE || row.kind == FoodRowKind.EXTRA) {
                    int minusX = right - 188;
                    int plusX = right - 86;
                    int actionX = right - 58;
                    int current = row.kind == FoodRowKind.NATIVE
                            ? rule.nativeFoods().getOrDefault(row.itemId, 0)
                            : rule.extraFoods().getOrDefault(row.itemId, 0);
                    if (mouseX >= minusX && mouseX < minusX + 20 && mouseY >= y + 12 && mouseY < y + 32) {
                        setRowAmount(row, Math.max(0, current - 1));
                        rebuildRightWidgetsOnly();
                        return true;
                    }
                    if (mouseX >= plusX && mouseX < plusX + 20 && mouseY >= y + 12 && mouseY < y + 32) {
                        setRowAmount(row, Math.max(1, current + 1));
                        rebuildRightWidgetsOnly();
                        return true;
                    }
                    if (mouseX >= actionX && mouseX < actionX + 54 && mouseY >= y + 12 && mouseY < y + 32) {
                        if (row.kind == FoodRowKind.NATIVE) {
                            model.excludeNative(selected, row.itemId);
                        } else {
                            model.removeExtra(selected, row.itemId);
                        }
                        rebuildRightWidgetsOnly();
                        return true;
                    }
                } else if (row.kind == FoodRowKind.REMOVED) {
                    int actionX = right - 76;
                    if (mouseX >= actionX && mouseX < actionX + 72 && mouseY >= y + 12 && mouseY < y + 32) {
                        model.restoreNative(selected, row.itemId);
                        rebuildRightWidgetsOnly();
                        return true;
                    }
                }
                return false;
            }
            y += h;
        }
        return false;
    }

    private void setRowAmount(FoodRow row, int amount) {
        ResourceLocation selected = model.selected();
        if (selected == null) {
            return;
        }
        if (row.kind == FoodRowKind.NATIVE) {
            model.setNativeAmount(selected, row.itemId, amount);
        } else if (row.kind == FoodRowKind.EXTRA) {
            model.setExtraAmount(selected, row.itemId, amount);
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingEntityScrollbar) {
            entityScroll = scrollFromMouse(mouseY, LIST_TOP + 2, height - LIST_TOP - 14, maxEntityScroll);
            return true;
        }
        if (button == 0 && draggingFoodScrollbar) {
            foodScroll = scrollFromMouse(mouseY, foodListTop(), height - 28 - foodListTop(), maxFoodScroll);
            rebuildEditorWidgets();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingEntityScrollbar = false;
            draggingFoodScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int scrollFromMouse(double mouseY, int trackY, int trackHeight, int maxScroll) {
        if (maxScroll <= 0 || trackHeight <= 1) {
            return 0;
        }
        double ratio = Mth.clamp((mouseY - trackY) / Math.max(1.0D, trackHeight), 0.0D, 1.0D);
        return Mth.clamp((int) Math.round(ratio * maxScroll), 0, maxScroll);
    }

    private enum ListFilter {
        ALL("gui.tl_domesticate_more_creatures.taming_editor.filter.all"),
        CONFIGURED("gui.tl_domesticate_more_creatures.taming_editor.filter.configured");

        private final String key;

        ListFilter(String key) {
            this.key = key;
        }
    }

    private enum FoodRowKind {
        HEADER,
        NATIVE,
        EXTRA,
        ADD_EXTRA,
        REMOVED
    }

    private record FoodRow(FoodRowKind kind, ResourceLocation itemId, String headerKey) {
        static FoodRow header(String key) {
            return new FoodRow(FoodRowKind.HEADER, null, key);
        }

        int height() {
            return kind == FoodRowKind.HEADER ? HEADER_ROW_HEIGHT
                    : kind == FoodRowKind.ADD_EXTRA ? 28 : FOOD_ROW_HEIGHT;
        }
    }

    private record AmountBoxBinding(TdmcEditBox box, FoodRowKind kind, ResourceLocation itemId) {
    }
}
