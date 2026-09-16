package com.szypxj.tldomesticatemorecreatures.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.api.client.SpyglassTitleContext;
import com.szypxj.tldomesticatemorecreatures.api.client.SpyglassTitleExtensionRegistry;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcUiTheme;
import com.szypxj.tldomesticatemorecreatures.config.ClientConfig;
import com.szypxj.tldomesticatemorecreatures.network.InspectSnapshot;
import com.szypxj.tldomesticatemorecreatures.network.TalentSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

import java.util.Locale;

public final class SpyglassPanelRenderer {
    public static final float SCALE = 0.75F;
    public static final int PANEL_WIDTH = 184;
    public static final int SCREEN_MARGIN = 8;
    private static final int DETAIL_PANEL_WIDTH = 176;
    private static final int PANEL_GAP = 4;
    private static final int STAT_ROW_HEIGHT = 20;
    private static final int RADAR_HEIGHT = 92;
    private static final int RADAR_FRAME_WIDTH = 64;
    private static final int RADAR_FRAME_HEIGHT = 50;
    private static final ResourceLocation RADAR_FRAME = ResourceLocation.tryBuild(
            TlDomesticateMoreCreatures.MOD_ID,
            "textures/gui/spyglass/radar_frame.png"
    );

    private SpyglassPanelRenderer() {
    }

    public static int panelHeight(InspectSnapshot snapshot) {
        if (snapshot == null) {
            return 0;
        }
        int mainHeight = mainPanelHeight(snapshot);
        int detailHeight = detailPanelHeight(snapshot);
        return detailHeight > 0 ? mainHeight + PANEL_GAP + detailHeight : mainHeight;
    }

    private static int mainPanelHeight(InspectSnapshot snapshot) {
        if (snapshot.unborn()) {
            if (!snapshot.geneticsAvailable()) {
                return 52;
            }
            int talentHeight = talentHeight(snapshot);
            return 94 + talentHeight + snapshot.stats().size() * STAT_ROW_HEIGHT;
        }
        if (!snapshot.tdmcAffected()) {
            return 30;
        }
        int eliteHeight = snapshot.elite() ? 12 : 0;
        int talentHeight = talentHeight(snapshot);
        int furyHeight = snapshot.fury().available() ? 20 : 0;
        int imprintHeight = snapshot.imprint().available() ? 36 : 0;
        int ridingHeight = snapshot.riding().available()
                ? (snapshot.riding().movementKey().isBlank() ? 12 : 22)
                : 0;
        int tamingHeight = snapshot.hasTamingRule()
                ? 26 + snapshot.tamingFoods().size() * 10
                + (snapshot.requiredTamingLevel() > 0 ? 10 : 0)
                + (snapshot.tamingActive() ? 20 : 0)
                : 0;
        int torporHeight = snapshot.torporAvailable() ? 20 : 0;
        int radarHeight = snapshot.radar().available() ? RADAR_HEIGHT : 0;
        return 30 + eliteHeight + talentHeight + furyHeight + imprintHeight + ridingHeight + radarHeight + tamingHeight + torporHeight;
    }

    private static int detailPanelHeight(InspectSnapshot snapshot) {
        if (!showDetailPanel(snapshot)) {
            return 0;
        }
        java.util.List<InspectSnapshot.InspectStat> detailStats = detailStats(snapshot);
        if (detailStats.isEmpty()) {
            return 0;
        }
        int ownerHeight = snapshot.ownerName().getString().isBlank() ? 0 : 12;
        return 24 + ownerHeight + detailStats.size() * STAT_ROW_HEIGHT + 8;
    }

    private static int totalPanelWidth(InspectSnapshot snapshot) {
        return Math.max(PANEL_WIDTH, showDetailPanel(snapshot) ? DETAIL_PANEL_WIDTH : 0);
    }

    public static int scaledWidth() {
        return Mth.ceil(PANEL_WIDTH * SCALE);
    }

    public static int scaledWidth(InspectSnapshot snapshot) {
        return Mth.ceil(totalPanelWidth(snapshot) * SCALE);
    }

    public static int scaledHeight(InspectSnapshot snapshot) {
        return Mth.ceil(panelHeight(snapshot) * SCALE);
    }

