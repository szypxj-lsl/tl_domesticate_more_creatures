package com.szypxj.tldomesticatemorecreatures.client.gui.theme;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class TdmcConfirmScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 112;

    private final Consumer<Boolean> callback;
    private final Component message;
    private final Component confirmText;
    private final Component cancelText;
    private boolean resolved;

    public TdmcConfirmScreen(
            Consumer<Boolean> callback,
            Component title,
            Component message,
            Component confirmText,
            Component cancelText
    ) {
        super(title);
        this.callback = callback;
        this.message = message;
        this.confirmText = confirmText;
        this.cancelText = cancelText;
    }

    @Override
    protected void init() {
        int panelLeft = (width - PANEL_WIDTH) / 2;
        int panelTop = (height - PANEL_HEIGHT) / 2;
        int buttonWidth = 120;
        int gap = 12;
        int totalWidth = buttonWidth * 2 + gap;
        int buttonX = panelLeft + (PANEL_WIDTH - totalWidth) / 2;
        int buttonY = panelTop + PANEL_HEIGHT - 31;

        addRenderableWidget(TdmcButton.create(
                buttonX, buttonY, buttonWidth, 20,
                confirmText,
                button -> resolve(true)
        ));
        addRenderableWidget(TdmcButton.create(
                buttonX + buttonWidth + gap, buttonY, buttonWidth, 20,
                cancelText,
                button -> resolve(false)
        ));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panelLeft = (width - PANEL_WIDTH) / 2;
        int panelTop = (height - PANEL_HEIGHT) / 2;
        TdmcUiTheme.fillPanel(graphics, panelLeft, panelTop, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.fill(
                panelLeft + 1,
                panelTop + 1,
                panelLeft + PANEL_WIDTH - 1,
                panelTop + 28,
                TdmcUiTheme.SECTION_BACKGROUND
        );
        graphics.drawCenteredString(font, title, width / 2, panelTop + 10, TdmcUiTheme.TEXT_PRIMARY);
        graphics.drawCenteredString(font, message, width / 2, panelTop + 43, TdmcUiTheme.TEXT_PRIMARY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        resolve(false);
    }

    private void resolve(boolean accepted) {
        if (resolved) {
            return;
        }
        resolved = true;
        callback.accept(accepted);
    }
}
