package com.szypxj.tldomesticatemorecreatures.client.gui.theme;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class TdmcEditBox extends EditBox {
    private final Font font;

    public TdmcEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
        this.font = font;
        setBordered(false);
        setTextColor(TdmcUiTheme.TEXT_PRIMARY);
        setTextColorUneditable(TdmcUiTheme.TEXT_MUTED);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int background = isFocused() ? TdmcUiTheme.INPUT_FOCUSED : TdmcUiTheme.INPUT_BACKGROUND;
        int border = isFocused() ? TdmcUiTheme.BORDER_HOVERED : TdmcUiTheme.BORDER;
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), background);
        TdmcUiTheme.outline(graphics, getX(), getY(), getWidth(), getHeight(), border);
        int originalY = getY();
        int textYOffset = Math.max(0, (getHeight() - font.lineHeight) / 2);
        setY(originalY + textYOffset);
        try {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
        } finally {
            setY(originalY);
        }
    }
}
