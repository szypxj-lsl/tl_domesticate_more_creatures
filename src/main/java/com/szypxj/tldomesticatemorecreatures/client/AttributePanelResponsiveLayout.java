package com.szypxj.tldomesticatemorecreatures.client;

public final class AttributePanelResponsiveLayout {
    public static final int HORIZONTAL_SAFE_MARGIN = 64;
    public static final int VERTICAL_SAFE_MARGIN = 16;
    public static final double MAX_SCALE = 1.08D;

    private AttributePanelResponsiveLayout() {
    }

    public static Layout calculate(int screenWidth, int screenHeight, int panelWidth, int panelHeight) {
        int safeWidth = Math.max(1, screenWidth - HORIZONTAL_SAFE_MARGIN * 2);
        int safeHeight = Math.max(1, screenHeight - VERTICAL_SAFE_MARGIN * 2);
        double fitScale = Math.min(
                safeWidth / (double) Math.max(1, panelWidth),
                safeHeight / (double) Math.max(1, panelHeight)
        );
        double scale = Math.max(0.05D, Math.min(MAX_SCALE, fitScale));
        Mode mode = scale >= 1.0D
                ? Mode.NORMAL
                : scale >= 0.8D ? Mode.COMPACT : Mode.SMALL;
        return new Layout(scale, mode);
    }

    public static double screenToLayout(double coordinate, double center, double scale) {
        return center + (coordinate - center) / scale;
    }

    public static double layoutToScreen(double coordinate, double center, double scale) {
        return center + (coordinate - center) * scale;
    }

    public static ScissorRect layoutScissorToScreen(
            int left,
            int top,
            int right,
            int bottom,
            int screenWidth,
            int screenHeight,
            double scale
    ) {
        double centerX = screenWidth / 2.0D;
        double centerY = screenHeight / 2.0D;
        int screenLeft = (int) Math.floor(layoutToScreen(left, centerX, scale));
        int screenTop = (int) Math.floor(layoutToScreen(top, centerY, scale));
        int screenRight = (int) Math.ceil(layoutToScreen(right, centerX, scale));
        int screenBottom = (int) Math.ceil(layoutToScreen(bottom, centerY, scale));
        return new ScissorRect(screenLeft, screenTop, screenRight, screenBottom);
    }

    public enum Mode {
        NORMAL,
        COMPACT,
        SMALL
    }

    public record Layout(double scale, Mode mode) {
    }

    public record ScissorRect(int left, int top, int right, int bottom) {
    }
}
