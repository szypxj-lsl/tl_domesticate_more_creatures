package com.szypxj.tldomesticatemorecreatures.client.riding;

import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import com.szypxj.tldomesticatemorecreatures.client.InventoryPanelKeyHandler;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

public final class RideActionRadialScreen extends Screen {
    private static final int ACTION_RIDE = 0;
    private static final int ACTION_PANEL = 1;
    private static final int ACTION_PERMISSION = 2;
    private static final int ACTION_RECALL = 3;
    private static final int ACTION_COUNT = 4;

    private final int targetEntityId;
    private final boolean persistentSelection;
    private int highlightedIndex = -1;
    private int centerX;
    private int centerY;
    private boolean closed;
    private boolean cancelled;

    public RideActionRadialScreen(int targetEntityId) {
        this(targetEntityId, false);
    }

    public RideActionRadialScreen(int targetEntityId, boolean persistentSelection) {
        super(Component.translatable("gui.tl_domesticate_more_creatures.riding.action_title"));
        this.targetEntityId = targetEntityId;
        this.persistentSelection = persistentSelection;
    }

    @Override
    protected void init() {
        super.init();
        centerX = width / 2;
        centerY = height / 2;
        centerCursor();
    }

    @Override
    public void tick() {
        if (!persistentSelection && !closed && !InventoryPanelKeyHandler.isInventoryKeyPhysicallyDown()) {
            finishSelection();
        }
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
            String key = switch (i) {
                case ACTION_RIDE -> "gui.tl_domesticate_more_creatures.riding.action_ride";
                case ACTION_PANEL -> "gui.tl_domesticate_more_creatures.riding.action_panel";
                case ACTION_PERMISSION -> "gui.tl_domesticate_more_creatures.riding.action_permission";
                default -> "gui.tl_domesticate_more_creatures.riding.action_recall";
            };
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

    private void finishSelection() {
        if (closed) {
            return;
        }
        closed = true;
        if (cancelled || minecraft == null) {
            onClose();
            return;
        }
        if (highlightedIndex == ACTION_RIDE) {
            NetworkHandler.requestRideMount(targetEntityId);
            onClose();
            return;
        }
        if (highlightedIndex == ACTION_PANEL) {
            ClientState.clearPanelCompanionEntityId();
            NetworkHandler.openPanel(targetEntityId);
            onClose();
            return;
        }
        if (highlightedIndex == ACTION_PERMISSION) {
            minecraft.setScreen(new RidePermissionRadialScreen(targetEntityId));
            return;
        }
        if (highlightedIndex == ACTION_RECALL) {
            if (minecraft.level != null && minecraft.level.getEntity(targetEntityId) instanceof LivingEntity living) {
                NetworkHandler.storeManagedPet(living.getUUID());
            }
            onClose();
            return;
        }
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (persistentSelection && button == 0 && !closed) {
            updateSelection((int) mouseX, (int) mouseY);
            finishSelection();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancelled = true;
            closed = true;
            onClose();
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
