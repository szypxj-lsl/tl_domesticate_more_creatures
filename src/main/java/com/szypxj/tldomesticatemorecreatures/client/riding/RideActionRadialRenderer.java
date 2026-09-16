package com.szypxj.tldomesticatemorecreatures.client.riding;

import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcUiTheme;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public final class RideActionRadialRenderer {
    public static final int DEFAULT_RADIUS = 52;
    public static final int DEFAULT_DEAD_ZONE = 12;
    private static final int SEGMENTS = 96;
    private static final float SECTOR_GAP_RADIANS = 0.065F;

    private RideActionRadialRenderer() {
    }

    public static void draw(PoseStack poseStack, int centerX, int centerY, int radius, int configuredDeadZone,
                            int count, int highlightedIndex) {
        poseStack.pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        float innerRadius = innerRadius(radius, configuredDeadZone);
        double sectorAngle = Math.PI * 2.0D / count;
        int sectorSteps = Math.max(SEGMENTS / count, 12);

        for (int i = 0; i < count; i++) {
            double start = i * sectorAngle - Math.PI / 2.0D + SECTOR_GAP_RADIANS / 2.0D;
            double end = (i + 1) * sectorAngle - Math.PI / 2.0D - SECTOR_GAP_RADIANS / 2.0D;
            boolean selected = i == highlightedIndex;
            if (selected) {
                drawFilledAnnularSector(poseStack, centerX, centerY, innerRadius, radius, start, end, sectorSteps,
                        TdmcUiTheme.red(TdmcUiTheme.RADIAL_SELECTED), TdmcUiTheme.green(TdmcUiTheme.RADIAL_SELECTED), TdmcUiTheme.blue(TdmcUiTheme.RADIAL_SELECTED), TdmcUiTheme.RADIAL_ALPHA);
            } else {
                drawFilledAnnularSector(poseStack, centerX, centerY, innerRadius, radius, start, end, sectorSteps,
                        TdmcUiTheme.red(TdmcUiTheme.RADIAL_FILL), TdmcUiTheme.green(TdmcUiTheme.RADIAL_FILL), TdmcUiTheme.blue(TdmcUiTheme.RADIAL_FILL), TdmcUiTheme.RADIAL_ALPHA);
            }
        }

        for (int i = 0; i < count; i++) {
            double start = i * sectorAngle - Math.PI / 2.0D + SECTOR_GAP_RADIANS / 2.0D;
            double end = (i + 1) * sectorAngle - Math.PI / 2.0D - SECTOR_GAP_RADIANS / 2.0D;
            boolean selected = i == highlightedIndex;
            if (selected) {
                drawAnnularSectorOutline(poseStack, centerX, centerY, innerRadius, radius, start, end, sectorSteps,
                        TdmcUiTheme.red(TdmcUiTheme.RADIAL_SELECTED_OUTLINE), TdmcUiTheme.green(TdmcUiTheme.RADIAL_SELECTED_OUTLINE), TdmcUiTheme.blue(TdmcUiTheme.RADIAL_SELECTED_OUTLINE), TdmcUiTheme.RADIAL_ALPHA, 2.2F);
            } else {
                drawAnnularSectorOutline(poseStack, centerX, centerY, innerRadius, radius, start, end, sectorSteps,
                        TdmcUiTheme.red(TdmcUiTheme.RADIAL_OUTLINE), TdmcUiTheme.green(TdmcUiTheme.RADIAL_OUTLINE), TdmcUiTheme.blue(TdmcUiTheme.RADIAL_OUTLINE), TdmcUiTheme.RADIAL_ALPHA, 1.4F);
            }
        }

        drawCircleOutline(poseStack, centerX, centerY, radius + 4.0F,
                TdmcUiTheme.red(TdmcUiTheme.RADIAL_OUTLINE), TdmcUiTheme.green(TdmcUiTheme.RADIAL_OUTLINE), TdmcUiTheme.blue(TdmcUiTheme.RADIAL_OUTLINE), TdmcUiTheme.RADIAL_ALPHA, 1.8F);

        RenderSystem.lineWidth(1.0F);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    public static float innerRadius(int radius, int configuredDeadZone) {
        return Math.max(configuredDeadZone + 10.0F, radius * 0.40F);
    }

    public static float labelRadius(int radius, int configuredDeadZone) {
        return (innerRadius(radius, configuredDeadZone) + radius) * 0.5F;
    }

    private static void drawFilledAnnularSector(PoseStack poseStack,
                                                float centerX, float centerY,
                                                float innerRadius, float outerRadius,
                                                double startAngle, double endAngle, int steps,
                                                float red, float green, float blue, float alpha) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        for (int i = 0; i <= steps; i++) {
            double angle = startAngle + (endAngle - startAngle) * i / steps;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            builder.vertex(matrix, centerX + outerRadius * cos, centerY + outerRadius * sin, 0.0F)
                    .color(red, green, blue, alpha).endVertex();
            builder.vertex(matrix, centerX + innerRadius * cos, centerY + innerRadius * sin, 0.0F)
                    .color(red, green, blue, alpha).endVertex();
        }
        tesselator.end();
    }

    private static void drawAnnularSectorOutline(PoseStack poseStack,
                                                  float centerX, float centerY,
                                                  float innerRadius, float outerRadius,
                                                  double startAngle, double endAngle, int steps,
                                                  float red, float green, float blue, float alpha,
                                                  float lineWidth) {
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();

        for (int i = 0; i <= steps; i++) {
            double angle = startAngle + (endAngle - startAngle) * i / steps;
            builder.vertex(matrix,
                    centerX + outerRadius * (float) Math.cos(angle),
                    centerY + outerRadius * (float) Math.sin(angle),
                    0.0F).color(red, green, blue, alpha).endVertex();
        }

        builder.vertex(matrix,
                centerX + innerRadius * (float) Math.cos(endAngle),
                centerY + innerRadius * (float) Math.sin(endAngle),
                0.0F).color(red, green, blue, alpha).endVertex();

        for (int i = steps; i >= 0; i--) {
            double angle = startAngle + (endAngle - startAngle) * i / steps;
            builder.vertex(matrix,
                    centerX + innerRadius * (float) Math.cos(angle),
                    centerY + innerRadius * (float) Math.sin(angle),
                    0.0F).color(red, green, blue, alpha).endVertex();
        }

        builder.vertex(matrix,
                centerX + outerRadius * (float) Math.cos(startAngle),
                centerY + outerRadius * (float) Math.sin(startAngle),
                0.0F).color(red, green, blue, alpha).endVertex();
        tesselator.end();
    }

    private static void drawCircleOutline(PoseStack poseStack,
                                          float centerX, float centerY, float radius,
                                          float red, float green, float blue, float alpha,
                                          float lineWidth) {
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = poseStack.last().pose();
        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = Math.PI * 2.0D * i / SEGMENTS;
            builder.vertex(matrix,
                    centerX + radius * (float) Math.cos(angle),
                    centerY + radius * (float) Math.sin(angle),
                    0.0F).color(red, green, blue, alpha).endVertex();
        }
        tesselator.end();
    }
}
