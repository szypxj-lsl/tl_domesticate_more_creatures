package com.szypxj.tldomesticatemorecreatures.client;

import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcUiTheme;
import com.szypxj.tldomesticatemorecreatures.network.TargetHudSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;

public final class TargetHudRenderer {
    private static final int PANEL_WIDTH = 168;
    private static final int PADDING = 8;
    private static final int LINE_HEIGHT = 10;
    private static final int BAR_HEIGHT = 5;
    private static final int HEALTH_COLOR = 0xFFD94A55;
    private static final int TORPOR_COLOR = 0xFFB45DE3;

    private TargetHudRenderer() {
    }

    public static int activeTalentAnchorY(Minecraft minecraft) {
        if (minecraft == null) return 0;
        return minecraft.getWindow().getGuiScaledHeight() / 2 + 7;
    }

    public static void render(GuiGraphics graphics, TargetHudSnapshot snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int top = minecraft.getWindow().getGuiScaledHeight() / 2 + 18;
        if (snapshot.tamed()) {
            renderPetActions(graphics, minecraft, centerX, top);
        } else {
            renderWildStatus(graphics, minecraft, snapshot, centerX, top);
        }
    }

    private static void renderPetActions(GuiGraphics graphics, Minecraft minecraft, int centerX, int top) {
        int height = 48;
        int left = centerX - PANEL_WIDTH / 2;
        TdmcUiTheme.fillPanel(graphics, left, top, PANEL_WIDTH, height);

        Component inventory = Component.translatable(
                "gui.tl_domesticate_more_creatures.target_hud.pet_inventory",
                minecraft.options.keyInventory.getTranslatedKeyMessage()
        );
        Component moreOptions = Component.translatable(
                "gui.tl_domesticate_more_creatures.target_hud.pet_more_options"
        );
        Component ride = Component.translatable(
                "gui.tl_domesticate_more_creatures.target_hud.pet_ride",
                ClientKeys.RIDE.getTranslatedKeyMessage()
        );

        graphics.drawCenteredString(minecraft.font, inventory, centerX, top + 6, 0xFFFFFF);
        graphics.drawCenteredString(minecraft.font, moreOptions, centerX, top + 17, TdmcUiTheme.TEXT_MUTED);
        int separatorY = top + 29;
        graphics.fill(left + PADDING, separatorY, left + PANEL_WIDTH - PADDING, separatorY + 1, TdmcUiTheme.BORDER);
        graphics.drawCenteredString(minecraft.font, ride, centerX, top + 35, 0xFFFFFF);
    }

    private static void renderWildStatus(GuiGraphics graphics, Minecraft minecraft, TargetHudSnapshot snapshot, int centerX, int top) {
        boolean torpor = snapshot.torporAvailable();
        boolean levelRequired = snapshot.requiredTamingLevel() > 0;
        int height = 28 + (torpor ? 30 : 0) + (levelRequired ? 20 : 0);
        int left = centerX - PANEL_WIDTH / 2;
        TdmcUiTheme.fillPanel(graphics, left, top, PANEL_WIDTH, height);

        Component health = Component.translatable(
                "gui.tl_domesticate_more_creatures.target_hud.health",
                format(snapshot.health()),
                format(snapshot.maxHealth())
        );
        graphics.drawCenteredString(minecraft.font, health, centerX, top + 5, 0xFFFFFF);
        renderBar(graphics, left + PADDING, top + 16, PANEL_WIDTH - PADDING * 2, snapshot.health(), snapshot.maxHealth(), HEALTH_COLOR);

        int nextY = top + 28;
        if (torpor) {
            graphics.fill(left + PADDING, nextY, left + PANEL_WIDTH - PADDING, nextY + 1, TdmcUiTheme.BORDER);
            Component torporText = Component.translatable(
                    "gui.tl_domesticate_more_creatures.target_hud.torpor",
                    format(snapshot.torpor()),
                    format(snapshot.maxTorpor())
            );
            graphics.drawCenteredString(minecraft.font, torporText, centerX, nextY + 7, 0xFFFFFF);
            renderBar(graphics, left + PADDING, nextY + 18, PANEL_WIDTH - PADDING * 2, snapshot.torpor(), snapshot.maxTorpor(), TORPOR_COLOR);
            nextY += 30;
        }
        if (levelRequired) {
            graphics.fill(left + PADDING, nextY, left + PANEL_WIDTH - PADDING, nextY + 1, TdmcUiTheme.BORDER);
            Component requirement = Component.translatable(
                    "gui.tl_domesticate_more_creatures.taming.required_player_level",
                    snapshot.requiredTamingLevel()
            );
            graphics.drawCenteredString(minecraft.font, requirement, centerX, nextY + 7, 0xFFFFFF);
        }
    }

    private static void renderBar(GuiGraphics graphics, int x, int y, int width, double current, double max, int color) {
        graphics.fill(x, y, x + width, y + BAR_HEIGHT, TdmcUiTheme.BAR_BACKGROUND);
        double ratio = max <= 0.0D ? 0.0D : Mth.clamp(current / max, 0.0D, 1.0D);
        int fill = (int) Math.round(width * ratio);
        if (fill > 0) {
            graphics.fill(x, y, x + fill, y + BAR_HEIGHT, color);
        }
    }

    private static String format(double value) {
        double safe = Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
        if (Math.abs(safe - Math.rint(safe)) < 0.05D) {
            return Long.toString(Math.round(safe));
        }
        return String.format(Locale.ROOT, "%.1f", safe);
    }
}
