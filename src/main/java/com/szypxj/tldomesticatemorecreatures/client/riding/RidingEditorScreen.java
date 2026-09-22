package com.szypxj.tldomesticatemorecreatures.client.riding;

import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcButton;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcConfirmScreen;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcEditBox;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcSliderButton;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcUiTheme;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.riding.RideMode;
import com.szypxj.tldomesticatemorecreatures.riding.RideMovementMode;
import com.szypxj.tldomesticatemorecreatures.riding.config.EntityRideProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.RideFilterMode;
import com.szypxj.tldomesticatemorecreatures.riding.config.RiderPosePreset;
import com.szypxj.tldomesticatemorecreatures.riding.config.RiderVisualProfile;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingConfigSnapshot;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleConsumer;

public final class RidingEditorScreen extends Screen {
    private static final int LEFT_WIDTH = 190;
    private static final int ROW_HEIGHT = 30;
    private static final int LIST_TOP = 86;
    private static final int CONTROL_TOP = 88;
    private static final int CONTROL_ROW_HEIGHT = 22;
    private final RidingEditorModel model = new RidingEditorModel();
    private final List<RowLabel> rowLabels = new ArrayList<>();
    private TdmcEditBox searchBox;
    private List<ResourceLocation> visibleIds = List.of();
    private String searchText = "";
    private int listScroll;
    private int controlScroll;
    private int maxControlScroll;
    private EditorTab tab = EditorTab.RIDE;
    private ListFilter filter = ListFilter.ALL;
    private LivingEntity previewMount;
    private RemotePlayer previewRider;
    private ResourceLocation previewEntityId;
    private boolean previewFailed;
    private boolean rotatingPreview;
    private double lastDragX;
    private double lastDragY;
    private float previewYaw = 25.0F;
    private float previewPitch = -10.0F;
    private float previewZoom = 1.0F;

    public RidingEditorScreen() {
        super(Component.translatable("gui.tl_domesticate_more_creatures.riding.title"));
    }

    public RidingEditorModel model() {
        return model;
    }

    @Override
    protected void init() {
        rowLabels.clear();
        int top = 8;
        int right = width - 8;
        addRenderableWidget(TdmcButton.create(
                right - 216, top, 68, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.riding.save"),
                button -> saveAll()
        ));
        addRenderableWidget(TdmcButton.create(
                right - 144, top, 68, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.riding.reload"),
                button -> requestReload()
        ));
        addRenderableWidget(TdmcButton.create(
                right - 72, top, 68, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.riding.done"),
                button -> onClose()
        ));

        searchBox = new TdmcEditBox(font, 8, 36, LEFT_WIDTH - 16, 20, Component.translatable("gui.tl_domesticate_more_creatures.riding.search"));
        searchBox.setHint(Component.translatable("gui.tl_domesticate_more_creatures.riding.search"));
        searchBox.setValue(searchText);
        searchBox.setResponder(value -> {
            searchText = value;
            rebuildVisible();
        });
        addRenderableWidget(searchBox);

        addRenderableWidget(TdmcButton.create(
                8, 60, LEFT_WIDTH - 16, 20,
                Component.translatable(filter.key),
                button -> {
                    filter = filter.next();
                    listScroll = 0;
                    rebuildEditorWidgets();
                }
        ));

        int tabX = LEFT_WIDTH + 8;
        int tabWidth = Math.max(52, Math.min(84, (width - LEFT_WIDTH - 250) / 4));
        for (EditorTab value : EditorTab.values()) {
            EditorTab target = value;
            addRenderableWidget(TdmcButton.create(
                    tabX, 36, tabWidth, 20,
                    Component.translatable(target.key),
                    button -> {
                        tab = target;
                        controlScroll = 0;
                        rebuildEditorWidgets();
                    }
            ));
            tabX += tabWidth + 4;
        }

        rebuildVisible();
        if (model.selected() == null && !visibleIds.isEmpty()) {
            model.select(visibleIds.get(0));
        }
        buildTabControls();
    }

    private void rebuildEditorWidgets() {
        if (searchBox != null) {
            searchText = searchBox.getValue();
        }
        clearWidgets();
        init();
    }