    public static SpyglassPanelLayout.Position configuredPosition(InspectSnapshot snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        return SpyglassPanelLayout.resolve(
                SpyglassPanelLayout.Anchor.parse(ClientConfig.SPYGLASS_PANEL_ANCHOR.get()),
                ClientConfig.SPYGLASS_PANEL_OFFSET_X.get(),
                ClientConfig.SPYGLASS_PANEL_OFFSET_Y.get(),
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(),
                scaledWidth(snapshot),
                scaledHeight(snapshot),
                SCREEN_MARGIN
        );
    }

    public static void renderConfigured(GuiGraphics graphics, InspectSnapshot snapshot) {
        SpyglassPanelLayout.Position position = configuredPosition(snapshot);
        renderAt(graphics, snapshot, position.x(), position.y());
    }

    public static void renderAt(GuiGraphics graphics, InspectSnapshot snapshot, int panelX, int panelY) {
        if (snapshot.unborn()) {
            renderUnbornAt(graphics, snapshot, panelX, panelY);
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int width = PANEL_WIDTH;
        int height = mainPanelHeight(snapshot);
        int eliteHeight = snapshot.elite() ? 12 : 0;
        int talentHeight = talentHeight(snapshot);

        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(panelX, panelY, 300.0F);
        pose.scale(SCALE, SCALE, 1.0F);
        int x = 0;
        int y = 0;

        TdmcUiTheme.fillPanel(graphics, x, y, width, height);
        renderTitle(graphics, minecraft, snapshot, x, y, width);
        if (!snapshot.tdmcAffected()) {
            if (showDetailPanel(snapshot)) {
                renderDetailPanel(graphics, minecraft, snapshot, x, y + height + PANEL_GAP, Math.max(width, DETAIL_PANEL_WIDTH));
            }
            pose.popPose();
            return;
        }

        int rowY = y + 22;
        if (snapshot.elite()) {
            graphics.drawCenteredString(
                    minecraft.font,
                    Component.translatable("spyglass.tl_domesticate_more_creatures.elite"),
                    x + width / 2,
                    rowY,
                    0xFFFFFF
            );
            rowY += eliteHeight;
        }
        if (!snapshot.talents().isEmpty()) {
            renderTalentRows(graphics, minecraft, snapshot, x + 4, rowY, width - 8);
            rowY += talentHeight;
        }
        if (snapshot.fury().available()) {
            renderFury(graphics, minecraft, snapshot, x + 8, rowY, width - 16);
            rowY += 20;
        }
        if (snapshot.imprint().available()) {
            renderImprint(graphics, minecraft, snapshot, x + 8, rowY);
            rowY += 36;
        }
        if (snapshot.riding().available()) {
            Component status = Component.translatable(snapshot.riding().statusKey()).withStyle(rideStatusColor(snapshot.riding().statusKey()));
            if (snapshot.riding().movementKey().isBlank()) {
                graphics.drawString(minecraft.font, status, x + 8, rowY, 0xFFFFFF, true);
                rowY += 12;
            } else {
                Component movement = Component.translatable(snapshot.riding().movementKey()).withStyle(ChatFormatting.GREEN);
                graphics.drawString(
                        minecraft.font,
                        Component.translatable("spyglass.tl_domesticate_more_creatures.riding_status", status),
                        x + 8,
                        rowY,
                        0xFFFFFF,
                        true
                );
                rowY += 10;
                graphics.drawString(
                        minecraft.font,
                        Component.translatable("spyglass.tl_domesticate_more_creatures.riding_movement", movement),
                        x + 8,
                        rowY,
                        0xFFFFFF,
                        true
                );
                rowY += 12;
            }
        }

        if (snapshot.radar().available()) {
            renderRadar(graphics, minecraft, snapshot.radar(), x + 8, rowY, width - 16);
            rowY += RADAR_HEIGHT;
        }

        if (snapshot.hasTamingRule()) {
            rowY += 2;
            graphics.drawString(
                    minecraft.font,
                    Component.translatable(
                            "spyglass.tl_domesticate_more_creatures.taming_method",
                            Component.translatable(snapshot.tamingMethodKey())
                    ),
                    x + 8,
                    rowY,
                    0xFFFFFF,
                    true
            );
            rowY += 12;

            if (snapshot.requiredTamingLevel() > 0) {
                graphics.drawString(
                        minecraft.font,
                        Component.translatable(
                                "gui.tl_domesticate_more_creatures.taming.required_player_level",
                                snapshot.requiredTamingLevel()
                        ),
                        x + 8,
                        rowY,
                        0xFFFFFF,
                        true
                );
                rowY += 10;
            }

            graphics.drawString(
                    minecraft.font,
                    Component.translatable("spyglass.tl_domesticate_more_creatures.taming_foods"),
                    x + 8,
                    rowY,
                    0xFFFFFF,
                    true
            );
            rowY += 10;

            for (InspectSnapshot.TamingFoodSnapshot food : snapshot.tamingFoods()) {
                MutableComponent line = Component.empty()
                        .append(Component.translatable(food.nameKey()).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" "));
                if (food.configured()) {
                    line = line.append(Component.translatable(
                            "spyglass.tl_domesticate_more_creatures.taming_food_amount",
                            Component.literal(Integer.toString(food.amount())).withStyle(ChatFormatting.YELLOW)
                    ).withStyle(ChatFormatting.YELLOW));
                } else {
                    line = line.append(Component.translatable(
                            "spyglass.tl_domesticate_more_creatures.taming_food_unconfigured"
                    ).withStyle(ChatFormatting.YELLOW));
                }
                graphics.drawString(
                        minecraft.font,
                        line,
                        x + 12,
                        rowY,
                        0xFFFFFF,
                        true
                );
                rowY += 10;
            }

        }

        if (snapshot.torporAvailable()) {
            Component torporText = Component.translatable(
                    "gui.tl_domesticate_more_creatures.target_hud.torpor",
                    number(snapshot.torpor()),
                    number(snapshot.maxTorpor())
            );
            graphics.drawString(minecraft.font, torporText, x + 8, rowY, 0xFFFFFF, true);
            rowY += 10;
            int torporX = x + 8;
            int torporWidth = width - 16;
            graphics.fill(torporX, rowY, torporX + torporWidth, rowY + 7, TdmcUiTheme.BAR_BACKGROUND);
            int torporFill = (int) Math.round((torporWidth - 2) * Mth.clamp(
                    snapshot.maxTorpor() <= 0.0D ? 0.0D : snapshot.torpor() / snapshot.maxTorpor(),
                    0.0D,
                    1.0D
            ));
            if (torporFill > 0) {
                graphics.fill(torporX + 1, rowY + 1, torporX + 1 + torporFill, rowY + 6, 0xFFE58A45);
            }
            rowY += 10;
        }

        if (snapshot.hasTamingRule() && snapshot.tamingActive()) {
            Component progressLabel = Component.translatable("spyglass.tl_domesticate_more_creatures.taming_progress");
            Component progressValue = Component.translatable(
                    "spyglass.tl_domesticate_more_creatures.taming_progress_value",
                    (int) Math.round(Mth.clamp(snapshot.tamingProgress(), 0.0D, 1.0D) * 100.0D)
            );
            graphics.drawString(minecraft.font, progressLabel, x + 8, rowY, 0xFFFFFF, true);
            graphics.drawString(
                    minecraft.font,
                    progressValue,
                    x + width - 8 - minecraft.font.width(progressValue),
                    rowY,
                    0xFFFFFF,
                    true
            );
            rowY += 10;

            int progressX = x + 8;
            int progressWidth = width - 16;
            graphics.fill(progressX, rowY, progressX + progressWidth, rowY + 7, TdmcUiTheme.BAR_BACKGROUND);
            int progressFill = (int) Math.round((progressWidth - 2) * Mth.clamp(snapshot.tamingProgress(), 0.0D, 1.0D));
            if (progressFill > 0) {
                graphics.fill(progressX + 1, rowY + 1, progressX + 1 + progressFill, rowY + 6, 0xFF63DD7D);
            }
        }

        if (showDetailPanel(snapshot)) {
            renderDetailPanel(graphics, minecraft, snapshot, x, y + height + PANEL_GAP, Math.max(width, DETAIL_PANEL_WIDTH));
        }

        pose.popPose();
    }

