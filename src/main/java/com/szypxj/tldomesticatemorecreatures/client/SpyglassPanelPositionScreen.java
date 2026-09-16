package com.szypxj.tldomesticatemorecreatures.client;

import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStats;
import com.szypxj.tldomesticatemorecreatures.api.creature.BaseStatsSource;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcButton;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcUiTheme;
import com.szypxj.tldomesticatemorecreatures.config.ClientConfig;
import com.szypxj.tldomesticatemorecreatures.network.InspectSnapshot;
import com.szypxj.tldomesticatemorecreatures.network.FurySnapshot;
import com.szypxj.tldomesticatemorecreatures.network.ImprintSnapshot;
import com.szypxj.tldomesticatemorecreatures.network.InspectTarget;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.network.RideStatusSnapshot;
import com.szypxj.tldomesticatemorecreatures.network.TalentSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SpyglassPanelPositionScreen extends Screen {
    private static final InspectSnapshot PREVIEW = createPreview();
    private final Screen parent;
    private final List<TdmcButton> presetButtons = new ArrayList<>();
    private SpyglassPanelLayout.Anchor anchor;
    private int offsetX;
    private int offsetY;
    private int panelX;
    private int panelY;
    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    private boolean presetsVisible;

    public SpyglassPanelPositionScreen(Screen parent) {
        super(Component.translatable("gui.tl_domesticate_more_creatures.spyglass_position.title"));
        this.parent = parent;
        this.anchor = SpyglassPanelLayout.Anchor.parse(ClientConfig.SPYGLASS_PANEL_ANCHOR.get());
        this.offsetX = ClientConfig.SPYGLASS_PANEL_OFFSET_X.get();
        this.offsetY = ClientConfig.SPYGLASS_PANEL_OFFSET_Y.get();
    }

    @Override
    protected void init() {
        resolveCurrentPosition();
        int buttonWidth = 68;
        int gap = 4;
        int totalWidth = buttonWidth * 4 + gap * 3;
        int startX = Math.max(4, (width - totalWidth) / 2);
        int buttonY = height - 28;

        addRenderableWidget(TdmcButton.create(
                startX, buttonY, buttonWidth, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.spyglass_position.presets"),
                button -> togglePresets()
        ));
        addRenderableWidget(TdmcButton.create(
                startX + (buttonWidth + gap), buttonY, buttonWidth, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.spyglass_position.reset"),
                button -> setPreset(SpyglassPanelLayout.Anchor.RIGHT_CENTER)
        ));
        addRenderableWidget(TdmcButton.create(
                startX + (buttonWidth + gap) * 2, buttonY, buttonWidth, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.spyglass_position.cancel"),
                button -> closeWithoutSaving()
        ));
        addRenderableWidget(TdmcButton.create(
                startX + (buttonWidth + gap) * 3, buttonY, buttonWidth, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.spyglass_position.done"),
                button -> saveAndClose()
        ));

        createPresetButtons();
        updatePresetVisibility();
    }

    private void createPresetButtons() {
        int buttonWidth = 76;
        int buttonHeight = 20;
        int gap = 4;
        int startX = (width - (buttonWidth * 3 + gap * 2)) / 2;
        int startY = Math.max(42, (height - (buttonHeight * 3 + gap * 2)) / 2);
        SpyglassPanelLayout.Anchor[] anchors = SpyglassPanelLayout.Anchor.values();
        for (int i = 0; i < anchors.length; i++) {
            SpyglassPanelLayout.Anchor preset = anchors[i];
            int column = i % 3;
            int row = i / 3;
            TdmcButton button = TdmcButton.create(
                    startX + column * (buttonWidth + gap),
                    startY + row * (buttonHeight + gap),
                    buttonWidth,
                    buttonHeight,
                    Component.translatable(anchorKey(preset)),
                    pressed -> setPreset(preset)
            );
            presetButtons.add(addRenderableWidget(button));
        }
    }

    private void togglePresets() {
        presetsVisible = !presetsVisible;
        updatePresetVisibility();
    }

    private void updatePresetVisibility() {
        for (TdmcButton button : presetButtons) {
            button.visible = presetsVisible;
            button.active = presetsVisible;
        }
    }

    private void setPreset(SpyglassPanelLayout.Anchor preset) {
        anchor = preset;
        offsetX = 0;
        offsetY = 0;
        resolveCurrentPosition();
        presetsVisible = false;
        updatePresetVisibility();
    }

    private void resolveCurrentPosition() {
        SpyglassPanelLayout.Position position = SpyglassPanelLayout.resolve(
                anchor,
                offsetX,
                offsetY,
                width,
                height,
                SpyglassPanelRenderer.scaledWidth(PREVIEW),
                SpyglassPanelRenderer.scaledHeight(PREVIEW),
                SpyglassPanelRenderer.SCREEN_MARGIN
        );
        panelX = position.x();
        panelY = position.y();
    }

    private void updateSavedPositionFromAbsolute() {
        anchor = SpyglassPanelLayout.nearestAnchor(
                panelX,
                panelY,
                width,
                height,
                SpyglassPanelRenderer.scaledWidth(PREVIEW),
                SpyglassPanelRenderer.scaledHeight(PREVIEW),
                SpyglassPanelRenderer.SCREEN_MARGIN
        );
        SpyglassPanelLayout.SavedPosition saved = SpyglassPanelLayout.fromAbsolute(
                panelX,
                panelY,
                anchor,
                width,
                height,
                SpyglassPanelRenderer.scaledWidth(PREVIEW),
                SpyglassPanelRenderer.scaledHeight(PREVIEW),
                SpyglassPanelRenderer.SCREEN_MARGIN
        );
        offsetX = saved.offsetX();
        offsetY = saved.offsetY();
    }

    private void saveAndClose() {
        updateSavedPositionFromAbsolute();
        ClientConfig.setSpyglassPanelPosition(anchor.name(), offsetX, offsetY);
        returnToParent();
    }

    private void closeWithoutSaving() {
        returnToParent();
    }

    private void returnToParent() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
            if (parent instanceof AttributePanelScreen panelScreen) {
                NetworkHandler.refreshOpenPanel(panelScreen.entityId());
            }
        }
    }

    @Override
    public void onClose() {
        closeWithoutSaving();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        graphics.drawCenteredString(
                font,
                Component.translatable("gui.tl_domesticate_more_creatures.spyglass_position.hint"),
                width / 2,
                24,
                0xFFFFFF
        );

        SpyglassPanelRenderer.renderAt(graphics, PREVIEW, panelX, panelY);

        if (presetsVisible) {
            int popupWidth = 252;
            int popupHeight = 76;
            int popupX = (width - popupWidth) / 2;
            int popupY = Math.max(36, (height - popupHeight) / 2 - 4);
            TdmcUiTheme.fillPanel(graphics, popupX, popupY, popupWidth, popupHeight);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && !presetsVisible && isInsidePanel(mouseX, mouseY)) {
            dragging = true;
            dragOffsetX = mouseX - panelX;
            dragOffsetY = mouseY - panelY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            SpyglassPanelLayout.Position position = SpyglassPanelLayout.clamp(
                    (int) Math.round(mouseX - dragOffsetX),
                    (int) Math.round(mouseY - dragOffsetY),
                    width,
                    height,
                    SpyglassPanelRenderer.scaledWidth(PREVIEW),
                    SpyglassPanelRenderer.scaledHeight(PREVIEW),
                    SpyglassPanelRenderer.SCREEN_MARGIN
            );
            panelX = position.x();
            panelY = position.y();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            updateSavedPositionFromAbsolute();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isInsidePanel(double mouseX, double mouseY) {
        return mouseX >= panelX
                && mouseX < panelX + SpyglassPanelRenderer.scaledWidth(PREVIEW)
                && mouseY >= panelY
                && mouseY < panelY + SpyglassPanelRenderer.scaledHeight(PREVIEW);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String anchorKey(SpyglassPanelLayout.Anchor anchor) {
        return "gui.tl_domesticate_more_creatures.spyglass_position.anchor." + anchor.name().toLowerCase(Locale.ROOT);
    }

    private static InspectSnapshot createPreview() {
        List<TalentSnapshot> talents = List.of(
                new TalentSnapshot("vitality", "talent.tl_domesticate_more_creatures.vitality", 1, false, List.of()),
                new TalentSnapshot("windchaser", "talent.tl_domesticate_more_creatures.windchaser", 2, false, List.of()),
                new TalentSnapshot("valor", "talent.tl_domesticate_more_creatures.valor", 3, false, List.of()),
                new TalentSnapshot("bloodthirsty", "talent.tl_domesticate_more_creatures.special.bloodthirsty", 0, true, List.of()),
                new TalentSnapshot("fury", "talent.tl_domesticate_more_creatures.special.fury", 0, true, List.of())
        );
        List<InspectSnapshot.InspectStat> stats = List.of(
                new InspectSnapshot.InspectStat("health", "attribute.name.generic.max_health", 40, 100.0D, "NUMBER", 82.0D, 100.0D),
                new InspectSnapshot.InspectStat("damage", "attribute.name.generic.attack_damage", 28, 16.5D, "NUMBER", 16.5D, 0.0D),
                new InspectSnapshot.InspectStat("speed", "attribute.name.generic.movement_speed", 22, 0.42D, "NUMBER", 0.42D, 0.0D),
                new InspectSnapshot.InspectStat("resistance", "attribute.name.generic.armor", 15, 8.0D, "NUMBER", 8.0D, 0.0D)
        );
        List<InspectSnapshot.TamingFoodSnapshot> foods = List.of(
                new InspectSnapshot.TamingFoodSnapshot("item.minecraft.beef", 8, true),
                new InspectSnapshot.TamingFoodSnapshot("item.minecraft.golden_carrot", 2, true)
        );
        return new InspectSnapshot(
                InspectTarget.entity(-1),
                Component.translatable("gui.tl_domesticate_more_creatures.spyglass_position.preview_name"),
                Component.literal("§aTDMC"),
                ResourceLocation.tryBuild("minecraft", "ravager"),
                true,
                new BaseStats(100.0D, 12.0D, 0.30D, BaseStatsSource.DEFAULT_ATTRIBUTES),
                75,
                true,
                false,
                false,
                0,
                0,
                false,
                false,
                RideStatusSnapshot.NONE,
                new ImprintSnapshot(true, true, false, 66, 8400L, "PET", "", 0L, 0, false),
                talents,
                new FurySnapshot(true, 32.0D, 80.0D, false),
                new InspectSnapshot.RadarSnapshot(true, 72, 91, 48),
                stats,
                "spyglass.tl_domesticate_more_creatures.taming_method_knockout",
                foods,
                true,
                0.64D,
                true,
                420.0D,
                800.0D,
                80
        );
    }
}
