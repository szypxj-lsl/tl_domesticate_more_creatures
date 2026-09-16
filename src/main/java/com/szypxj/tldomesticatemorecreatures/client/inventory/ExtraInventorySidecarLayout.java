package com.szypxj.tldomesticatemorecreatures.client.inventory;

public final class ExtraInventorySidecarLayout {
    private ExtraInventorySidecarLayout() {
    }

    public static Position resolve(
            int screenWidth,
            int screenHeight,
            int leftPos,
            int topPos,
            int imageWidth,
            int imageHeight,
            int sideWidth,
            int sideHeight,
            int gap,
            int margin
    ) {
        int leftSpace = leftPos - margin;
        int rightEdge = leftPos + imageWidth;
        int rightSpace = screenWidth - margin - rightEdge;
        int required = sideWidth + gap;

        int x;
        if (leftSpace >= required) {
            x = leftPos - gap - sideWidth;
        } else if (rightSpace >= required) {
            x = rightEdge + gap;
        } else if (rightSpace > leftSpace) {
            x = clamp(rightEdge + gap, margin, Math.max(margin, screenWidth - margin - sideWidth));
        } else {
            x = clamp(leftPos - gap - sideWidth, margin, Math.max(margin, screenWidth - margin - sideWidth));
        }

        int centeredY = topPos + (imageHeight - sideHeight) / 2;
        int maxY = Math.max(margin, screenHeight - margin - sideHeight);
        int y = clamp(centeredY, margin, maxY);
        return new Position(x, y);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Position(int x, int y) {
    }
}