    private static boolean showDetailPanel(InspectSnapshot snapshot) {
        return !detailStats(snapshot).isEmpty();
    }

    private static java.util.List<InspectSnapshot.InspectStat> detailStats(InspectSnapshot snapshot) {
        if (snapshot == null || snapshot.unborn() || snapshot.stats().isEmpty()) {
            return java.util.List.of();
        }
        Player player = Minecraft.getInstance().player;
        if (!ClientEvents.isSuperSpyglassEquippedOrHeld(player)) {
            return java.util.List.of();
        }
        return snapshot.stats().stream()
                .filter(stat -> !"torpor".equals(stat.id()))
                .toList();
    }

    private static void renderTitle(GuiGraphics graphics, Minecraft minecraft, InspectSnapshot snapshot, int x, int y, int width) {
        var font = minecraft.font;
        SpyglassTitleContext context = new SpyglassTitleContext(snapshot.entityTypeId(), snapshot.tdmcAffected(), snapshot.baseStats());
        var extensions = SpyglassTitleExtensionRegistry.entries();
        Component level = snapshot.tdmcAffected()
                ? Component.translatable("spyglass.tl_domesticate_more_creatures.level_segment", snapshot.level())
                : Component.empty();
        int gap = 3;
        int total = font.width(snapshot.name());
        for (var entry : extensions) {
            int extensionWidth = Math.max(0, entry.extension().width(font, context));
            if (extensionWidth > 0) {
                total += gap + extensionWidth;
            }
        }
        if (!level.getString().isEmpty()) {
            total += gap + font.width(level);
        }
        int cursor = x + (width - total) / 2;
        graphics.drawString(font, snapshot.name(), cursor, y + 7, 0xFFFFFF, true);
        cursor += font.width(snapshot.name());
        for (var entry : extensions) {
            int extensionWidth = Math.max(0, entry.extension().width(font, context));
            if (extensionWidth <= 0) {
                continue;
            }
            cursor += gap;
            entry.extension().render(graphics, font, context, cursor, y + 7);
            cursor += extensionWidth;
        }
        if (!level.getString().isEmpty()) {
            cursor += gap;
            graphics.drawString(font, level, cursor, y + 7, 0xFFFFFF, true);
        }
    }

