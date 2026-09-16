package com.szypxj.tldomesticatemorecreatures.client.gui.theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public abstract class TdmcSliderButton extends AbstractSliderButton {
    protected TdmcSliderButton(int x, int y, int width, int height, Component message, double value) {
        super(x, y, width, height, message, value);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int track = active ? TdmcUiTheme.SLIDER_TRACK : TdmcUiTheme.BUTTON_DISABLED;
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), track);
        TdmcUiTheme.outline(
                graphics,
                getX(),
                getY(),
                getWidth(),
                getHeight(),
                isHoveredOrFocused() && active ? TdmcUiTheme.BORDER_HOVERED : TdmcUiTheme.BORDER
        );

        int thumbWidth = Math.min(8, Math.max(4, getWidth()));
        int travel = Math.max(0, getWidth() - thumbWidth - 2);
        int thumbX = getX() + 1 + (int) Math.round(value * travel);
        int thumbColor = isHoveredOrFocused() && active
                ? TdmcUiTheme.SLIDER_THUMB_HOVERED
                : TdmcUiTheme.SLIDER_THUMB;
        graphics.fill(thumbX, getY() + 2, thumbX + thumbWidth, getY() + getHeight() - 2, thumbColor);

        graphics.drawCenteredString(
                Minecraft.getInstance().font,
                getMessage(),
                getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2,
                active ? TdmcUiTheme.TEXT_PRIMARY : TdmcUiTheme.TEXT_MUTED
        );
    }
}
