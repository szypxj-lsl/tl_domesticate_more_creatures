package com.szypxj.tldomesticatemorecreatures.client.gui.theme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class TdmcButton extends Button {
    private TdmcButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    public static TdmcButton create(int x, int y, int width, int height, Component message, OnPress onPress) {
        return new TdmcButton(x, y, width, height, message, onPress);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean pressed = active
                && isHovered()
                && GLFW.glfwGetMouseButton(minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        TdmcUiTheme.fillControl(
                graphics,
                getX(),
                getY(),
                getWidth(),
                getHeight(),
                active,
                isHoveredOrFocused(),
                pressed
        );
        int textColor = active ? TdmcUiTheme.TEXT_PRIMARY : TdmcUiTheme.TEXT_MUTED;
        graphics.drawCenteredString(
                minecraft.font,
                getMessage(),
                getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2,
                textColor
        );
    }
}