    private void rebuildVisible() {
        String query = searchText.trim().toLowerCase(Locale.ROOT);
        List<ResourceLocation> result = new ArrayList<>();
        for (ResourceLocation id : ClientRidingConfigCache.configurableEntityIds()) {
            String idText = id.toString().toLowerCase(Locale.ROOT);
            String localizedName = entityDisplayName(id).getString().toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !idText.contains(query) && !localizedName.contains(query)) {
                continue;
            }
            if (!matchesFilter(id)) {
                continue;
            }
            result.add(id);
        }
        result.sort(Comparator
                .comparing((ResourceLocation id) -> !(model.isDirty(id) || ClientRidingConfigCache.modifiedProfiles().containsKey(id)))
                .thenComparing(id -> entityDisplayName(id).getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ResourceLocation::toString));
        visibleIds = List.copyOf(result);
        if (model.selected() != null && !visibleIds.contains(model.selected())) {
            model.clearSelection();
            invalidatePreview();
        }
        if (model.selected() == null && !visibleIds.isEmpty()) {
            model.select(visibleIds.get(0));
        }
        int max = Math.max(0, visibleIds.size() * ROW_HEIGHT - Math.max(1, height - LIST_TOP - 10));
        listScroll = Mth.clamp(listScroll, 0, max);
    }

    private Component entityDisplayName(ResourceLocation id) {
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

    private boolean matchesFilter(ResourceLocation id) {
        return switch (filter) {
            case ALL -> true;
            case MODIFIED -> model.isDirty(id) || ClientRidingConfigCache.modifiedProfiles().containsKey(id);
            case DISABLED -> currentProfile(id).mode() == RideMode.DISABLED;
            case BLACKLIST -> model.workingSettings().filterMode() == RideFilterMode.BLACKLIST
                    && model.workingSettings().filterEntries().contains(id);
            case WHITELIST -> model.workingSettings().filterMode() == RideFilterMode.WHITELIST
                    && model.workingSettings().filterEntries().contains(id);
        };
    }

    private EntityRideProfile currentProfile(ResourceLocation id) {
        return model.isDirty(id) ? model.draft(id) : ClientRidingConfigCache.profile(id);
    }

    private void saveAll() {
        if (model.settingsDirty()) {
            NetworkHandler.updateRidingSettings(model.workingSettings());
        }
        for (ResourceLocation entityId : model.dirtyProfiles()) {
            EntityRideProfile profile = model.draft(entityId);
            if (profile != null) {
                NetworkHandler.updateRidingProfile(profile);
            }
        }
    }

    private void requestReload() {
        if (minecraft == null) {
            return;
        }
        if (!model.anyDirty()) {
            NetworkHandler.reloadRidingConfig();
            return;
        }
        minecraft.setScreen(new TdmcConfirmScreen(
                accepted -> {
                    if (accepted) {
                        model.discardAll();
                        NetworkHandler.reloadRidingConfig();
                    }
                    if (minecraft != null) {
                        minecraft.setScreen(this);
                    }
                },
                Component.translatable("gui.tl_domesticate_more_creatures.riding.reload_confirm_title"),
                Component.translatable("gui.tl_domesticate_more_creatures.riding.reload_confirm_message"),
                Component.translatable("gui.tl_domesticate_more_creatures.riding.reload_discard"),
                Component.translatable("gui.tl_domesticate_more_creatures.riding.cancel")
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
        minecraft.setScreen(new TdmcConfirmScreen(
                save -> {
                    if (save) {
                        saveAll();
                    } else {
                        model.discardAll();
                    }
                    if (minecraft != null) {
                        minecraft.setScreen(null);
                    }
                },
                Component.translatable("gui.tl_domesticate_more_creatures.riding.unsaved_title"),
                Component.translatable("gui.tl_domesticate_more_creatures.riding.unsaved_message"),
                Component.translatable("gui.tl_domesticate_more_creatures.riding.save"),
                Component.translatable("gui.tl_domesticate_more_creatures.riding.discard")
        ));
    }

    public void onServerSnapshot(RidingConfigSnapshot snapshot) {
        model.markSettingsSaved(snapshot.generation());
        model.refreshCleanDrafts();
        rebuildVisible();
    }

    public void onProfileSaved(long generation, EntityRideProfile profile) {
        model.markSaved(profile.entityId(), generation);
        rebuildVisible();
    }

    private void buildTabControls() {
        EntityRideProfile profile = model.workingProfile();
        if (profile == null) {
            maxControlScroll = 0;
            return;
        }
        int[] row = {0};
        switch (tab) {
            case RIDE -> buildRideControls(profile, row);
            case SEAT -> buildSeatControls(profile, row);
            case POSE -> buildPoseControls(profile, row);
            case CAMERA -> buildCameraControls(profile, row);
        }
        int visibleHeight = Math.max(1, height - CONTROL_TOP - 16);
        maxControlScroll = Math.max(0, row[0] * CONTROL_ROW_HEIGHT - visibleHeight);
        controlScroll = Mth.clamp(controlScroll, 0, maxControlScroll);
    }

    private void buildRideControls(EntityRideProfile profile, int[] row) {
        addButtonRow("gui.tl_domesticate_more_creatures.riding.mode", modeText(profile.mode()), row,
                () -> model.update(value -> withMode(value, nextMode(value.mode()))));
        addButtonRow("gui.tl_domesticate_more_creatures.riding.movement", movementText(profile.movementMode()), row,
                () -> model.update(value -> withMovement(value, nextMovement(value.movementMode()))));
        addButtonRow("gui.tl_domesticate_more_creatures.riding.filter_mode", filterModeText(model.workingSettings().filterMode()), row,
                () -> model.updateSettings(settings -> new RidingSettings(
                        settings.filterMode() == RideFilterMode.BLACKLIST ? RideFilterMode.WHITELIST : RideFilterMode.BLACKLIST,
                        settings.filterEntries()
                )));
        boolean listed = model.workingSettings().filterEntries().contains(profile.entityId());
        addButtonRow("gui.tl_domesticate_more_creatures.riding.filter_entry",
                Component.translatable(listed
                        ? "gui.tl_domesticate_more_creatures.riding.filter_remove"
                        : "gui.tl_domesticate_more_creatures.riding.filter_add"), row,
                () -> toggleFilterEntry(profile.entityId()));
        addNumberRow("gui.tl_domesticate_more_creatures.riding.ground_speed", profile.groundSpeedMultiplier(), row,
                value -> updateProfileNumber(ProfileNumber.GROUND_SPEED, value));
        addNumberRow("gui.tl_domesticate_more_creatures.riding.turn_rate", profile.turnRateDegrees(), row,
                value -> updateProfileNumber(ProfileNumber.TURN_RATE, value));
        addNumberRow("gui.tl_domesticate_more_creatures.riding.acceleration", profile.acceleration(), row,
                value -> updateProfileNumber(ProfileNumber.ACCELERATION, value));
        addNumberRow("gui.tl_domesticate_more_creatures.riding.deceleration", profile.deceleration(), row,
                value -> updateProfileNumber(ProfileNumber.DECELERATION, value));
        addNumberRow("gui.tl_domesticate_more_creatures.riding.jump_strength", profile.jumpStrength(), row,
                value -> updateProfileNumber(ProfileNumber.JUMP, value));
        addNumberRow("gui.tl_domesticate_more_creatures.riding.flight_speed", profile.flightSpeedMultiplier(), row,
                value -> updateProfileNumber(ProfileNumber.FLIGHT_SPEED, value));
        addNumberRow("gui.tl_domesticate_more_creatures.riding.ascent_speed", profile.ascentSpeed(), row,
                value -> updateProfileNumber(ProfileNumber.ASCENT, value));
        addNumberRow("gui.tl_domesticate_more_creatures.riding.descent_speed", profile.descentSpeed(), row,
                value -> updateProfileNumber(ProfileNumber.DESCENT, value));
        addNumberRow("gui.tl_domesticate_more_creatures.riding.swim_speed", profile.swimSpeedMultiplier(), row,
                value -> updateProfileNumber(ProfileNumber.SWIM_SPEED, value));
        addButtonRow("gui.tl_domesticate_more_creatures.riding.auto_attack", booleanText(profile.autoAttackWhileRidden()), row,
                () -> model.update(value -> withAutoAttack(value, !value.autoAttackWhileRidden())));
        addButtonRow("gui.tl_domesticate_more_creatures.riding.visual_override", booleanText(profile.visualOverride()), row,
                () -> model.update(value -> withVisualOverride(value, !value.visualOverride())));
    }

    private void buildSeatControls(EntityRideProfile profile, int[] row) {
        RiderVisualProfile visual = profile.visual();
        addNumberRow("gui.tl_domesticate_more_creatures.riding.lateral_ratio", visual.lateralRatio(), row,
                value -> updateVisualNumber(VisualNumber.LATERAL_RATIO, value));
        addNumberRow("gui.tl_domesticate_more_creatures.riding.vertical_ratio", visual.verticalRatio(), row,
                value -> updateVisualNumber(VisualNumber.VERTICAL_RATIO, value));
        addNumberRow("gui.tl_domesticate_more_creatures.riding.forward_ratio", visual.forwardRatio(), row,
                value -> updateVisualNumber(VisualNumber.FORWARD_RATIO, value));
        addNumberRow("gui.tl_domesticate_more_creatures.riding.offset_x", visual.offsetX(), row,
                value -> updateVisualNumber(VisualNumber.OFFSET_X, value));
        addNumberRow("gui.tl_domesticate_more_creatures.riding.offset_y", visual.offsetY(), row,
                value -> updateVisualNumber(VisualNumber.OFFSET_Y, value));
        addNumberRow("gui.tl_domesticate_more_creatures.riding.offset_z", visual.offsetZ(), row,
                value -> updateVisualNumber(VisualNumber.OFFSET_Z, value));
        addNumberRow("gui.tl_domesticate_more_creatures.riding.player_scale", visual.visualPlayerScale(), row,
                value -> updateVisualNumber(VisualNumber.PLAYER_SCALE, value));
        addButtonRow("gui.tl_domesticate_more_creatures.riding.reset", Component.translatable("gui.tl_domesticate_more_creatures.riding.reset"), row,
                model::resetCurrent);
        addButtonRow("gui.tl_domesticate_more_creatures.riding.copy", Component.translatable("gui.tl_domesticate_more_creatures.riding.copy"), row,
                model::copyCurrent);
        addButtonRow("gui.tl_domesticate_more_creatures.riding.paste", Component.translatable("gui.tl_domesticate_more_creatures.riding.paste"), row,
                model::pasteToCurrent);
    }

    private void buildPoseControls(EntityRideProfile profile, int[] row) {
        RiderVisualProfile visual = profile.visual();
        addButtonRow("gui.tl_domesticate_more_creatures.riding.pose_preset", poseText(visual.posePreset()), row,
                () -> model.update(value -> withPosePreset(value, nextPose(value.visual().posePreset()))));
        addNumberRow("gui.tl_domesticate_more_creatures.riding.body_pitch", visual.bodyPitch(), row,
                value -> updateVisualNumber(VisualNumber.BODY_PITCH, value));
        addNumberRow("gui.tl_domesticate_more_creatures.riding.body_yaw", visual.bodyYawOffset(), row,
                value -> updateVisualNumber(VisualNumber.BODY_YAW, value));
        addLimbRows("left_leg", visual.leftLeg(), VisualNumber.LEFT_LEG_PITCH, VisualNumber.LEFT_LEG_YAW, VisualNumber.LEFT_LEG_ROLL, row);
        addLimbRows("right_leg", visual.rightLeg(), VisualNumber.RIGHT_LEG_PITCH, VisualNumber.RIGHT_LEG_YAW, VisualNumber.RIGHT_LEG_ROLL, row);
        addLimbRows("left_arm", visual.leftArm(), VisualNumber.LEFT_ARM_PITCH, VisualNumber.LEFT_ARM_YAW, VisualNumber.LEFT_ARM_ROLL, row);
        addLimbRows("right_arm", visual.rightArm(), VisualNumber.RIGHT_ARM_PITCH, VisualNumber.RIGHT_ARM_YAW, VisualNumber.RIGHT_ARM_ROLL, row);
    }

    private void addLimbRows(
            String limb,
            RiderVisualProfile.LimbRotation rotation,
            VisualNumber pitch,
            VisualNumber yaw,
            VisualNumber roll,
            int[] row
    ) {
        addNumberRow("gui.tl_domesticate_more_creatures.riding." + limb + "_pitch", rotation.pitch(), row, value -> updateVisualNumber(pitch, value));
        addNumberRow("gui.tl_domesticate_more_creatures.riding." + limb + "_yaw", rotation.yaw(), row, value -> updateVisualNumber(yaw, value));
        addNumberRow("gui.tl_domesticate_more_creatures.riding." + limb + "_roll", rotation.roll(), row, value -> updateVisualNumber(roll, value));
    }

    private void buildCameraControls(EntityRideProfile profile, int[] row) {
        addNumberRow("gui.tl_domesticate_more_creatures.riding.camera_vertical", profile.visual().cameraVerticalOffset(), row,
                value -> updateVisualNumber(VisualNumber.CAMERA_VERTICAL, value));
        addNumberRow("gui.tl_domesticate_more_creatures.riding.camera_backward", profile.visual().cameraBackwardOffset(), row,
                value -> updateVisualNumber(VisualNumber.CAMERA_BACKWARD, value));
    }

    private void addButtonRow(String labelKey, Component buttonText, int[] row, Runnable action) {
        int y = rowY(row[0]++);
        rowLabels.add(new RowLabel(Component.translatable(labelKey), controlX(), y + 6));
        if (!rowVisible(y)) {
            return;
        }
        addRenderableWidget(TdmcButton.create(
                controlButtonX(), y, controlButtonWidth(), 20,
                buttonText,
                button -> {
                    action.run();
                    rebuildEditorWidgets();
                }
        ));
    }

    private void addInfoRow(String labelKey, Component value, int[] row) {
        int y = rowY(row[0]++);
        rowLabels.add(new RowLabel(Component.translatable(labelKey), controlX(), y + 6));
        rowLabels.add(new RowLabel(value, controlButtonX(), y + 6));
    }

    private void addNumberRow(String labelKey, double value, int[] row, DoubleConsumer consumer) {
        int y = rowY(row[0]++);
        rowLabels.add(new RowLabel(Component.translatable(labelKey), controlX(), y + 6));
        if (!rowVisible(y)) {
            return;
        }
        int totalWidth = controlButtonWidth();
        int fieldWidth = Math.min(68, Math.max(42, totalWidth / 3));
        int sliderWidth = Math.max(20, totalWidth - fieldWidth - 4);
        int sliderX = controlButtonX();
        int fieldX = sliderX + sliderWidth + 4;
        NumberRange range = numberRange(labelKey);
        boolean[] syncing = {false};
        NumericSlider[] sliderRef = new NumericSlider[1];

        TdmcEditBox field = new TdmcEditBox(font, fieldX, y, fieldWidth, 20, Component.translatable(labelKey));
        field.setMaxLength(18);
        field.setValue(formatNumber(value));
        field.setResponder(text -> {
            if (syncing[0]) {
                return;
            }
            try {
                if (!text.isBlank() && !"-".equals(text) && !".".equals(text) && !"-.".equals(text)) {
                    double parsed = Double.parseDouble(text);
                    consumer.accept(parsed);
                    if (sliderRef[0] != null) {
                        sliderRef[0].syncFromActual(parsed);
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        });

        NumericSlider slider = new NumericSlider(sliderX, y, sliderWidth, 20, range.min(), range.max(), value, actual -> {
            syncing[0] = true;
            field.setValue(formatNumber(actual));
            syncing[0] = false;
            consumer.accept(actual);
        });
        sliderRef[0] = slider;
        addRenderableWidget(slider);
        addRenderableWidget(field);
    }

    private static NumberRange numberRange(String labelKey) {
        if (labelKey.endsWith("ground_speed") || labelKey.endsWith("flight_speed") || labelKey.endsWith("swim_speed")) {
            return new NumberRange(0.05D, 8.0D);
        }
        if (labelKey.endsWith("turn_rate")) {
            return new NumberRange(0.1D, 180.0D);
        }
        if (labelKey.endsWith("acceleration") || labelKey.endsWith("deceleration")) {
            return new NumberRange(0.001D, 4.0D);
        }
        if (labelKey.endsWith("jump_strength") || labelKey.endsWith("ascent_speed") || labelKey.endsWith("descent_speed")) {
            return new NumberRange(0.0D, 4.0D);
        }
        if (labelKey.endsWith("lateral_ratio") || labelKey.endsWith("vertical_ratio") || labelKey.endsWith("forward_ratio")) {
            return new NumberRange(-2.0D, 2.0D);
        }
        if (labelKey.endsWith("player_scale")) {
            return new NumberRange(0.25D, 4.0D);
        }
        if (labelKey.endsWith("offset_x") || labelKey.endsWith("offset_y") || labelKey.endsWith("offset_z")
                || labelKey.endsWith("camera_vertical") || labelKey.endsWith("camera_backward")) {
            return new NumberRange(-16.0D, 16.0D);
        }
        return new NumberRange(-180.0D, 180.0D);
    }

    private int rowY(int row) {
        return CONTROL_TOP + row * CONTROL_ROW_HEIGHT - controlScroll;
    }

    private boolean rowVisible(int y) {
        return y >= CONTROL_TOP - 1 && y <= height - 28;
    }

    private int controlX() {
        RiderPreviewRenderer.PreviewViewport viewport = previewViewport();
        return viewport.x() + viewport.width() + 12;
    }

    private int controlButtonX() {
        return controlX() + Math.min(118, Math.max(74, (width - controlX()) / 2));
    }

    private int controlButtonWidth() {
        return Math.max(70, width - controlButtonX() - 12);
    }

    private void toggleFilterEntry(ResourceLocation entityId) {
        model.updateSettings(settings -> {
            Set<ResourceLocation> entries = new LinkedHashSet<>(settings.filterEntries());
            if (!entries.remove(entityId)) {
                entries.add(entityId);
            }
            return new RidingSettings(settings.filterMode(), entries);
        });
    }

    private void updateProfileNumber(ProfileNumber field, double number) {
        model.update(profile -> withProfileNumber(profile, field, number));
    }

    private void updateVisualNumber(VisualNumber field, double number) {
        model.update(profile -> withVisualNumber(profile, field, number));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        TdmcUiTheme.fillPanel(graphics, 4, 32, LEFT_WIDTH - 4, height - 36);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        renderEntityList(graphics, mouseX, mouseY);
        renderEditorShell(graphics, partialTick);
        for (RowLabel label : rowLabels) {
            if (label.y() >= CONTROL_TOP && label.y() < height - 12) {
                graphics.drawString(font, label.text(), label.x(), label.y(), 0xFFFFFF, false);
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderEntityList(GuiGraphics graphics, int mouseX, int mouseY) {
        int y1 = height - 8;
        graphics.enableScissor(6, LIST_TOP, LEFT_WIDTH - 4, y1);
        int first = Math.max(0, listScroll / ROW_HEIGHT);
        int last = Math.min(visibleIds.size(), first + (y1 - LIST_TOP) / ROW_HEIGHT + 2);
        for (int i = first; i < last; i++) {
            ResourceLocation id = visibleIds.get(i);
            int y = LIST_TOP + i * ROW_HEIGHT - listScroll;
            boolean selected = id.equals(model.selected());
            boolean modified = model.isDirty(id) || ClientRidingConfigCache.modifiedProfiles().containsKey(id);
            int background = selected ? TdmcUiTheme.ROW_SELECTED : modified ? TdmcUiTheme.ROW_MODIFIED : TdmcUiTheme.ROW_BACKGROUND;
            if (mouseX >= 8 && mouseX < LEFT_WIDTH - 8 && mouseY >= y && mouseY < y + ROW_HEIGHT - 1) {
                background = TdmcUiTheme.ROW_HOVERED;
            }
            graphics.fill(8, y, LEFT_WIDTH - 8, y + ROW_HEIGHT - 1, background);
            graphics.drawString(font, entityDisplayName(id), 12, y + 4, 0xFFFFFF, false);
            graphics.drawString(font, id.toString(), 12, y + 16, 0xB8C7D9, false);
        }
        graphics.disableScissor();
    }

    private void renderEditorShell(GuiGraphics graphics, float partialTick) {
        int x = LEFT_WIDTH + 8;
        int y = 60;
        TdmcUiTheme.fillPanel(graphics, x, y, width - 8 - x, height - 8 - y);
        EntityRideProfile profile = model.workingProfile();
        if (profile == null) {
            graphics.drawString(font, Component.translatable("gui.tl_domesticate_more_creatures.riding.no_selection"), x + 10, y + 10, 0xFFFFFF, false);
            return;
        }
        Component displayName = entityDisplayName(profile.entityId());
        graphics.drawString(font, displayName, x + 10, y + 8, 0xFFFFFF, false);
        graphics.drawString(font, profile.entityId().toString(), x + 10, y + 20, 0xB8C7D9, false);
        if (model.dirty()) {
            Component modified = Component.translatable("gui.tl_domesticate_more_creatures.riding.modified");
            graphics.drawString(font, modified, width - 18 - font.width(modified), y + 8, 0xFFFFFF, false);
        }
        ensurePreview();
        RiderPreviewRenderer.PreviewViewport viewport = previewViewport();
        if (previewMount != null && previewRider != null) {
            renderPreviewSafely(graphics, previewRider, profile, viewport, partialTick);
        }
        if (previewFailed) {
            graphics.fill(viewport.x(), viewport.y(), viewport.x() + viewport.width(), viewport.y() + viewport.height(), 0xFFB8B8B8);
            graphics.drawCenteredString(font, Component.translatable("gui.tl_domesticate_more_creatures.riding.preview_failed"), viewport.x() + viewport.width() / 2, viewport.y() + viewport.height() / 2, 0xFFFFFF);
        }
    }

    private void renderPreviewSafely(
            GuiGraphics graphics,
            RemotePlayer rider,
            EntityRideProfile profile,
            RiderPreviewRenderer.PreviewViewport viewport,
            float partialTick
    ) {
        try {
            RiderPreviewRenderer.render(graphics, previewMount, rider, profile.visual(), viewport, previewYaw, previewPitch, previewZoom, partialTick);
        } catch (RuntimeException exception) {
            previewRenderFailure();
        }
    }

    private void previewRenderFailure() {
        if (previewRider != null) {
            previewRider.stopRiding();
        }
        previewRider = null;
        previewMount = null;
        previewFailed = true;
    }

    private RiderPreviewRenderer.PreviewViewport previewViewport() {
        int x = LEFT_WIDTH + 18;
        int y = 88;
        int availableWidth = Math.max(300, width - x - 18);
        int previewWidth = Math.max(140, Math.min(300, (int) (availableWidth * 0.45D)));
        int previewHeight = Math.max(140, height - y - 18);
        return new RiderPreviewRenderer.PreviewViewport(x, y, previewWidth, previewHeight);
    }

    private void ensurePreview() {
        if (minecraft == null || minecraft.level == null || minecraft.player == null || model.selected() == null) {
            if (previewRider != null) {
                previewRider.stopRiding();
            }
            previewRider = null;
            previewMount = null;
            return;
        }
        if (model.selected().equals(previewEntityId)) {
            return;
        }
        previewEntityId = model.selected();
        if (previewRider != null) {
            previewRider.stopRiding();
        }
        previewRider = null;
        previewMount = null;
        previewFailed = false;
        try {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(previewEntityId);
            if (type == null) {
                previewFailed = true;
                return;
            }
            Entity created = type.create(minecraft.level);
            if (created instanceof LivingEntity living) {
                previewMount = living;
                previewRider = RiderPreviewRenderer.createPreviewRider(living, minecraft.player);
                if (previewRider == null) {
                    previewMount = null;
                    previewFailed = true;
                    return;
                }
            } else {
                previewFailed = true;
            }
        } catch (RuntimeException exception) {
            previewFailed = true;
        }
    }

    private void invalidatePreview() {
        if (previewRider != null) {
            previewRider.stopRiding();
        }
        previewEntityId = null;
        previewRider = null;
        previewMount = null;
        previewFailed = false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && previewViewport().contains(mouseX, mouseY) && model.workingProfile() != null) {
            rotatingPreview = true;
            lastDragX = mouseX;
            lastDragY = mouseY;
            return true;
        }
        if (button == 0 && mouseX >= 8 && mouseX < LEFT_WIDTH - 8 && mouseY >= LIST_TOP && mouseY < height - 8) {
            int index = (int) ((mouseY - LIST_TOP + listScroll) / ROW_HEIGHT);
            if (index >= 0 && index < visibleIds.size()) {
                model.select(visibleIds.get(index));
                controlScroll = 0;
                invalidatePreview();
                rebuildEditorWidgets();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && rotatingPreview && previewViewport().contains(mouseX, mouseY)) {
            previewYaw += (float) (mouseX - lastDragX) * 0.8F;
            previewPitch = Mth.clamp(previewPitch + (float) (mouseY - lastDragY) * 0.6F, -80.0F, 80.0F);
            lastDragX = mouseX;
            lastDragY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && rotatingPreview) {
            rotatingPreview = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (previewViewport().contains(mouseX, mouseY)) {
            previewZoom = Mth.clamp(previewZoom + (float) delta * 0.1F, 0.35F, 3.0F);
            return true;
        }
        if (mouseX < LEFT_WIDTH) {
            int max = Math.max(0, visibleIds.size() * ROW_HEIGHT - Math.max(1, height - LIST_TOP - 10));
            listScroll = Mth.clamp(listScroll - (int) Math.signum(delta) * ROW_HEIGHT * 3, 0, max);
            return true;
        }
        if (mouseX >= controlX()) {
            int old = controlScroll;
            controlScroll = Mth.clamp(controlScroll - (int) Math.signum(delta) * CONTROL_ROW_HEIGHT * 3, 0, maxControlScroll);
            if (old != controlScroll) {
                rebuildEditorWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private static EntityRideProfile withMode(EntityRideProfile p, RideMode mode) {
        return new EntityRideProfile(p.entityId(), mode, p.movementMode(), p.groundSpeedMultiplier(), p.turnRateDegrees(), p.acceleration(), p.deceleration(), p.jumpStrength(), p.flightSpeedMultiplier(), p.ascentSpeed(), p.descentSpeed(), p.swimSpeedMultiplier(), p.autoAttackWhileRidden(), p.visualOverride(), p.visual());
    }

    private static EntityRideProfile withMovement(EntityRideProfile p, RideMovementMode movement) {
        return new EntityRideProfile(p.entityId(), p.mode(), movement, p.groundSpeedMultiplier(), p.turnRateDegrees(), p.acceleration(), p.deceleration(), p.jumpStrength(), p.flightSpeedMultiplier(), p.ascentSpeed(), p.descentSpeed(), p.swimSpeedMultiplier(), p.autoAttackWhileRidden(), p.visualOverride(), p.visual());
    }

    private static EntityRideProfile withAutoAttack(EntityRideProfile p, boolean value) {
        return new EntityRideProfile(p.entityId(), p.mode(), p.movementMode(), p.groundSpeedMultiplier(), p.turnRateDegrees(), p.acceleration(), p.deceleration(), p.jumpStrength(), p.flightSpeedMultiplier(), p.ascentSpeed(), p.descentSpeed(), p.swimSpeedMultiplier(), value, p.visualOverride(), p.visual());
    }

    private static EntityRideProfile withVisualOverride(EntityRideProfile p, boolean value) {
        return new EntityRideProfile(p.entityId(), p.mode(), p.movementMode(), p.groundSpeedMultiplier(), p.turnRateDegrees(), p.acceleration(), p.deceleration(), p.jumpStrength(), p.flightSpeedMultiplier(), p.ascentSpeed(), p.descentSpeed(), p.swimSpeedMultiplier(), p.autoAttackWhileRidden(), value, p.visual());
    }

    private static EntityRideProfile withVisual(EntityRideProfile p, RiderVisualProfile visual) {
        return new EntityRideProfile(p.entityId(), p.mode(), p.movementMode(), p.groundSpeedMultiplier(), p.turnRateDegrees(), p.acceleration(), p.deceleration(), p.jumpStrength(), p.flightSpeedMultiplier(), p.ascentSpeed(), p.descentSpeed(), p.swimSpeedMultiplier(), p.autoAttackWhileRidden(), p.visualOverride(), visual);
    }

    private static EntityRideProfile withProfileNumber(EntityRideProfile p, ProfileNumber field, double value) {
        return new EntityRideProfile(
                p.entityId(), p.mode(), p.movementMode(),
                field == ProfileNumber.GROUND_SPEED ? value : p.groundSpeedMultiplier(),
                field == ProfileNumber.TURN_RATE ? value : p.turnRateDegrees(),
                field == ProfileNumber.ACCELERATION ? value : p.acceleration(),
                field == ProfileNumber.DECELERATION ? value : p.deceleration(),
                field == ProfileNumber.JUMP ? value : p.jumpStrength(),
                field == ProfileNumber.FLIGHT_SPEED ? value : p.flightSpeedMultiplier(),
                field == ProfileNumber.ASCENT ? value : p.ascentSpeed(),
                field == ProfileNumber.DESCENT ? value : p.descentSpeed(),
                field == ProfileNumber.SWIM_SPEED ? value : p.swimSpeedMultiplier(),
                p.autoAttackWhileRidden(), p.visualOverride(), p.visual()
        );
    }

    private static EntityRideProfile withPosePreset(EntityRideProfile p, RiderPosePreset preset) {
        RiderVisualProfile v = p.visual();
        return withVisual(p, new RiderVisualProfile(v.lateralRatio(), v.verticalRatio(), v.forwardRatio(), v.offsetX(), v.offsetY(), v.offsetZ(), preset, v.bodyPitch(), v.bodyYawOffset(), v.leftLeg(), v.rightLeg(), v.leftArm(), v.rightArm(), v.visualPlayerScale(), v.cameraVerticalOffset(), v.cameraBackwardOffset()));
    }

    private static EntityRideProfile withVisualNumber(EntityRideProfile p, VisualNumber field, double value) {
        RiderVisualProfile v = p.visual();
        RiderVisualProfile.LimbRotation leftLeg = editLimb(v.leftLeg(), field, VisualNumber.LEFT_LEG_PITCH, VisualNumber.LEFT_LEG_YAW, VisualNumber.LEFT_LEG_ROLL, value);
        RiderVisualProfile.LimbRotation rightLeg = editLimb(v.rightLeg(), field, VisualNumber.RIGHT_LEG_PITCH, VisualNumber.RIGHT_LEG_YAW, VisualNumber.RIGHT_LEG_ROLL, value);
        RiderVisualProfile.LimbRotation leftArm = editLimb(v.leftArm(), field, VisualNumber.LEFT_ARM_PITCH, VisualNumber.LEFT_ARM_YAW, VisualNumber.LEFT_ARM_ROLL, value);
        RiderVisualProfile.LimbRotation rightArm = editLimb(v.rightArm(), field, VisualNumber.RIGHT_ARM_PITCH, VisualNumber.RIGHT_ARM_YAW, VisualNumber.RIGHT_ARM_ROLL, value);
        RiderVisualProfile updated = new RiderVisualProfile(
                field == VisualNumber.LATERAL_RATIO ? value : v.lateralRatio(),
                field == VisualNumber.VERTICAL_RATIO ? value : v.verticalRatio(),
                field == VisualNumber.FORWARD_RATIO ? value : v.forwardRatio(),
                field == VisualNumber.OFFSET_X ? value : v.offsetX(),
                field == VisualNumber.OFFSET_Y ? value : v.offsetY(),
                field == VisualNumber.OFFSET_Z ? value : v.offsetZ(),
                v.posePreset(),
                (float) (field == VisualNumber.BODY_PITCH ? value : v.bodyPitch()),
                (float) (field == VisualNumber.BODY_YAW ? value : v.bodyYawOffset()),
                leftLeg, rightLeg, leftArm, rightArm,
                (float) (field == VisualNumber.PLAYER_SCALE ? value : v.visualPlayerScale()),
                field == VisualNumber.CAMERA_VERTICAL ? value : v.cameraVerticalOffset(),
                field == VisualNumber.CAMERA_BACKWARD ? value : v.cameraBackwardOffset()
        ).validated();
        return withVisual(p, updated);
    }

    private static RiderVisualProfile.LimbRotation editLimb(
            RiderVisualProfile.LimbRotation current,
            VisualNumber field,
            VisualNumber pitchField,
            VisualNumber yawField,
            VisualNumber rollField,
            double value
    ) {
        return new RiderVisualProfile.LimbRotation(
                (float) (field == pitchField ? value : current.pitch()),
                (float) (field == yawField ? value : current.yaw()),
                (float) (field == rollField ? value : current.roll())
        ).validated();
    }

    private static RideMode nextMode(RideMode mode) {
        return switch (mode) {
            case AUTO -> RideMode.FORCE_GENERIC;
            case FORCE_GENERIC -> RideMode.DISABLED;
            case DISABLED -> RideMode.AUTO;
        };
    }

    private static RideMovementMode nextMovement(RideMovementMode mode) {
        return switch (mode) {
            case AUTO -> RideMovementMode.GROUND;
            case GROUND -> RideMovementMode.FLIGHT;
            case FLIGHT -> RideMovementMode.SWIM;
            case SWIM -> RideMovementMode.GROUND_SWIM;
            case GROUND_SWIM -> RideMovementMode.FLIGHT_SWIM;
            case FLIGHT_SWIM -> RideMovementMode.AUTO;
        };
    }

    private static RiderPosePreset nextPose(RiderPosePreset pose) {
        RiderPosePreset[] values = RiderPosePreset.values();
        return values[(pose.ordinal() + 1) % values.length];
    }

    private static Component modeText(RideMode mode) {
        return Component.translatable("gui.tl_domesticate_more_creatures.riding.mode." + mode.name().toLowerCase(Locale.ROOT));
    }

    private static Component movementText(RideMovementMode mode) {
        return Component.translatable("gui.tl_domesticate_more_creatures.riding.movement." + mode.name().toLowerCase(Locale.ROOT));
    }

    private static Component poseText(RiderPosePreset pose) {
        return Component.translatable("gui.tl_domesticate_more_creatures.riding.pose." + pose.name().toLowerCase(Locale.ROOT));
    }

    private static Component filterModeText(RideFilterMode mode) {
        return Component.translatable("gui.tl_domesticate_more_creatures.riding.filter_mode." + mode.name().toLowerCase(Locale.ROOT));
    }

    private static Component booleanText(boolean value) {
        return Component.translatable(value
                ? "gui.tl_domesticate_more_creatures.riding.enabled"
                : "gui.tl_domesticate_more_creatures.riding.disabled");
    }

    private static String formatNumber(double value) {
        String text = String.format(Locale.ROOT, "%.3f", value);
        while (text.contains(".") && text.endsWith("0")) text = text.substring(0, text.length() - 1);
        if (text.endsWith(".")) text = text.substring(0, text.length() - 1);
        return text;
    }

    private enum EditorTab {
        RIDE("gui.tl_domesticate_more_creatures.riding.tab.ride"),
        SEAT("gui.tl_domesticate_more_creatures.riding.tab.seat"),
        POSE("gui.tl_domesticate_more_creatures.riding.tab.pose"),
        CAMERA("gui.tl_domesticate_more_creatures.riding.tab.camera");

        private final String key;
        EditorTab(String key) { this.key = key; }
    }

    private enum ListFilter {
        ALL("gui.tl_domesticate_more_creatures.riding.filter.all"),
        MODIFIED("gui.tl_domesticate_more_creatures.riding.filter.modified"),
        DISABLED("gui.tl_domesticate_more_creatures.riding.filter.disabled"),
        BLACKLIST("gui.tl_domesticate_more_creatures.riding.filter.blacklist"),
        WHITELIST("gui.tl_domesticate_more_creatures.riding.filter.whitelist");

        private final String key;
        ListFilter(String key) { this.key = key; }
        private ListFilter next() {
            ListFilter[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private enum ProfileNumber {
        GROUND_SPEED, TURN_RATE, ACCELERATION, DECELERATION, JUMP, FLIGHT_SPEED, ASCENT, DESCENT, SWIM_SPEED
    }

    private enum VisualNumber {
        LATERAL_RATIO, VERTICAL_RATIO, FORWARD_RATIO, OFFSET_X, OFFSET_Y, OFFSET_Z, PLAYER_SCALE,
        BODY_PITCH, BODY_YAW,
        LEFT_LEG_PITCH, LEFT_LEG_YAW, LEFT_LEG_ROLL,
        RIGHT_LEG_PITCH, RIGHT_LEG_YAW, RIGHT_LEG_ROLL,
        LEFT_ARM_PITCH, LEFT_ARM_YAW, LEFT_ARM_ROLL,
        RIGHT_ARM_PITCH, RIGHT_ARM_YAW, RIGHT_ARM_ROLL,
        CAMERA_VERTICAL, CAMERA_BACKWARD
    }

    private static final class NumericSlider extends TdmcSliderButton {
        private final double min;
        private final double max;
        private final DoubleConsumer onChange;

        private NumericSlider(int x, int y, int width, int height, double min, double max, double actual, DoubleConsumer onChange) {
            super(x, y, width, height, Component.empty(), normalize(actual, min, max));
            this.min = min;
            this.max = max;
            this.onChange = onChange;
            updateMessage();
        }

        private void syncFromActual(double actual) {
            this.value = normalize(actual, min, max);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(formatNumber(actualValue())));
        }

        @Override
        protected void applyValue() {
            onChange.accept(actualValue());
        }

        private double actualValue() {
            return min + (max - min) * value;
        }

        private static double normalize(double actual, double min, double max) {
            if (max <= min) {
                return 0.0D;
            }
            return Mth.clamp((actual - min) / (max - min), 0.0D, 1.0D);
        }
    }

    private record NumberRange(double min, double max) {
    }

    private record RowLabel(Component text, int x, int y) {
    }
}