    private static void renderImprint(GuiGraphics graphics, Minecraft minecraft, InspectSnapshot snapshot, int x, int y) {
        var imprint = snapshot.imprint();
        Component percent = Component.literal(Integer.toString(imprint.percent()) + "%").withStyle(ChatFormatting.GREEN);
        graphics.drawString(minecraft.font, Component.translatable("spyglass.tl_domesticate_more_creatures.imprint_progress", percent), x, y, 0xFFFFFF, true);
        if (imprint.active()) {
            Component remaining = Component.literal(formatTicks(imprint.remainingTicks())).withStyle(ChatFormatting.GREEN);
            graphics.drawString(minecraft.font, Component.translatable("spyglass.tl_domesticate_more_creatures.imprint_remaining", remaining), x, y + 10, 0xFFFFFF, true);
            Component need = imprintNeedText(imprint);
            graphics.drawString(minecraft.font, need, x, y + 20, 0xFFFFFF, true);
        } else {
            Component bonus = Component.literal("+" + imprint.levelCapBonus()).withStyle(ChatFormatting.GREEN);
            graphics.drawString(minecraft.font, Component.translatable("spyglass.tl_domesticate_more_creatures.imprint_level_cap", bonus), x, y + 10, 0xFFFFFF, true);
            graphics.drawString(minecraft.font, Component.translatable(imprint.bonded()
                    ? "spyglass.tl_domesticate_more_creatures.imprint_bond_yes"
                    : "spyglass.tl_domesticate_more_creatures.imprint_bond_no"), x, y + 20, 0xFFFFFF, true);
        }
    }

    private static Component imprintNeedText(com.szypxj.tldomesticatemorecreatures.network.ImprintSnapshot imprint) {
        if (imprint.needType() == null || imprint.needType().isBlank()) {
            Component time = Component.literal(formatTicks(imprint.nextNeedTicks())).withStyle(ChatFormatting.GREEN);
            return Component.translatable("spyglass.tl_domesticate_more_creatures.imprint_next_need", time);
        }
        if ("FEED".equals(imprint.needType())) {
            Component food = imprint.foodNameKey() == null || imprint.foodNameKey().isBlank()
                    ? Component.translatable("spyglass.tl_domesticate_more_creatures.imprint_food_unknown")
                    : Component.translatable(imprint.foodNameKey()).withStyle(ChatFormatting.GOLD);
            return Component.translatable("spyglass.tl_domesticate_more_creatures.imprint_need_feed", food);
        }
        if ("WALK".equals(imprint.needType())) {
            return Component.translatable("spyglass.tl_domesticate_more_creatures.imprint_need_walk");
        }
        return Component.translatable("spyglass.tl_domesticate_more_creatures.imprint_need_pet");
    }

