package com.szypxj.tldomesticatemorecreatures.client.petmanagement;

import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcUiTheme;
import com.szypxj.tldomesticatemorecreatures.client.riding.RideActionKeyHandler;
import com.szypxj.tldomesticatemorecreatures.client.riding.RideActionRadialRenderer;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.network.PetManagementSummary;
import com.szypxj.tldomesticatemorecreatures.petmanagement.PetRecordState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class PetShortcutRadialScreen extends Screen {
    private static final int ACTION_COUNT = 9;
    private static final int RADIUS = 92;
    private static final int DEAD_ZONE = 18;
    private int highlightedIndex = -1;
    private int centerX;
    private int centerY;
    private boolean closed;
    private boolean cancelled;

    public PetShortcutRadialScreen() {
        super(Component.translatable("gui.tl_domesticate_more_creatures.pet_management.radial.title"));
    }

    @Override
    protected void init() {
        centerX = width / 2;
        centerY = height / 2;
        centerCursor();
        NetworkHandler.requestPetManagementList();
    }

    @Override
    public void tick() {
        if (!closed && !RideActionKeyHandler.isActionKeyPhysicallyDown()) finishSelection();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (closed) return;
        updateSelection(mouseX, mouseY);
        RideActionRadialRenderer.draw(graphics.pose(), centerX, centerY, RADIUS, DEAD_ZONE, ACTION_COUNT, highlightedIndex);
        float labelRadius = RideActionRadialRenderer.labelRadius(RADIUS, DEAD_ZONE);
        for (int i = 0; i < ACTION_COUNT; i++) {
            int slot = i + 1;
            double middle = (i + 0.5D) * (Math.PI * 2.0D / ACTION_COUNT) - Math.PI / 2.0D;
            int x = (int) Math.round(centerX + labelRadius * Math.cos(middle));
            int y = (int) Math.round(centerY + labelRadius * Math.sin(middle));
            PetManagementSummary pet = shortcut(slot);
            Component first = pet == null
                    ? Component.translatable("gui.tl_domesticate_more_creatures.pet_management.radial.empty", slot)
                    : Component.literal(slot + " · " + pet.displayName());
            int color = pet != null && pet.state() == PetRecordState.DEAD ? 0xFFFF7C7C : TdmcUiTheme.TEXT_PRIMARY;
            graphics.drawCenteredString(font, first, x, y - 8, color);
            if (pet != null) {
                Component second = Component.translatable(
                        "gui.tl_domesticate_more_creatures.pet_management.radial.level_status",
                        pet.level(), statusText(pet.state()));
                graphics.drawCenteredString(font, second, x, y + 3, TdmcUiTheme.TEXT_MUTED);
            }
        }
        graphics.drawCenteredString(font, title, centerX, centerY - RADIUS - 18, TdmcUiTheme.TEXT_PRIMARY);
    }

    private void updateSelection(int mouseX, int mouseY) {
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.hypot(dx, dy);
        float innerRadius = RideActionRadialRenderer.innerRadius(RADIUS, DEAD_ZONE);
        if (distance < innerRadius) {
            highlightedIndex = -1;
            return;
        }
        double angle = Math.atan2(dy, dx) + Math.PI / 2.0D;
        if (angle < 0.0D) angle += Math.PI * 2.0D;
        if (angle >= Math.PI * 2.0D) angle -= Math.PI * 2.0D;
        highlightedIndex = Math.min((int) (angle / (Math.PI * 2.0D / ACTION_COUNT)), ACTION_COUNT - 1);
    }

    private void finishSelection() {
        if (closed) return;
        closed = true;
        if (!cancelled && highlightedIndex >= 0) {
            int slot = highlightedIndex + 1;
            PetManagementSummary pet = shortcut(slot);
            if (pet != null && pet.state() != PetRecordState.DEAD) NetworkHandler.summonPetShortcut(slot);
        }
        onClose();
    }

    private static PetManagementSummary shortcut(int slot) {
        for (PetManagementSummary pet : ClientState.petManagementList()) {
            if (pet.shortcutSlot() == slot) return pet;
        }
        return null;
    }

    private static Component statusText(PetRecordState state) {
        String suffix = switch (state) {
            case WORLD -> "world";
            case STORED -> "stored";
            case SUMMONING -> "summoning";
            case DEAD -> "dead";
            case UNLOCATED -> "unlocated";
        };
        return Component.translatable("gui.tl_domesticate_more_creatures.pet_management.status." + suffix);
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
        if (minecraft == null) return;
        GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(),
                minecraft.getWindow().getScreenWidth() / 2.0D,
                minecraft.getWindow().getScreenHeight() / 2.0D);
    }
}
