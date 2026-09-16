package com.szypxj.tldomesticatemorecreatures.client.taming;

import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcButton;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcUiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

final class TamingEditorExitConfirmScreen extends Screen {
    enum Choice { SAVE, DISCARD, CANCEL }

    private final Consumer<Choice> callback;
    private boolean resolved;

    TamingEditorExitConfirmScreen(Consumer<Choice> callback) {
        super(Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.unsaved_title"));
        this.callback = callback;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(410, width - 24);
        int panelLeft = (width - panelWidth) / 2;
        int panelTop = (height - 118) / 2;
        int gap = 6;
        int buttonWidth = Math.max(84, (panelWidth - 28 - gap * 2) / 3);
        int x = panelLeft + 14;
        int y = panelTop + 78;
        addRenderableWidget(TdmcButton.create(x, y, buttonWidth, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.save_exit"),
                button -> resolve(Choice.SAVE)));
        addRenderableWidget(TdmcButton.create(x + buttonWidth + gap, y, buttonWidth, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.discard"),
                button -> resolve(Choice.DISCARD)));
        addRenderableWidget(TdmcButton.create(x + (buttonWidth + gap) * 2, y, buttonWidth, 20,
                Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.cancel"),
                button -> resolve(Choice.CANCEL)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panelWidth = Math.min(410, width - 24);
        int panelLeft = (width - panelWidth) / 2;
        int panelTop = (height - 118) / 2;
        TdmcUiTheme.fillPanel(graphics, panelLeft, panelTop, panelWidth, 118);
        TdmcUiTheme.fillHeader(graphics, panelLeft + 1, panelTop + 1, panelWidth - 2, 28);
        graphics.drawCenteredString(font, title, width / 2, panelTop + 10, TdmcUiTheme.TEXT_PRIMARY);
        graphics.drawCenteredString(font,
                Component.translatable("gui.tl_domesticate_more_creatures.taming_editor.unsaved_message"),
                width / 2, panelTop + 46, TdmcUiTheme.TEXT_PRIMARY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        resolve(Choice.CANCEL);
    }

    private void resolve(Choice choice) {
        if (resolved) {
            return;
        }
        resolved = true;
        callback.accept(choice);
    }
}
