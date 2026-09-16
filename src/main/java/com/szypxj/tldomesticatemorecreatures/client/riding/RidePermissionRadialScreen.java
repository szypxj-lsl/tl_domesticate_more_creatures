package com.szypxj.tldomesticatemorecreatures.client.riding;

import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class RidePermissionRadialScreen extends Screen {
    private static final int ACTION_ENABLE = 0;
    private static final int ACTION_DISABLE = 1;
    private static final int ACTION_COUNT = 2;

    private final int targetEntityId;
    private int highlightedIndex = -1;
    private int centerX;
    private int centerY;
    private boolean closed;

    public RidePermissionRadialScreen(int targetEntityId) {
        super(Component.translatable("gui.tl_domesticate_more_creatures.riding.permission_title"));
        this.targetEntityId = targetEntityId;
    }

    @Override
    protected void init() {
        super.init();
        centerX = width / 2;
        centerY = height / 2;
        centerCursor();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (closed) {
            return;
        }
        updateSelection(mouseX, mouseY);
        RideActionRadialRenderer.draw(
                graphics.pose(), centerX, centerY,
                RideActionRadialRenderer.DEFAULT_RADIUS,
                RideActionRadialRenderer.DEFAULT_DEAD_ZONE,
                ACTION_COUNT, highlightedIndex
        );
        renderLabels(graphics);
    }

    private void updateSelection(int mouseX, int mouseY) {
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.hypot(dx, dy);
        float innerRadius = RideActionRadialRenderer.innerRadius(
                RideActionRadialRenderer.DEFAULT_RADIUS,
                RideActionRadialRenderer.DEFAULT_DEAD_ZONE
        );
        if (distance < innerRadius) {
            highlightedIndex = -1;
            return;
        }
        double angle = Math.atan2(dy, dx) + Math.PI / 2.0D;
        if (angle < 0.0D) {
            angle += Math.PI * 2.0D;
        }
        if (angle >= Math.PI * 2.0D) {
            angle -= Math.PI * 2.0D;
        }
        double sectorAngle = Math.PI * 2.0D / ACTION_COUNT;
        highlightedIndex = Math.min((int) (angle / sectorAngle), ACTION_COUNT - 1);
    }

    private void renderLabels(GuiGraphics graphics) {
        float textRadius = RideActionRadialRenderer.labelRadius(
                RideActionRadialRenderer.DEFAULT_RADIUS,
                RideActionRadialRenderer.DEFAULT_DEAD_ZONE
        );
        for (int i = 0; i < ACTION_COUNT; i++) {
            double middle = (i + 0.5D) * (Math.PI * 2.0D / ACTION_COUNT) - Math.PI / 2.0D;
            int x = (int) (centerX + textRadius * Math.cos(middle));
            int y = (int) (centerY + textRadius * Math.sin(middle));
            String key = i == ACTION_ENABLE
                    ? "gui.tl_domesticate_more_creatures.riding.permission_enable"
                    : "gui.tl_domesticate_more_creatures.riding.permission_disable";
            graphics.drawCenteredString(font, Component.translatable(key), x, y - font.lineHeight / 2, 0xFFFFFFFF);
        }
        graphics.drawCenteredString(
                font,
                title,
                centerX,
                centerY - RideActionRadialRenderer.DEFAULT_RADIUS - 18,
                0xFFFFFFFF
        );
    }

    private void applySelection() {
        if (closed || minecraft == null || highlightedIndex < 0) {
            return;
        }
        if (highlightedIndex == ACTION_ENABLE) {
            NetworkHandler.setRidePermission(targetEntityId, true);
        } else if (highlightedIndex == ACTION_DISABLE) {
            NetworkHandler.setRidePermission(targetEntityId, false);
        } else {
            return;
        }
        closed = true;
        minecraft.setScreen(new RideActionRadialScreen(targetEntityId, true));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !closed) {
            updateSelection((int) mouseX, (int) mouseY);
            applySelection();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closed = true;
            if (minecraft != null) {
                minecraft.setScreen(new RideActionRadialScreen(targetEntityId, true));
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void centerCursor() {
        if (minecraft == null) {
            return;
        }
        GLFW.glfwSetCursorPos(
                minecraft.getWindow().getWindow(),
                minecraft.getWindow().getScreenWidth() / 2.0D,
                minecraft.getWindow().getScreenHeight() / 2.0D
        );
    }
}