    private static String formatTicks(long ticks) {
        long seconds = Math.max(0L, ticks) / 20L;
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60L, seconds % 60L);
    }

    private static void renderUnbornAt(GuiGraphics graphics, InspectSnapshot snapshot, int panelX, int panelY) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = PANEL_WIDTH;
        int height = mainPanelHeight(snapshot);
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(panelX, panelY, 300.0F);
        pose.scale(SCALE, SCALE, 1.0F);

        TdmcUiTheme.fillPanel(graphics, 0, 0, width, height);
        graphics.drawCenteredString(minecraft.font, snapshot.name(), width / 2, 7, 0xFFFFFF);
        graphics.drawCenteredString(
                minecraft.font,
                Component.translatable("spyglass.tl_domesticate_more_creatures.egg_unhatched"),
                width / 2,
                20,
                0xFFFFFF
        );

        if (!snapshot.geneticsAvailable()) {
            graphics.drawCenteredString(
                    minecraft.font,
                    Component.translatable("spyglass.tl_domesticate_more_creatures.egg_no_genetics"),
                    width / 2,
                    35,
                    0xFFFFFF
            );
            pose.popPose();
            return;
        }

        int rowY = 34;
        Component expectedLevel = Component.translatable(
                "spyglass.tl_domesticate_more_creatures.egg_expected_level",
                Component.literal(Integer.toString(snapshot.level())).withStyle(ChatFormatting.GREEN)
        );
        graphics.drawCenteredString(minecraft.font, expectedLevel, width / 2, rowY, 0xFFFFFF);
        rowY += 14;

        if (!snapshot.talents().isEmpty()) {
            renderTalentRows(graphics, minecraft, snapshot, 4, rowY, width - 8);
            rowY += talentHeight(snapshot);
        }

        int referenceMaxPoints = 1;
        for (InspectSnapshot.InspectStat stat : snapshot.stats()) {
            referenceMaxPoints = Math.max(referenceMaxPoints, Math.max(1, stat.points()));
        }
        for (InspectSnapshot.InspectStat stat : snapshot.stats()) {
            graphics.fill(4, rowY, width - 4, rowY + 17, TdmcUiTheme.ROW_BACKGROUND);
            Component statName = Component.translatable(stat.nameKey());
            graphics.drawString(minecraft.font, statName, 8, rowY + 2, 0xFFFFFF, true);
            Component pointValue = Component.literal(Integer.toString(stat.points())).withStyle(ChatFormatting.GREEN);
            graphics.drawString(minecraft.font, pointValue, width - 8 - minecraft.font.width(pointValue), rowY + 2, 0xFFFFFF, true);

            int barX = 8;
            int barWidth = width - 16;
            int barY = rowY + 11;
            graphics.fill(barX, barY, barX + barWidth, barY + 5, TdmcUiTheme.BAR_BACKGROUND);
            int fill = (int) Math.round((barWidth - 2) * Mth.clamp((double) stat.points() / referenceMaxPoints, 0.0D, 1.0D));
            if (fill > 0) {
                graphics.fill(barX + 1, barY + 1, barX + 1 + fill, barY + 4, fillColor(stat.id()));
            }
            rowY += STAT_ROW_HEIGHT;
        }

        rowY += 2;
        drawGeneticMetaLine(
                graphics,
                minecraft,
                "spyglass.tl_domesticate_more_creatures.egg_paternal_mutations",
                Component.literal(Integer.toString(snapshot.paternalMutations())).withStyle(ChatFormatting.GREEN),
                rowY
        );
        rowY += 10;
        drawGeneticMetaLine(
                graphics,
                minecraft,
                "spyglass.tl_domesticate_more_creatures.egg_maternal_mutations",
                Component.literal(Integer.toString(snapshot.maternalMutations())).withStyle(ChatFormatting.GREEN),
                rowY
        );
        rowY += 10;
        drawGeneticMetaLine(
                graphics,
                minecraft,
                "spyglass.tl_domesticate_more_creatures.egg_mutation_result",
                Component.translatable(snapshot.mutated()
                        ? "spyglass.tl_domesticate_more_creatures.egg_yes_mutation"
                        : "spyglass.tl_domesticate_more_creatures.egg_no_mutation")
                        .withStyle(snapshot.mutated() ? ChatFormatting.GOLD : ChatFormatting.WHITE),
                rowY
        );
        rowY += 10;
        drawGeneticMetaLine(
                graphics,
                minecraft,
                "spyglass.tl_domesticate_more_creatures.egg_owner_inherited",
                Component.translatable(snapshot.inheritedOwner()
                        ? "spyglass.tl_domesticate_more_creatures.egg_yes"
                        : "spyglass.tl_domesticate_more_creatures.egg_no")
                        .withStyle(snapshot.inheritedOwner() ? ChatFormatting.GREEN : ChatFormatting.WHITE),
                rowY
        );

        pose.popPose();
    }

    private static void renderDetailPanel(
            GuiGraphics graphics,
            Minecraft minecraft,
            InspectSnapshot snapshot,
            int x,
            int y,
            int width
    ) {
        int height = detailPanelHeight(snapshot);
        TdmcUiTheme.fillPanel(graphics, x, y, width, height);
        graphics.drawCenteredString(
                minecraft.font,
                Component.translatable("spyglass.tl_domesticate_more_creatures.detail_panel"),
                x + width / 2,
                y + 7,
                0xFFFFFF
        );

        int rowY = y + 22;
        if (!snapshot.ownerName().getString().isBlank()) {
            graphics.drawString(
                    minecraft.font,
                    Component.translatable("spyglass.tl_domesticate_more_creatures.detail_owner", snapshot.ownerName()),
                    x + 8,
                    rowY,
                    0xFFFFFF,
                    true
            );
            rowY += 12;
        }

        java.util.List<InspectSnapshot.InspectStat> detailStats = detailStats(snapshot);
        for (InspectSnapshot.InspectStat stat : detailStats) {
            graphics.fill(x + 4, rowY, x + width - 4, rowY + 17, TdmcUiTheme.ROW_BACKGROUND);
            Component statName = Component.translatable(stat.nameKey());
            Component value = valueText(stat);
            boolean showBar = shouldRenderStatBar(stat);
            int valueY = showBar ? rowY + 2 : rowY + 6;
            graphics.drawString(minecraft.font, statName, x + 8, rowY + 2, 0xFFFFFF, true);
            graphics.drawString(
                    minecraft.font,
                    value,
                    x + width - 8 - minecraft.font.width(value),
                    valueY,
                    0xFFFFFF,
                    true
            );

            if (showBar) {
                int barX = x + 8;
                int barWidth = width - 16;
                int barY = rowY + 11;
                graphics.fill(barX, barY, barX + barWidth, barY + 5, TdmcUiTheme.BAR_BACKGROUND);
                int fill = (int) Math.round((barWidth - 2) * barRatio(stat));
                if (fill > 0) {
                    graphics.fill(barX + 1, barY + 1, barX + 1 + fill, barY + 4, fillColor(stat.id()));
                }
            }
            rowY += STAT_ROW_HEIGHT;
        }
    }

    private static void renderRadar(
            GuiGraphics graphics,
            Minecraft minecraft,
            InspectSnapshot.RadarSnapshot radar,
            int x,
            int y,
            int width
    ) {
        int centerX = x + width / 2;
        int centerY = y + 43;
        int radius = 31;
        int outerTopX = centerX;
        int outerTopY = centerY - radius;
        int outerLeftX = centerX - 27;
        int outerLeftY = centerY + 16;
        int outerRightX = centerX + 27;
        int outerRightY = centerY + 16;

        int powerX = axisPoint(centerX, outerTopX, radar.power());
        int powerY = axisPoint(centerY, outerTopY, radar.power());
        int lifeX = axisPoint(centerX, outerLeftX, radar.life());
        int lifeY = axisPoint(centerY, outerLeftY, radar.life());
        int speedX = axisPoint(centerX, outerRightX, radar.speed());
        int speedY = axisPoint(centerY, outerRightY, radar.speed());

        drawRadarFill(graphics.pose(), powerX, powerY, lifeX, lifeY, speedX, speedY);
        graphics.blit(
                RADAR_FRAME,
                centerX - RADAR_FRAME_WIDTH / 2,
                centerY - 31,
                0.0F,
                0.0F,
                RADAR_FRAME_WIDTH,
                RADAR_FRAME_HEIGHT,
                RADAR_FRAME_WIDTH,
                RADAR_FRAME_HEIGHT
        );
        drawRadarOutline(graphics.pose(), powerX, powerY, lifeX, lifeY, speedX, speedY);

        Component power = Component.translatable("spyglass.tl_domesticate_more_creatures.radar.power", radar.power());
        Component life = Component.translatable("spyglass.tl_domesticate_more_creatures.radar.life", radar.life());
        Component speed = Component.translatable("spyglass.tl_domesticate_more_creatures.radar.speed", radar.speed());
        graphics.drawCenteredString(minecraft.font, power, centerX, y + 1, 0xFFFFFF);
        graphics.drawString(minecraft.font, life, x, y + 70, 0xFFFFFF, true);
        graphics.drawString(minecraft.font, speed, x + width - minecraft.font.width(speed), y + 70, 0xFFFFFF, true);
    }

    private static int axisPoint(int center, int edge, int score) {
        double ratio = Mth.clamp(score / 100.0D, 0.0D, 1.0D);
        return (int) Math.round(center + (edge - center) * ratio);
    }

    private static void drawRadarFill(
            PoseStack poseStack,
            float x1,
            float y1,
            float x2,
            float y2,
            float x3,
            float y3
    ) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(matrix, x1, y1, 0.0F).color(0.278F, 0.710F, 1.0F, 0.40F).endVertex();
        builder.vertex(matrix, x2, y2, 0.0F).color(0.278F, 0.710F, 1.0F, 0.40F).endVertex();
        builder.vertex(matrix, x3, y3, 0.0F).color(0.278F, 0.710F, 1.0F, 0.40F).endVertex();
        tesselator.end();
    }

    private static void drawRadarOutline(
            PoseStack poseStack,
            float x1,
            float y1,
            float x2,
            float y2,
            float x3,
            float y3
    ) {
        RenderSystem.lineWidth(1.0F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        builder.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(matrix, x1, y1, 0.0F).color(0.498F, 0.859F, 1.0F, 1.0F).endVertex();
        builder.vertex(matrix, x2, y2, 0.0F).color(0.498F, 0.859F, 1.0F, 1.0F).endVertex();
        builder.vertex(matrix, x3, y3, 0.0F).color(0.498F, 0.859F, 1.0F, 1.0F).endVertex();
        builder.vertex(matrix, x1, y1, 0.0F).color(0.498F, 0.859F, 1.0F, 1.0F).endVertex();
        tesselator.end();
    }

    private static void drawGeneticMetaLine(
            GuiGraphics graphics,
            Minecraft minecraft,
            String key,
            Component value,
            int y
    ) {
        Component line = Component.translatable(key, value);
        graphics.drawString(minecraft.font, line, 8, y, 0xFFFFFF, true);
    }

    private static void renderTalentRows(
            GuiGraphics graphics,
            Minecraft minecraft,
            InspectSnapshot snapshot,
            int x,
            int y,
            int width
    ) {
        java.util.List<TalentSnapshot> normal = snapshot.talents().stream().filter(talent -> !talent.special()).toList();
        java.util.List<TalentSnapshot> special = snapshot.talents().stream().filter(TalentSnapshot::special).toList();
        if (!normal.isEmpty()) {
            renderTalentRow(graphics, minecraft, normal, x, y, width);
            y += 18;
        }
        if (!special.isEmpty()) {
            renderTalentRow(graphics, minecraft, special, x, y, width);
        }
    }

    private static void renderTalentRow(
            GuiGraphics graphics,
            Minecraft minecraft,
            java.util.List<TalentSnapshot> talents,
            int x,
            int y,
            int width
    ) {
        int slotWidth = 52;
        int slotHeight = 14;
        int gap = 4;
        int count = talents.size();
        int totalWidth = count * slotWidth + (count - 1) * gap;
        int startX = x + Math.max(0, (width - totalWidth) / 2);

        for (int i = 0; i < count; i++) {
            TalentSnapshot talent = talents.get(i);
            int slotX = startX + i * (slotWidth + gap);
            int background = talent.special() ? 0xFFB32626 : talentBackground(talent.level());
            int textColor = talent.special() ? 0xFFFFFFFF : talentTextColor(talent.level());

            graphics.fill(slotX, y, slotX + slotWidth, y + slotHeight, background);

            Component name = talentName(talent);
            String fittedText = minecraft.font.plainSubstrByWidth(name.getString(), slotWidth - 4);
            Component visibleName = Component.literal(fittedText).withStyle(ChatFormatting.BOLD);
            int textX = slotX + (slotWidth - minecraft.font.width(visibleName)) / 2;

            graphics.drawString(minecraft.font, visibleName, textX, y + 3, textColor, false);
        }
    }

    private static void renderFury(
            GuiGraphics graphics,
            Minecraft minecraft,
            InspectSnapshot snapshot,
            int x,
            int y,
            int width
    ) {
        Component current = Component.literal(number(snapshot.fury().anger())).withStyle(ChatFormatting.WHITE);
        Component max = Component.literal(number(snapshot.fury().maxAnger())).withStyle(ChatFormatting.WHITE);
        Component label = Component.translatable(
                snapshot.fury().berserk()
                        ? "spyglass.tl_domesticate_more_creatures.fury_berserk"
                        : "spyglass.tl_domesticate_more_creatures.fury_anger",
                current,
                max
        ).withStyle(snapshot.fury().berserk() ? ChatFormatting.RED : ChatFormatting.WHITE);
        graphics.drawString(minecraft.font, label, x, y, 0xFFFFFF, true);
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

    private static int talentHeight(InspectSnapshot snapshot) {
        boolean normal = false;
        boolean special = false;
        for (TalentSnapshot talent : snapshot.talents()) {
            if (talent.special()) {
                special = true;
            } else {
                normal = true;
            }
        }
        return ((normal ? 1 : 0) + (special ? 1 : 0)) * 18;
    }

    private static ChatFormatting rideStatusColor(String statusKey) {
        if ("gui.tl_domesticate_more_creatures.riding.status.disabled".equals(statusKey)) {
            return ChatFormatting.RED;
        }
        if ("gui.tl_domesticate_more_creatures.riding.status.native".equals(statusKey)) {
            return ChatFormatting.AQUA;
        }
        return ChatFormatting.GREEN;
    }

    private static Component valueText(InspectSnapshot.InspectStat stat) {
        if (shouldRenderStatBar(stat)) {
            return Component.literal(number(stat.currentValue()) + "/" + number(stat.maxValue()));
        }
        return formatValue(stat.displayedValue(), stat.displayFormat());
    }

    private static Component formatValue(double value, String displayFormat) {
        if ("MULTIPLIER".equals(displayFormat)) {
            return Component.translatable("gui.tl_domesticate_more_creatures.value_multiplier", String.format(Locale.ROOT, "%.2f", value));
        }
        if ("PERCENT".equals(displayFormat)) {
            return Component.translatable("gui.tl_domesticate_more_creatures.value_percent", String.format(Locale.ROOT, "%.1f", value * 100.0D));
        }
        return Component.literal(number(value));
    }

    private static String number(double value) {
        return Math.abs(value - Math.rint(value)) < 0.0001D
                ? Long.toString(Math.round(value))
                : String.format(Locale.ROOT, "%.2f", value);
    }

    private static boolean shouldRenderStatBar(InspectSnapshot.InspectStat stat) {
        return stat.maxValue() > 0.0D;
    }

    private static double barRatio(InspectSnapshot.InspectStat stat) {
        if (!shouldRenderStatBar(stat)) {
            return 0.0D;
        }
        return Mth.clamp(stat.currentValue() / stat.maxValue(), 0.0D, 1.0D);
    }

    private static int fillColor(String statId) {
        if ("health".equals(statId)) {
            return 0xFFD84C4C;
        }
        if ("damage".equals(statId)) {
            return 0xFFE2C04E;
        }
        if ("speed".equals(statId)) {
            return 0xFF63DD7D;
        }
        if ("swim_speed".equals(statId)) {
            return 0xFF53C4FF;
        }
        if ("resistance".equals(statId)) {
            return 0xFFBA68FF;
        }
        if ("torpor".equals(statId)) {
            return 0xFFE58A45;
        }
        return 0xFF6AA9FF;
    }

    private static Component talentName(TalentSnapshot talent) {
        String translated = Component.translatable(talent.nameKey()).getString();
        String plain = ChatFormatting.stripFormatting(translated);
        return Component.literal(plain == null ? translated : plain).withStyle(ChatFormatting.BOLD);
    }

    private static int talentBackground(int level) {
        if (level <= 1) {
            return 0xFFD8D8D8;
        }
        if (level == 2) {
            return 0xFF3F7FD9;
        }
        return 0xFFD6A62A;
    }

    private static int talentTextColor(int level) {
        return level == 2 ? 0xFFFFFFFF : 0xFF202020;
    }
}
