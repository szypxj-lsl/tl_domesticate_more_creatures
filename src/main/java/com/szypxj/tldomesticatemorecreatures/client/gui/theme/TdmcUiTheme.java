package com.szypxj.tldomesticatemorecreatures.client.gui.theme;

import net.minecraft.client.gui.GuiGraphics;

public final class TdmcUiTheme {
    public static final int ALPHA_75 = 0xBF;

    public static final int PANEL_BACKGROUND = argb(0x08, 0x11, 0x1D);
    public static final int HEADER_BACKGROUND = argb(0x10, 0x29, 0x43);
    public static final int SECTION_BACKGROUND = argb(0x0A, 0x18, 0x29);
    public static final int ROW_BACKGROUND = argb(0x10, 0x24, 0x3A);
    public static final int ROW_HOVERED = argb(0x18, 0x3D, 0x60);
    public static final int ROW_SELECTED = argb(0x1D, 0x55, 0x80);
    public static final int ROW_MODIFIED = argb(0x19, 0x3A, 0x59);

    public static final int BUTTON_BACKGROUND = argb(0x13, 0x35, 0x55);
    public static final int BUTTON_HOVERED = argb(0x1C, 0x55, 0x82);
    public static final int BUTTON_PRESSED = argb(0x24, 0x73, 0xA8);
    public static final int BUTTON_DISABLED = argb(0x20, 0x2A, 0x36);

    public static final int INPUT_BACKGROUND = argb(0x08, 0x17, 0x28);
    public static final int INPUT_FOCUSED = argb(0x10, 0x32, 0x50);
    public static final int SLOT_BACKGROUND = argb(0x08, 0x16, 0x26);
    public static final int SLOT_HOVERED = argb(0x14, 0x3B, 0x5C);
    public static final int SLIDER_TRACK = argb(0x0D, 0x26, 0x3D);
    public static final int SLIDER_THUMB = argb(0x27, 0x69, 0x9B);
    public static final int SLIDER_THUMB_HOVERED = argb(0x35, 0x8B, 0xC3);

    public static final int HUD_BACKGROUND = argb(0x08, 0x11, 0x1D);
    public static final int BAR_BACKGROUND = argb(0x05, 0x0E, 0x19);

    public static final int BORDER_DARK = 0xFF08111C;
    public static final int BORDER = 0xFF315A78;
    public static final int BORDER_HOVERED = 0xFF58B8E8;
    public static final int BORDER_SELECTED = 0xFF79D7FF;
    public static final int SLOT_BORDER = 0xFF3D6D8D;
    public static final int ACCENT = 0xFF48C8FF;
    public static final int ACCENT_SOFT = argb(0x22, 0x77, 0xA6);

    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_MUTED = 0xFFB8CAD8;
    public static final int TEXT_ACCENT = 0xFF8ADFFF;

    public static final float RADIAL_ALPHA = 0.75F;
    public static final int RADIAL_FILL = 0x102943;
    public static final int RADIAL_SELECTED = 0x2473A8;
    public static final int RADIAL_OUTLINE = 0x3D789D;
    public static final int RADIAL_SELECTED_OUTLINE = 0x79D7FF;

    private TdmcUiTheme() {
    }

    public static void fillPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_BACKGROUND);
        outline(graphics, x, y, width, height, BORDER_DARK);
        if (width > 4 && height > 4) {
            outline(graphics, x + 1, y + 1, width - 2, height - 2, BORDER);
        }
        graphics.fill(x + 2, y + 2, x + Math.max(2, width - 2), y + 3, ACCENT_SOFT);
    }

    public static void fillHeader(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, HEADER_BACKGROUND);
        graphics.fill(x, y + height - 1, x + width, y + height, ACCENT);
    }

    public static void fillSection(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, SECTION_BACKGROUND);
        outline(graphics, x, y, width, height, BORDER);
    }

    public static void fillSlot(GuiGraphics graphics, int x, int y, int width, int height, boolean hovered) {
        graphics.fill(x, y, x + width, y + height, hovered ? SLOT_HOVERED : SLOT_BACKGROUND);
        outline(graphics, x, y, width, height, hovered ? BORDER_HOVERED : SLOT_BORDER);
    }

    public static void fillControl(GuiGraphics graphics, int x, int y, int width, int height,
                                   boolean active, boolean hoveredOrFocused, boolean pressed) {
        int background = !active
                ? BUTTON_DISABLED
                : pressed ? BUTTON_PRESSED : hoveredOrFocused ? BUTTON_HOVERED : BUTTON_BACKGROUND;
        int border = hoveredOrFocused && active ? BORDER_HOVERED : BORDER;
        graphics.fill(x, y, x + width, y + height, background);
        outline(graphics, x, y, width, height, border);
        if (active && width > 4) {
            graphics.fill(x + 2, y + 1, x + width - 2, y + 2, pressed ? BORDER_SELECTED : ACCENT_SOFT);
        }
    }

    public static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    public static float red(int rgb) {
        return ((rgb >>> 16) & 0xFF) / 255.0F;
    }

    public static float green(int rgb) {
        return ((rgb >>> 8) & 0xFF) / 255.0F;
    }

    public static float blue(int rgb) {
        return (rgb & 0xFF) / 255.0F;
    }

    private static int argb(int red, int green, int blue) {
        return (ALPHA_75 << 24) | (red << 16) | (green << 8) | blue;
    }
}
