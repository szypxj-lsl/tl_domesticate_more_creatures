package com.szypxj.tldomesticatemorecreatures.client;

import com.szypxj.tldomesticatemorecreatures.api.client.PanelSummaryProvider;
import com.szypxj.tldomesticatemorecreatures.api.client.PanelSummaryRegistry;
import com.szypxj.tldomesticatemorecreatures.client.talent.ActiveTalentTooltip;
import com.szypxj.tldomesticatemorecreatures.api.client.PanelAction;
import com.szypxj.tldomesticatemorecreatures.api.client.PanelActionRegistry;
import com.szypxj.tldomesticatemorecreatures.api.compat.ErsCompatApi;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcButton;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcUiTheme;
import com.szypxj.tldomesticatemorecreatures.config.ClientConfig;
import com.szypxj.tldomesticatemorecreatures.client.petmanagement.PetManagementScreen;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.network.PanelSnapshot;
import com.szypxj.tldomesticatemorecreatures.network.StatSnapshot;
import com.szypxj.tldomesticatemorecreatures.network.TalentSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.szypxj.tldomesticatemorecreatures.menu.AttributePanelMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AttributePanelScreen extends AbstractContainerScreen<AttributePanelMenu> {
    private static final int PANEL_WIDTH = AttributePanelMenu.IMAGE_WIDTH;
    private static final int PANEL_HEIGHT = AttributePanelMenu.IMAGE_HEIGHT;
    private static final int STORAGE_SLOT_FRAME_SIZE = 22;
    private static final int ROW_HEIGHT = 25;
    private static final int MIDDLE_X_OFFSET = 188;
    private static final int MIDDLE_WIDTH = 180;
    private static final int RIGHT_X_OFFSET = 376;
    private static final int RIGHT_WIDTH = 136;
    private static final int PREVIEW_RIGHT_MARGIN = 8;
    private static final int LIST_Y_OFFSET = 114;
    private static final int TALENT_ROW_STEP = 19;
    private static final int TALENT_MAX_SLOT_WIDTH = 68;
    private static final int FURY_ROW_HEIGHT = 20;
    private static final int PANEL_REFRESH_INTERVAL_TICKS = 5;
    private static final int EXPERIENCE_BAR_WIDTH = 150;
    private static final int EXPERIENCE_BAR_HEIGHT = 9;
    private static final int EFFECT_PANEL_WIDTH = 63;
    private static final int EFFECT_PANEL_HEADER_HEIGHT = 14;
    private static final int EFFECT_PANEL_ROW_HEIGHT = 24;
    private static final int EFFECT_PANEL_ICON_SIZE = 18;
    private static final int EFFECT_PANEL_MARGIN = 4;
    private PanelSnapshot snapshot;
    private final List<StatButton> statButtons = new ArrayList<>();
    private final List<TdmcButton> panelActionButtons = new ArrayList<>();
    private int scroll;
    private int left;
    private int top;
    private int refreshTicks;
    private TdmcButton mountedTargetSwitchButton;
    private TdmcButton craftingButton;
    private List<Component> pendingTooltip;
    private int effectPanelX;
    private int effectPanelY;
    private boolean effectPanelPositionInitialized;
    private boolean effectPanelDragging;
    private int effectPanelDragOffsetX;
    private int effectPanelDragOffsetY;
    private AttributePanelResponsiveLayout.Layout responsiveLayout =
            new AttributePanelResponsiveLayout.Layout(1.0D, AttributePanelResponsiveLayout.Mode.NORMAL);

    public AttributePanelScreen(AttributePanelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.snapshot = menu.initialSnapshot();
        if (!this.snapshot.player()) {
            ClientState.setPanelCompanionEntityId(this.snapshot.entityId());
        }
        this.imageWidth = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    public int entityId() {
        return snapshot.entityId();
    }

    public void updateSnapshot(PanelSnapshot newSnapshot) {
        boolean layoutChanged = statLayoutChanged(this.snapshot, newSnapshot);
        this.snapshot = newSnapshot;
        if (minecraft == null) {
            return;
        }
        if (layoutChanged) {
            clearWidgets();
            init();
            return;
        }
        for (StatButton statButton : statButtons) {
            int index = statButton.index();
            if (index >= 0 && index < snapshot.stats().size()) {
                statButton.button().active = snapshot.stats().get(index).canAllocate();
            }
        }
        updateMountedTargetSwitchButton();
    }

    private static boolean statLayoutChanged(PanelSnapshot oldSnapshot, PanelSnapshot newSnapshot) {
        if (oldSnapshot.fury().available() != newSnapshot.fury().available()
                || talentRowCount(oldSnapshot.talents()) != talentRowCount(newSnapshot.talents())
                || oldSnapshot.stats().size() != newSnapshot.stats().size()) {
            return true;
        }
        for (int i = 0; i < oldSnapshot.stats().size(); i++) {
            if (!oldSnapshot.stats().get(i).id().equals(newSnapshot.stats().get(i).id())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void containerTick() {
        updateMountedTargetSwitchButton();
        refreshTicks++;
        if (refreshTicks >= PANEL_REFRESH_INTERVAL_TICKS) {
            refreshTicks = 0;
            NetworkHandler.refreshOpenPanel(snapshot.entityId());
        }
    }

    @Override
    protected void init() {
        responsiveLayout = AttributePanelResponsiveLayout.calculate(width, height, PANEL_WIDTH, PANEL_HEIGHT);
        super.init();
        left = leftPos;
        top = topPos;
        initializeEffectPanelPosition();
        statButtons.clear();
        panelActionButtons.clear();
        mountedTargetSwitchButton = null;
        craftingButton = addRenderableWidget(TdmcButton.create(
                left + 14, top + 34, 72, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.crafting"),
                pressed -> NetworkHandler.openCrafting()
        ));
        addRenderableWidget(TdmcButton.create(
                left + PANEL_WIDTH - 227, top + 5, 72, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.pet_management.open"),
                pressed -> {
                    if (minecraft != null) minecraft.setScreen(new PetManagementScreen(this));
                }
        ));
        addRenderableWidget(TdmcButton.create(
                left + PANEL_WIDTH - 151, top + 5, 52, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.inventory_switch.vanilla"),
                pressed -> openVanillaInventory()
        ));
        mountedTargetSwitchButton = addRenderableWidget(TdmcButton.create(
                left + PANEL_WIDTH - 95, top + 5, 52, 20,
                Component.empty(),
                pressed -> switchMountedPanelTarget()
        ));
        updateMountedTargetSwitchButton();
        addRenderableWidget(TdmcButton.create(
                left + PANEL_WIDTH - 39, top + 5, 34, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.spyglass_position.open"),
                pressed -> {
                    if (minecraft != null) {
                        minecraft.setScreen(new SpyglassPanelPositionScreen(this));
                    }
                }
        ));
        addPanelActionButtons();
        if (snapshot.owner() && hasNativePetInventory()) {
            addRenderableWidget(TdmcButton.create(
                    left + RIGHT_X_OFFSET + 12, top + 266, RIGHT_WIDTH - 24, 20,
                    Component.translatable("gui.tl_domesticate_more_creatures.native_backpack"),
                    pressed -> NetworkHandler.openNativePetInventory(snapshot.entityId())
            ));
        }
        for (int i = 0; i < snapshot.stats().size(); i++) {
            StatSnapshot stat = snapshot.stats().get(i);
            if ("torpor".equals(stat.id())) {
                continue;
            }
            TdmcButton button = TdmcButton.create(
                    left + MIDDLE_X_OFFSET + MIDDLE_WIDTH - 24, statListY() + i * ROW_HEIGHT - scroll, 20, 20,
                    Component.translatable("gui.tl_domesticate_more_creatures.add"),
                    pressed -> NetworkHandler.allocateStat(snapshot.entityId(), stat.id())
            );
            button.active = stat.canAllocate();
            addRenderableWidget(button);
            statButtons.add(new StatButton(i, button));
        }
        layoutButtons();
    }

    private void addPanelActionButtons() {
        List<PanelAction> actions = PanelActionRegistry.actions();
        if (actions.isEmpty()) {
            return;
        }
        int x = left + 92;
        int y = top + 5;
        int maxX = left + PANEL_WIDTH - 231;
        for (PanelAction action : actions) {
            int buttonWidth = action.width();
            if (x + buttonWidth > maxX) {
                x = left + 14;
                y += 24;
            }
            TdmcButton button = TdmcButton.create(
                    x, y, buttonWidth, 20,
                    Component.translatable(action.nameKey()),
                    pressed -> action.action().run()
            );
            addRenderableWidget(button);
            panelActionButtons.add(button);
            x += buttonWidth + 4;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        pendingTooltip = null;
        int layoutMouseX = (int) Math.round(toLayoutX(mouseX));
        int layoutMouseY = (int) Math.round(toLayoutY(mouseY));
        graphics.pose().pushPose();
        applyResponsiveTransform(graphics);
        super.render(graphics, layoutMouseX, layoutMouseY, partialTick);
        renderEffectPanel(graphics);
        if (pendingTooltip != null && !pendingTooltip.isEmpty()) {
            graphics.renderComponentTooltip(font, pendingTooltip, layoutMouseX, layoutMouseY);
        }
        renderTooltip(graphics, layoutMouseX, layoutMouseY);
        graphics.pose().popPose();
    }


    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            InventoryPanelKeyHandler.suppressInventoryKeyUntilRelease();
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double layoutMouseX = toLayoutX(mouseX);
        double layoutMouseY = toLayoutY(mouseY);
        if (button == 0 && hasActivePlayerEffects() && isInsideEffectPanelHeader(layoutMouseX, layoutMouseY)) {
            effectPanelDragging = true;
            effectPanelDragOffsetX = (int) Math.round(layoutMouseX) - effectPanelX;
            effectPanelDragOffsetY = (int) Math.round(layoutMouseY) - effectPanelY;
            return true;
        }
        return super.mouseClicked(layoutMouseX, layoutMouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && effectPanelDragging) {
            effectPanelDragging = false;
            ClientConfig.setEffectPanelPosition(effectPanelX, effectPanelY);
            return true;
        }
        return super.mouseReleased(toLayoutX(mouseX), toLayoutY(mouseY), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && effectPanelDragging) {
            int layoutMouseX = (int) Math.round(toLayoutX(mouseX));
            int layoutMouseY = (int) Math.round(toLayoutY(mouseY));
            effectPanelX = clampEffectPanelX(layoutMouseX - effectPanelDragOffsetX);
            effectPanelY = clampEffectPanelY(layoutMouseY - effectPanelDragOffsetY);
            return true;
        }
        double scale = responsiveLayout.scale();
        return super.mouseDragged(
                toLayoutX(mouseX),
                toLayoutY(mouseY),
                button,
                dragX / scale,
                dragY / scale
        );
    }

    private void initializeEffectPanelPosition() {
        if (effectPanelPositionInitialized) {
            effectPanelX = clampEffectPanelX(effectPanelX);
            effectPanelY = clampEffectPanelY(effectPanelY);
            return;
        }
        if (ClientConfig.EFFECT_PANEL_CUSTOM_POSITION.get()) {
            effectPanelX = clampEffectPanelX(ClientConfig.EFFECT_PANEL_X.get());
            effectPanelY = clampEffectPanelY(ClientConfig.EFFECT_PANEL_Y.get());
        } else {
            effectPanelX = clampEffectPanelX(left - EFFECT_PANEL_WIDTH - EFFECT_PANEL_MARGIN);
            effectPanelY = clampEffectPanelY(top + 31);
        }
        effectPanelPositionInitialized = true;
    }

    private boolean hasActivePlayerEffects() {
        return minecraft != null && minecraft.player != null && !minecraft.player.getActiveEffects().isEmpty();
    }

    private boolean isInsideEffectPanelHeader(double mouseX, double mouseY) {
        return mouseX >= effectPanelX
                && mouseX < effectPanelX + EFFECT_PANEL_WIDTH
                && mouseY >= effectPanelY
                && mouseY < effectPanelY + EFFECT_PANEL_HEADER_HEIGHT;
    }

    private int clampEffectPanelX(int x) {
        return Mth.clamp(x, 0, Math.max(0, width - EFFECT_PANEL_WIDTH));
    }

    private int clampEffectPanelY(int y) {
        return Mth.clamp(y, 0, Math.max(0, height - EFFECT_PANEL_HEADER_HEIGHT));
    }

    private void renderEffectPanel(GuiGraphics graphics) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        List<MobEffectInstance> effects = new ArrayList<>(minecraft.player.getActiveEffects());
        if (effects.isEmpty()) {
            effectPanelDragging = false;
            return;
        }
        initializeEffectPanelPosition();
        int panelHeight = EFFECT_PANEL_HEADER_HEIGHT + effects.size() * EFFECT_PANEL_ROW_HEIGHT + 2;
        TdmcUiTheme.fillPanel(graphics, effectPanelX, effectPanelY, EFFECT_PANEL_WIDTH, panelHeight);
        TdmcUiTheme.fillHeader(graphics, effectPanelX + 1, effectPanelY + 1, EFFECT_PANEL_WIDTH - 2, EFFECT_PANEL_HEADER_HEIGHT - 1);
        graphics.drawCenteredString(
                font,
                Component.translatable("gui.tl_domesticate_more_creatures.effect_panel.title"),
                effectPanelX + EFFECT_PANEL_WIDTH / 2,
                effectPanelY + 3,
                TdmcUiTheme.TEXT_PRIMARY
        );

        int rowY = effectPanelY + EFFECT_PANEL_HEADER_HEIGHT;
        for (MobEffectInstance effect : effects) {
            renderEffectPanelRow(graphics, effect, rowY);
            rowY += EFFECT_PANEL_ROW_HEIGHT;
        }
    }

    private void renderEffectPanelRow(GuiGraphics graphics, MobEffectInstance effect, int rowY) {
        graphics.fill(
                effectPanelX + 2,
                rowY + 1,
                effectPanelX + EFFECT_PANEL_WIDTH - 2,
                rowY + EFFECT_PANEL_ROW_HEIGHT - 1,
                TdmcUiTheme.ROW_BACKGROUND
        );
        TextureAtlasSprite sprite = minecraft.getMobEffectTextures().get(effect.getEffect());
        graphics.blit(
                effectPanelX + 3,
                rowY + 3,
                0,
                EFFECT_PANEL_ICON_SIZE,
                EFFECT_PANEL_ICON_SIZE,
                sprite
        );

        int textX = effectPanelX + 23;
        int textWidth = EFFECT_PANEL_WIDTH - 25;
        String levelText = effectLevelText(effect.getAmplifier() + 1);
        String name = effect.getEffect().getDisplayName().getString();
        String firstLine = trimEffectText(name + " " + levelText, textWidth);
        String duration = MobEffectUtil.formatDuration(effect, 1.0F).getString();
        String secondLine = trimEffectText(duration, textWidth);
        graphics.drawString(font, firstLine, textX, rowY + 3, TdmcUiTheme.TEXT_PRIMARY, true);
        graphics.drawString(font, secondLine, textX, rowY + 13, TdmcUiTheme.TEXT_MUTED, true);
    }

    private String trimEffectText(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int suffixWidth = font.width(suffix);
        if (suffixWidth >= maxWidth) {
            return suffix;
        }
        String value = text;
        while (!value.isEmpty() && font.width(value) + suffixWidth > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value + suffix;
    }

    private static String effectLevelText(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> Integer.toString(level);
        };
    }

    private void applyResponsiveTransform(GuiGraphics graphics) {
        float centerX = width / 2.0F;
        float centerY = height / 2.0F;
        float scale = (float) responsiveLayout.scale();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-centerX, -centerY, 0.0F);
    }

    private double toLayoutX(double screenX) {
        return AttributePanelResponsiveLayout.screenToLayout(screenX, width / 2.0D, responsiveLayout.scale());
    }

    private double toLayoutY(double screenY) {
        return AttributePanelResponsiveLayout.screenToLayout(screenY, height / 2.0D, responsiveLayout.scale());
    }

    private void enableResponsiveScissor(GuiGraphics graphics, int left, int top, int right, int bottom) {
        AttributePanelResponsiveLayout.ScissorRect rect = AttributePanelResponsiveLayout.layoutScissorToScreen(
                left,
                top,
                right,
                bottom,
                width,
                height,
                responsiveLayout.scale()
        );
        graphics.enableScissor(rect.left(), rect.top(), rect.right(), rect.bottom());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        left = leftPos;
        top = topPos;

        TdmcUiTheme.fillPanel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.fill(left + 1, top + 1, left + PANEL_WIDTH - 1, top + 30, TdmcUiTheme.SECTION_BACKGROUND);
        graphics.drawCenteredString(font, snapshot.name(), left + PANEL_WIDTH / 2, top + 10, 0xFFFFFF);
        if (snapshot.elite()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.tl_domesticate_more_creatures.elite"),
                    left + PANEL_WIDTH / 2,
                    top + 22,
                    0xFFFFFF
            );
        }

        renderInventoryArea(graphics, mouseX, mouseY);
        List<Component> tooltip = pendingTooltip;
        renderSummary(graphics);
        renderRightSummary(graphics);

        List<Component> talentTooltip = renderTalents(graphics, mouseX, mouseY);
        if (talentTooltip != null) {
            tooltip = talentTooltip;
        }
        renderFury(graphics);
        if (snapshot.imprint().available()) {
            renderImprint(graphics);
        }
        renderEntity(graphics, mouseX, mouseY);
        renderPetBackpack(graphics);
        List<Component> statTooltip = renderStats(graphics, mouseX, mouseY);
        if (statTooltip != null) {
            tooltip = statTooltip;
        }
        renderExperience(graphics);
        pendingTooltip = tooltip;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    private void renderSummary(GuiGraphics graphics) {
        graphics.drawString(
                font,
                Component.translatable("gui.tl_domesticate_more_creatures.unspent", snapshot.unspentPoints()),
                left + MIDDLE_X_OFFSET, unspentY(), 0xFFFFFF, true
        );
    }

    private void renderRightSummary(GuiGraphics graphics) {
        int x = left + RIGHT_X_OFFSET;
        graphics.drawString(
                font,
                Component.translatable("gui.tl_domesticate_more_creatures.level", snapshot.level(), snapshot.maxLevel()),
                x, top + 38, 0xFFFFFF, true
        );
        if (!snapshot.player()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.tl_domesticate_more_creatures.initial_level", snapshot.initialLevel()),
                    x, top + 50, 0xFFFFFF, true
            );
        }
        renderPanelSummaryLines(graphics, x, rightSummaryStartY());
    }

    private void renderPanelSummaryLines(GuiGraphics graphics, int x, int startY) {
        List<Component> lines = panelSummaryLines();
        for (int i = 0; i < lines.size(); i++) {
            drawRightSummaryLine(graphics, lines.get(i), x, startY + i * 12);
        }
    }

    private void drawRightSummaryLine(GuiGraphics graphics, Component line, int x, int y) {
        int textWidth = font.width(line);
        int maxWidth = RIGHT_WIDTH;
        if (textWidth <= maxWidth || textWidth <= 0) {
            graphics.drawString(font, line, x, y, 0xFFFFFF, true);
            return;
        }
        float scale = maxWidth / (float) textWidth;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, line, 0, 0, 0xFFFFFF, true);
        graphics.pose().popPose();
    }

    private List<Component> panelSummaryLines() {
        return PanelSummaryRegistry.lines(new PanelSummaryProvider.Context(snapshot.entityId(), snapshot.player()));
    }

    private int rightSummaryStartY() {
        return snapshot.player() ? top + 52 : top + 64;
    }

    private void renderInventoryArea(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font,
                Component.translatable("gui.tl_domesticate_more_creatures.inventory"),
                left + AttributePanelMenu.INVENTORY_X, top + 61, 0xFFFFFF, true
        );
        int separatorX = left + AttributePanelMenu.HOTBAR_X - 9;
        int separatorTop = top + AttributePanelMenu.INVENTORY_Y - 3;
        int separatorBottom = top + AttributePanelMenu.INVENTORY_Y
                + (AttributePanelMenu.INVENTORY_ROWS - 1) * AttributePanelMenu.INVENTORY_ROW_SPACING + 19;
        graphics.fill(
                separatorX, separatorTop,
                separatorX + 1, separatorBottom,
                TdmcUiTheme.BORDER
        );
        List<Integer> equipmentIndices = menu.equipmentMenuSlotIndices();
        List<Integer> backpackIndices = menu.backpackMenuSlotIndices();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            boolean equipmentSlot = equipmentIndices.contains(i);
            boolean backpackSlot = backpackIndices.contains(i);
            int frameSize = equipmentSlot || backpackSlot ? 18 : STORAGE_SLOT_FRAME_SIZE;
            int frameInset = (frameSize - 16) / 2;
            int frameX = left + slot.x - frameInset;
            int frameY = top + slot.y - frameInset;
            graphics.fill(frameX, frameY, frameX + frameSize, frameY + frameSize, TdmcUiTheme.ROW_BACKGROUND);
            TdmcUiTheme.outline(graphics, frameX, frameY, frameSize, frameSize, TdmcUiTheme.BORDER);
        }
        renderEquipmentSlotTooltip(mouseX, mouseY);
    }

    private void renderEquipmentSlotTooltip(int mouseX, int mouseY) {
        List<Integer> equipmentIndices = menu.equipmentMenuSlotIndices();
        for (int i = 0; i < equipmentIndices.size(); i++) {
            Slot slot = menu.slots.get(equipmentIndices.get(i));
            int x = left + slot.x;
            int y = top + slot.y;
            if (mouseX < x || mouseX >= x + 16 || mouseY < y || mouseY >= y + 16 || !slot.getItem().isEmpty()) {
                continue;
            }
            String key;
            if (snapshot.player()) {
                key = switch (i) {
                    case 0 -> "gui.tl_domesticate_more_creatures.pet_equipment.slot.head";
                    case 1 -> "gui.tl_domesticate_more_creatures.pet_equipment.slot.chest";
                    case 2 -> "gui.tl_domesticate_more_creatures.pet_equipment.slot.legs";
                    case 3 -> "gui.tl_domesticate_more_creatures.pet_equipment.slot.feet";
                    default -> "gui.tl_domesticate_more_creatures.player_equipment.slot.offhand";
                };
            } else if (i < menu.petDefinitions().size()) {
                key = menu.petDefinitions().get(i).nameKey();
            } else {
                continue;
            }
            pendingTooltip = List.of(Component.translatable(key));
            return;
        }
    }

    private List<Component> renderTalents(GuiGraphics graphics, int mouseX, int mouseY) {
        if (snapshot.player() || snapshot.talents().isEmpty()) {
            return null;
        }
        List<TalentSnapshot> normal = snapshot.talents().stream().filter(talent -> !talent.special()).toList();
        List<TalentSnapshot> special = snapshot.talents().stream().filter(TalentSnapshot::special).toList();
        int y = rightTalentStartY();
        List<Component> tooltip = renderTalentRow(graphics, normal, y, mouseX, mouseY);
        if (!normal.isEmpty()) {
            y += TALENT_ROW_STEP;
        }
        List<Component> specialTooltip = renderTalentRow(graphics, special, y, mouseX, mouseY);
        return specialTooltip != null ? specialTooltip : tooltip;
    }

    private List<Component> renderTalentRow(
            GuiGraphics graphics,
            List<TalentSnapshot> talents,
            int y,
            int mouseX,
            int mouseY
    ) {
        if (talents.isEmpty()) {
            return null;
        }
        int areaX = left + RIGHT_X_OFFSET;
        int areaWidth = RIGHT_WIDTH;
        int slotHeight = 15;
        int gap = 4;
        int count = talents.size();
        int availableForSlots = Math.max(count, areaWidth - Math.max(0, count - 1) * gap);
        int slotWidth = Math.max(1, Math.min(TALENT_MAX_SLOT_WIDTH, availableForSlots / count));
        int totalWidth = count * slotWidth + (count - 1) * gap;
        int startX = areaX + Math.max(0, (areaWidth - totalWidth) / 2);

        List<Component> hoveredTooltip = null;
        for (int i = 0; i < count; i++) {
            TalentSnapshot talent = talents.get(i);
            int slotX = startX + i * (slotWidth + gap);
            int background = talent.special() ? 0xFFB32626 : talentBackground(talent.level());
            int textColor = talent.special() ? 0xFFFFFFFF : talentTextColor(talent.level());
            graphics.fill(slotX, y, slotX + slotWidth, y + slotHeight, background);

            Component name = talentName(talent);
            int textX = slotX + (slotWidth - font.width(name)) / 2;
            enableResponsiveScissor(graphics, slotX + 1, y + 1, slotX + slotWidth - 1, y + slotHeight - 1);
            graphics.drawString(font, name, textX, y + 3, textColor, false);
            graphics.disableScissor();

            if (mouseX >= slotX && mouseX < slotX + slotWidth && mouseY >= y && mouseY < y + slotHeight) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(name);
                for (TalentSnapshot.Effect effect : talent.effects()) {
                    tooltip.add(Component.translatable(
                            "gui.tl_domesticate_more_creatures.talent_effect",
                            Component.translatable(effect.statNameKey()),
                            effect.points()
                    ));
                }
                ActiveTalentTooltip.appendIfActive(tooltip, talent);
                hoveredTooltip = tooltip;
            }
        }
        return hoveredTooltip;
    }

    private void renderFury(GuiGraphics graphics) {
        if (!snapshot.fury().available()) {
            return;
        }
        int x = left + RIGHT_X_OFFSET + 4;
        int width = RIGHT_WIDTH - 8;
        int y = rightTalentStartY() + talentRowCount(snapshot.talents()) * TALENT_ROW_STEP;
        Component current = Component.literal(number(snapshot.fury().anger())).withStyle(ChatFormatting.WHITE);
        Component max = Component.literal(number(snapshot.fury().maxAnger())).withStyle(ChatFormatting.WHITE);
        Component label = Component.translatable(
                snapshot.fury().berserk()
                        ? "gui.tl_domesticate_more_creatures.fury_berserk"
                        : "gui.tl_domesticate_more_creatures.fury_anger",
                current,
                max
        ).withStyle(snapshot.fury().berserk() ? ChatFormatting.RED : ChatFormatting.WHITE);
        graphics.drawString(font, label, x, y, 0xFFFFFF, true);
        int barY = y + 10;
        graphics.fill(x, barY, x + width, barY + 6, TdmcUiTheme.BAR_BACKGROUND);
        double ratio = snapshot.fury().maxAnger() <= 0.0D
                ? 0.0D
                : Mth.clamp(snapshot.fury().anger() / snapshot.fury().maxAnger(), 0.0D, 1.0D);
        int fill = (int) Math.round((width - 2) * ratio);
        if (fill > 0) {
            graphics.fill(x + 1, barY + 1, x + 1 + fill, barY + 5, 0xFFD93636);
        }
    }

    private void renderImprint(GuiGraphics graphics) {
        var imprint = snapshot.imprint();
        Component percent = Component.literal(Integer.toString(imprint.percent()) + "%").withStyle(ChatFormatting.GREEN);
        int x = left + RIGHT_X_OFFSET;
        int y = rightImprintStartY();
        graphics.drawString(font, Component.translatable("gui.tl_domesticate_more_creatures.imprint_progress", percent), x, y, 0xFFFFFF, true);
        if (imprint.active()) {
            Component remaining = Component.literal(formatImprintTicks(imprint.remainingTicks())).withStyle(ChatFormatting.GREEN);
            graphics.drawString(font, Component.translatable("gui.tl_domesticate_more_creatures.imprint_remaining", remaining), x, y + 12, 0xFFFFFF, true);
            Component need;
            if (imprint.needType() == null || imprint.needType().isBlank()) {
                Component next = Component.literal(formatImprintTicks(imprint.nextNeedTicks())).withStyle(ChatFormatting.GREEN);
                need = Component.translatable("gui.tl_domesticate_more_creatures.imprint_next_need", next);
            } else if ("FEED".equals(imprint.needType())) {
                Component food = imprint.foodNameKey() == null || imprint.foodNameKey().isBlank()
                        ? Component.translatable("gui.tl_domesticate_more_creatures.imprint_food_unknown")
                        : Component.translatable(imprint.foodNameKey()).withStyle(ChatFormatting.GOLD);
                need = Component.translatable("gui.tl_domesticate_more_creatures.imprint_need_feed", food);
            } else if ("WALK".equals(imprint.needType())) {
                need = Component.translatable("gui.tl_domesticate_more_creatures.imprint_need_walk");
            } else {
                need = Component.translatable("gui.tl_domesticate_more_creatures.imprint_need_pet");
            }
            graphics.drawString(font, need, x, y + 24, 0xFFFFFF, true);
        } else {
            Component bonus = Component.literal("+" + imprint.levelCapBonus()).withStyle(ChatFormatting.GREEN);
            graphics.drawString(font, Component.translatable("gui.tl_domesticate_more_creatures.imprint_level_cap", bonus), x, y + 12, 0xFFFFFF, true);
            graphics.drawString(font, Component.translatable(imprint.bonded()
                    ? "gui.tl_domesticate_more_creatures.imprint_bond_yes"
                    : "gui.tl_domesticate_more_creatures.imprint_bond_no"), x, y + 24, 0xFFFFFF, true);
        }
    }

    private static String formatImprintTicks(long ticks) {
        long seconds = Math.max(0L, ticks) / 20L;
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60L, seconds % 60L);
    }

    private void renderEntity(GuiGraphics graphics, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(snapshot.entityId());
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        int x0 = left + RIGHT_X_OFFSET;
        int y0 = rightPreviewTopY();
        int x1 = left + PANEL_WIDTH - PREVIEW_RIGHT_MARGIN;
        int y1 = snapshot.backpackAvailable()
                ? top + AttributePanelMenu.BACKPACK_Y - 16
                : hasNativePetInventory() ? top + 254 : top + 232;
        float size = Math.max(living.getBbWidth(), living.getBbHeight());
        int scale = Math.max(18, Math.min(55, (int) (54.0F / Math.max(1.0F, size))));
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics,
                (x0 + x1) / 2,
                y1 - 8,
                scale,
                (float) ((x0 + x1) / 2 - mouseX),
                (float) ((y0 + y1) / 2 - mouseY),
                living
        );
    }

    private boolean hasNativePetInventory() {
        if (minecraft == null || minecraft.level == null || snapshot.player() || !snapshot.owner()) {
            return false;
        }
        Entity entity = minecraft.level.getEntity(snapshot.entityId());
        return entity instanceof LivingEntity living && ErsCompatApi.hasNativeInventory(living);
    }

    private void renderPetBackpack(GuiGraphics graphics) {
        if (!snapshot.backpackAvailable()) {
            return;
        }
        graphics.drawCenteredString(
                font,
                Component.translatable("gui.tl_domesticate_more_creatures.pet_backpack"),
                left + RIGHT_X_OFFSET + RIGHT_WIDTH / 2,
                top + AttributePanelMenu.BACKPACK_Y - 12,
                0xFFFFFF
        );
    }

    private List<Component> renderStats(GuiGraphics graphics, int mouseX, int mouseY) {
        int listX = left + MIDDLE_X_OFFSET;
        int listY = statListY();
        int listHeight = statListHeight();
        int listWidth = MIDDLE_WIDTH;
        int buttonX = left + MIDDLE_X_OFFSET + MIDDLE_WIDTH - 24;

        List<Component> tooltip = null;
        enableResponsiveScissor(graphics, listX, listY, listX + listWidth, listY + listHeight);
        for (int i = 0; i < snapshot.stats().size(); i++) {
            StatSnapshot stat = snapshot.stats().get(i);
            int y = listY + i * ROW_HEIGHT - scroll;
            if (y + ROW_HEIGHT < listY || y > listY + listHeight) {
                continue;
            }

            graphics.fill(listX, y, listX + listWidth, y + 23, TdmcUiTheme.ROW_BACKGROUND);
            renderIcon(graphics, stat.icon(), listX + 4, y + 3);

            boolean pointStat = !"torpor".equals(stat.id());
            boolean showBar = shouldRenderStatBar(stat);
            Component name = Component.translatable(stat.nameKey());
            Component value = valueText(stat);
            Component unavailable = Component.translatable("gui.tl_domesticate_more_creatures.unavailable");

            int contentLeft = listX + 26;
            int contentRight = pointStat ? buttonX - 6 : listX + listWidth - 8;
            int textWidth = stat.available() ? font.width(value) : font.width(unavailable);
            int valueX = Math.max(contentLeft, contentRight - textWidth);

            graphics.drawString(font, name, contentLeft, y + 2, 0xFFFFFF, true);
            if (stat.available()) {
                graphics.drawString(font, value, valueX, showBar ? y + 2 : y + 7, 0xFFFFFF, true);
            } else {
                graphics.drawString(font, unavailable, valueX, showBar ? y + 2 : y + 7, 0xFFFFFF, true);
            }

            if (showBar) {
                int barX = contentLeft;
                int barRight = contentRight;
                int barWidth = Math.max(20, barRight - barX);
                int barY = y + 13;
                graphics.fill(barX, barY, barX + barWidth, barY + 5, TdmcUiTheme.BAR_BACKGROUND);
                double ratio = barRatio(stat);
                int fill = (int) Math.round((barWidth - 2) * ratio);
                if (fill > 0) {
                    graphics.fill(barX + 1, barY + 1, barX + 1 + fill, barY + 4, fillColor(stat));
                }
            }

            if (pointStat
                    && mouseX >= listX && mouseX <= listX + listWidth - 1
                    && mouseY >= y && mouseY <= y + 23) {
                tooltip = new ArrayList<>();
                tooltip.add(Component.translatable("gui.tl_domesticate_more_creatures.points_wild", stat.wildPoints()));
                tooltip.add(Component.translatable("gui.tl_domesticate_more_creatures.points_trained", stat.trainedPoints()));
                tooltip.add(Component.translatable("gui.tl_domesticate_more_creatures.points_talent", stat.talentPoints()));
                tooltip.add(Component.translatable("gui.tl_domesticate_more_creatures.points_total", stat.totalPoints()));
            }
        }
        graphics.disableScissor();
        return tooltip;
    }

    private void renderExperience(GuiGraphics graphics) {
        int x = left + MIDDLE_X_OFFSET + (MIDDLE_WIDTH - EXPERIENCE_BAR_WIDTH) / 2;
        int y = experienceBarY();
        int width = EXPERIENCE_BAR_WIDTH;
        int height = EXPERIENCE_BAR_HEIGHT;
        graphics.fill(x, y, x + width, y + height, TdmcUiTheme.BAR_BACKGROUND);
        if (snapshot.experienceRequired() > 0L) {
            double ratio = Math.min(1.0D, (double) snapshot.experience() / snapshot.experienceRequired());
            graphics.fill(x + 1, y + 1, x + 1 + (int) ((width - 2) * ratio), y + height - 1, TdmcUiTheme.BUTTON_PRESSED);
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.tl_domesticate_more_creatures.experience", snapshot.experience(), snapshot.experienceRequired()),
                    x + width / 2,
                    y,
                    0xFFFFFF
            );
        } else if (!snapshot.player() && snapshot.tamed()) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, TdmcUiTheme.BUTTON_PRESSED);
            Component stored = Component.translatable(
                    "gui.tl_domesticate_more_creatures.stored_experience",
                    Component.literal(Long.toString(snapshot.experience())).withStyle(ChatFormatting.GREEN)
            );
            graphics.drawCenteredString(
                    font,
                    stored,
                    x + width / 2,
                    y,
                    0xFFFFFF
            );
        } else {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("gui.tl_domesticate_more_creatures.max_level"),
                    x + width / 2,
                    y,
                    0xFFFFFF
            );
        }
    }

    private void renderIcon(GuiGraphics graphics, String iconId, int x, int y) {
        ResourceLocation id = ResourceLocation.tryParse(iconId);
        if (id != null && isTextureIcon(id)) {
            graphics.blit(id, x, y, 0, 0, 16, 16, 16, 16);
            return;
        }
        Item item = id == null ? Items.STONE : ForgeRegistries.ITEMS.getValue(id);
        if (item == null) {
            item = Items.STONE;
        }
        graphics.renderFakeItem(new ItemStack(item), x, y);
    }

    private boolean isTextureIcon(ResourceLocation id) {
        return id.getPath().endsWith(".png");
    }

    private Component valueText(StatSnapshot stat) {
        if (stat.dynamicDisplay()) {
            return Component.literal(number(stat.currentValue()) + "/" + number(stat.maxValue()));
        }
        return formatValue(stat.displayedValue(), stat.displayFormat());
    }

    private Component formatValue(double value, String displayFormat) {
        if ("MULTIPLIER".equals(displayFormat)) {
            return Component.translatable("gui.tl_domesticate_more_creatures.value_multiplier", String.format(Locale.ROOT, "%.2f", value));
        }
        if ("PERCENT".equals(displayFormat)) {
            return Component.translatable("gui.tl_domesticate_more_creatures.value_percent", String.format(Locale.ROOT, "%.1f", value * 100.0D));
        }
        return Component.literal(number(value));
    }

    private String number(double value) {
        return Math.abs(value - Math.rint(value)) < 0.0001D
                ? Long.toString(Math.round(value))
                : String.format(Locale.ROOT, "%.2f", value);
    }

    private boolean shouldRenderStatBar(StatSnapshot stat) {
        return stat.dynamicDisplay() && stat.maxValue() > 0.0D;
    }

    private double barRatio(StatSnapshot stat) {
        if (!shouldRenderStatBar(stat)) {
            return 0.0D;
        }
        return Mth.clamp(stat.currentValue() / stat.maxValue(), 0.0D, 1.0D);
    }

    private int fillColor(StatSnapshot stat) {
        return switch (stat.id()) {
            case "health" -> 0xFFD84C4C;
            case "armor" -> 0xFFE0E0E0;
            case "damage" -> 0xFFE2C04E;
            case "speed" -> 0xFF63DD7D;
            case "swim_speed" -> 0xFF53C4FF;
            case "resistance" -> 0xFFBA68FF;
            case "torpor" -> 0xFFE58A45;
            case "tl_domesticate_more_creatures:mana" -> 0xFF9A5CFF;
            default -> 0xFF6AA9FF;
        };
    }

    private static Component talentName(TalentSnapshot talent) {
        String translated = Component.translatable(talent.nameKey()).getString();
        String plain = ChatFormatting.stripFormatting(translated);

        return Component.literal(plain == null ? translated : plain)
                .withStyle(ChatFormatting.BOLD);
    }

    private int talentBackground(int level) {
        if (level <= 1) {
            return 0xFFD8D8D8;
        }
        if (level == 2) {
            return 0xFF3F7FD9;
        }
        return 0xFFD6A62A;
    }

    private int talentTextColor(int level) {
        return level == 2 ? 0xFFFFFFFF : 0xFF202020;
    }


    private void updateMountedTargetSwitchButton() {
        if (mountedTargetSwitchButton == null || minecraft == null || minecraft.player == null) {
            return;
        }
        int playerEntityId = minecraft.player.getId();
        boolean viewingPlayer = snapshot.entityId() == playerEntityId;
        if (!viewingPlayer) {
            ClientState.setPanelCompanionEntityId(snapshot.entityId());
            mountedTargetSwitchButton.visible = true;
            mountedTargetSwitchButton.active = true;
            mountedTargetSwitchButton.setMessage(Component.translatable(
                    "gui.tl_domesticate_more_creatures.panel.switch_player"));
            return;
        }

        int companionEntityId = currentPanelCompanionEntityId();
        boolean visible = companionEntityId >= 0;
        mountedTargetSwitchButton.visible = visible;
        mountedTargetSwitchButton.active = visible;
        if (visible) {
            mountedTargetSwitchButton.setMessage(Component.translatable(
                    "gui.tl_domesticate_more_creatures.panel.switch_pet"));
        }
    }

    private int currentPanelCompanionEntityId() {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            return -1;
        }
        Entity vehicle = minecraft.player.getVehicle();
        if (vehicle instanceof LivingEntity living) {
            ClientState.setPanelCompanionEntityId(living.getId());
            return living.getId();
        }
        int companionEntityId = ClientState.panelCompanionEntityId();
        if (companionEntityId < 0 || companionEntityId == minecraft.player.getId()) {
            return -1;
        }
        Entity entity = minecraft.level.getEntity(companionEntityId);
        if (entity instanceof LivingEntity) {
            return companionEntityId;
        }
        ClientState.clearPanelCompanionEntityId();
        return -1;
    }

    private void openVanillaInventory() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        ClientState.setInventoryReturnPanelEntityId(snapshot.entityId());
        minecraft.setScreen(new InventoryScreen(minecraft.player));
    }

    private void switchMountedPanelTarget() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        if (snapshot.entityId() != minecraft.player.getId()) {
            ClientState.setPanelCompanionEntityId(snapshot.entityId());
            NetworkHandler.openPanel(minecraft.player.getId());
            return;
        }
        int companionEntityId = currentPanelCompanionEntityId();
        if (companionEntityId >= 0) {
            NetworkHandler.openPanel(companionEntityId);
        } else {
            updateMountedTargetSwitchButton();
        }
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, snapshot.stats().size() * ROW_HEIGHT - statListHeight());
        if (maxScroll <= 0) {
            return super.mouseScrolled(toLayoutX(mouseX), toLayoutY(mouseY), delta);
        }
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(delta) * ROW_HEIGHT));
        layoutButtons();
        return true;
    }

    private void layoutButtons() {
        int listY = statListY();
        int listHeight = statListHeight();
        int buttonX = left + MIDDLE_X_OFFSET + MIDDLE_WIDTH - 24;
        for (StatButton statButton : statButtons) {
            int y = listY + statButton.index() * ROW_HEIGHT - scroll + 2;
            statButton.button().setX(buttonX);
            statButton.button().setY(y);
            statButton.button().visible = y >= listY && y + 20 <= listY + listHeight;
        }
    }

    private int unspentY() {
        int equipmentBottom = top + AttributePanelMenu.EQUIPMENT_TOP_Y
                + menu.equipmentRowCount() * AttributePanelMenu.EQUIPMENT_SLOT_SPACING;
        return Math.max(top + 99, equipmentBottom + 3);
    }

    private int rightTalentStartY() {
        int summaryLines = panelSummaryLines().size();
        if (summaryLines <= 0) {
            return top + 66;
        }
        return Math.max(top + 66, rightSummaryStartY() + summaryLines * 12 + 2);
    }

    private int rightImprintStartY() {
        int y = rightTalentStartY() + talentRowCount(snapshot.talents()) * TALENT_ROW_STEP;
        if (snapshot.fury().available()) {
            y += FURY_ROW_HEIGHT;
        }
        return y + 2;
    }

    private int rightPreviewTopY() {
        int y = rightImprintStartY();
        if (snapshot.imprint().available()) {
            y += 38;
        }
        return Math.max(top + 112, y + 4);
    }

    private int experienceBarY() {
        return unspentY() + 12;
    }

    private int statListY() {
        return Math.max(top + LIST_Y_OFFSET, experienceBarY() + EXPERIENCE_BAR_HEIGHT + 4);
    }

    private int statListHeight() {
        return Math.max(25, top + PANEL_HEIGHT - 8 - statListY());
    }

    private static int talentRowCount(List<TalentSnapshot> talents) {
        boolean normal = false;
        boolean special = false;
        for (TalentSnapshot talent : talents) {
            if (talent.special()) {
                special = true;
            } else {
                normal = true;
            }
        }
        return (normal ? 1 : 0) + (special ? 1 : 0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record StatButton(int index, TdmcButton button) {
    }
}
