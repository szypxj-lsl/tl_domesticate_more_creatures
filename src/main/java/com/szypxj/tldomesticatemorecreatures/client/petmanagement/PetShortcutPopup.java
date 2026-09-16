package com.szypxj.tldomesticatemorecreatures.client.petmanagement;

import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcUiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.UUID;

public final class PetShortcutPopup {
    private static final int CELL = 24;
    private static final int PADDING = 4;
    private final UUID petUuid;
    private final int x;
    private final int y;

    public PetShortcutPopup(UUID petUuid, int x, int y) {
        this.petUuid = petUuid;
        this.x = x;
        this.y = y;
    }

    public UUID petUuid() {
        return petUuid;
    }

    public int slotAt(double mouseX, double mouseY) {
        int localX = (int) mouseX - x - PADDING;
        int localY = (int) mouseY - y - PADDING;
        if (localX < 0 || localY < 0) return -1;
        int col = localX / CELL;
        int row = localY / CELL;
        if (col < 0 || col >= 3 || row < 0 || row >= 3) return -1;
        if (localX % CELL >= CELL - 2 || localY % CELL >= CELL - 2) return -1;
        return row * 3 + col + 1;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + PADDING * 2 + CELL * 3
                && mouseY >= y && mouseY < y + PADDING * 2 + CELL * 3 + 18;
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        int width = PADDING * 2 + CELL * 3;
        int height = PADDING * 2 + CELL * 3 + 18;
        TdmcUiTheme.fillPanel(graphics, x, y, width, height);
        for (int slot = 1; slot <= 9; slot++) {
            int index = slot - 1;
            int cellX = x + PADDING + (index % 3) * CELL;
            int cellY = y + PADDING + (index / 3) * CELL;
            boolean hovered = mouseX >= cellX && mouseX < cellX + CELL - 2
                    && mouseY >= cellY && mouseY < cellY + CELL - 2;
            graphics.fill(cellX, cellY, cellX + CELL - 2, cellY + CELL - 2,
                    hovered ? TdmcUiTheme.ROW_HOVERED : TdmcUiTheme.ROW_BACKGROUND);
            TdmcUiTheme.outline(graphics, cellX, cellY, CELL - 2, CELL - 2,
                    hovered ? TdmcUiTheme.BORDER_HOVERED : TdmcUiTheme.BORDER);
            graphics.drawCenteredString(font, Integer.toString(slot), cellX + (CELL - 2) / 2, cellY + 7, TdmcUiTheme.TEXT_PRIMARY);
        }
        graphics.drawCenteredString(font, net.minecraft.network.chat.Component.translatable(
                "gui.tl_domesticate_more_creatures.pet_management.shortcut.clear"),
                x + width / 2, y + height - 14, TdmcUiTheme.TEXT_MUTED);
    }

    public boolean isClearArea(double mouseX, double mouseY) {
        int width = PADDING * 2 + CELL * 3;
        int height = PADDING * 2 + CELL * 3 + 18;
        return mouseX >= x + 4 && mouseX < x + width - 4
                && mouseY >= y + height - 18 && mouseY < y + height;
    }
}
