package com.szypxj.tldomesticatemorecreatures.client;

public final class SpyglassPanelLayout {
    private SpyglassPanelLayout() {
    }

    public enum Anchor {
        LEFT_TOP,
        CENTER_TOP,
        RIGHT_TOP,
        LEFT_CENTER,
        CENTER,
        RIGHT_CENTER,
        LEFT_BOTTOM,
        CENTER_BOTTOM,
        RIGHT_BOTTOM;

        public static Anchor parse(String value) {
            if (value != null) {
                try {
                    return Anchor.valueOf(value);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return RIGHT_CENTER;
        }
    }

    public static Position resolve(
            Anchor anchor,
            int offsetX,
            int offsetY,
            int screenWidth,
            int screenHeight,
            int panelWidth,
            int panelHeight,
            int margin
    ) {
        Position base = basePosition(anchor, screenWidth, screenHeight, panelWidth, panelHeight, margin);
        return clamp(
                base.x() + offsetX,
                base.y() + offsetY,
                screenWidth,
                screenHeight,
                panelWidth,
                panelHeight,
                margin
        );
    }

    public static SavedPosition fromAbsolute(
            int x,
            int y,
            Anchor anchor,
            int screenWidth,
            int screenHeight,
            int panelWidth,
            int panelHeight,
            int margin
    ) {
        Position clamped = clamp(x, y, screenWidth, screenHeight, panelWidth, panelHeight, margin);
        Position base = basePosition(anchor, screenWidth, screenHeight, panelWidth, panelHeight, margin);
        return new SavedPosition(anchor, clamped.x() - base.x(), clamped.y() - base.y());
    }

    public static Anchor nearestAnchor(
            int x,
            int y,
            int screenWidth,
            int screenHeight,
            int panelWidth,
            int panelHeight,
            int margin
    ) {
        Position clamped = clamp(x, y, screenWidth, screenHeight, panelWidth, panelHeight, margin);
        Anchor nearest = Anchor.RIGHT_CENTER;
        long nearestDistance = Long.MAX_VALUE;
        for (Anchor anchor : Anchor.values()) {
            Position base = basePosition(anchor, screenWidth, screenHeight, panelWidth, panelHeight, margin);
            long dx = clamped.x() - base.x();
            long dy = clamped.y() - base.y();
            long distance = dx * dx + dy * dy;
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = anchor;
            }
        }
        return nearest;
    }

    public static Position clamp(
            int x,
            int y,
            int screenWidth,
            int screenHeight,
            int panelWidth,
            int panelHeight,
            int margin
    ) {
        int maxPhysicalX = Math.max(0, screenWidth - panelWidth);
        int maxPhysicalY = Math.max(0, screenHeight - panelHeight);
        int minX = Math.min(Math.max(0, margin), maxPhysicalX);
        int minY = Math.min(Math.max(0, margin), maxPhysicalY);
        int maxX = Math.max(minX, Math.min(maxPhysicalX, screenWidth - panelWidth - Math.max(0, margin)));
        int maxY = Math.max(minY, Math.min(maxPhysicalY, screenHeight - panelHeight - Math.max(0, margin)));
        return new Position(clampInt(x, minX, maxX), clampInt(y, minY, maxY));
    }

    private static Position basePosition(
            Anchor anchor,
            int screenWidth,
            int screenHeight,
            int panelWidth,
            int panelHeight,
            int margin
    ) {
        int left = margin;
        int centerX = (screenWidth - panelWidth) / 2;
        int right = screenWidth - panelWidth - margin;
        int top = margin;
        int centerY = (screenHeight - panelHeight) / 2;
        int bottom = screenHeight - panelHeight - margin;

        int x = switch (anchor) {
            case LEFT_TOP, LEFT_CENTER, LEFT_BOTTOM -> left;
            case CENTER_TOP, CENTER, CENTER_BOTTOM -> centerX;
            case RIGHT_TOP, RIGHT_CENTER, RIGHT_BOTTOM -> right;
        };
        int y = switch (anchor) {
            case LEFT_TOP, CENTER_TOP, RIGHT_TOP -> top;
            case LEFT_CENTER, CENTER, RIGHT_CENTER -> centerY;
            case LEFT_BOTTOM, CENTER_BOTTOM, RIGHT_BOTTOM -> bottom;
        };
        return clamp(x, y, screenWidth, screenHeight, panelWidth, panelHeight, margin);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Position(int x, int y) {
    }

    public record SavedPosition(Anchor anchor, int offsetX, int offsetY) {
    }
}
