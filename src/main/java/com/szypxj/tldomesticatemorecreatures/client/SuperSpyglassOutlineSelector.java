package com.szypxj.tldomesticatemorecreatures.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class SuperSpyglassOutlineSelector {
    private static final int SAMPLE_RADIUS = 1;
    private static final int SAMPLE_SIZE = SAMPLE_RADIUS * 2 + 1;
    private static final ByteBuffer PIXELS = ByteBuffer.allocateDirect(SAMPLE_SIZE * SAMPLE_SIZE * 4);

    /*
     * This buffer source is deliberately reused for every selection-mask render.
     *
     * BufferBuilder owns native/direct memory and expands to fit the rendered model.
     * Creating a new BufferBuilder for every candidate on every render frame leaves
     * many large direct buffers waiting for GC/Cleaner reclamation and can exhaust
     * native memory long before Java heap is full.
     */
    private static final MultiBufferSource.BufferSource SELECTION_BUFFER_SOURCE =
            MultiBufferSource.immediate(new BufferBuilder(256));

    private static RenderTarget selectionTarget;
    private static int selectedEntityId = -1;

    private SuperSpyglassOutlineSelector() {
    }

    public static void updateSelection(Minecraft minecraft, RenderLevelStageEvent event, double range, Predicate<Entity> predicate) {
        selectedEntityId = -1;
        if (minecraft == null
                || minecraft.level == null
                || minecraft.player == null
                || event == null
                || range <= 0.0D) {
            return;
        }

        Vec3 eye = minecraft.player.getEyePosition(event.getPartialTick());
        Vec3 look = minecraft.player.getViewVector(event.getPartialTick()).normalize();
        AABB searchBox = minecraft.player.getBoundingBox().inflate(range);
        double rangeSqr = range * range;

        List<LivingEntity> candidates = minecraft.level.getEntitiesOfClass(
                        LivingEntity.class,
                        searchBox,
                        entity -> entity != minecraft.player && entity.isAlive() && predicate.test(entity)
                ).stream()
                .filter(entity -> eye.distanceToSqr(entity.getBoundingBox().getCenter()) <= rangeSqr)
                .filter(entity -> rayCanReachRenderedModel(eye, look, entity))
                .sorted(Comparator.comparingDouble(entity -> eye.distanceToSqr(entity.getBoundingBox().getCenter())))
                .toList();

        for (LivingEntity entity : candidates) {
            if (renderSelectionMask(minecraft, event, entity)) {
                selectedEntityId = entity.getId();
                return;
            }
        }
    }

    public static LivingEntity findTarget(Minecraft minecraft, double range, Predicate<Entity> predicate) {
        if (selectedEntityId < 0 || minecraft == null || minecraft.level == null || minecraft.player == null || range <= 0.0D) {
            return null;
        }
        Entity entity = minecraft.level.getEntity(selectedEntityId);
        if (!(entity instanceof LivingEntity living)
                || living == minecraft.player
                || !living.isAlive()
                || !predicate.test(living)) {
            return null;
        }
        Vec3 eye = minecraft.player.getEyePosition(1.0F);
        if (eye.distanceToSqr(living.getBoundingBox().getCenter()) > range * range) {
            return null;
        }
        return living;
    }

    public static void reset() {
        selectedEntityId = -1;
        if (selectionTarget != null) {
            selectionTarget.destroyBuffers();
            selectionTarget = null;
        }
    }

    private static boolean renderSelectionMask(Minecraft minecraft, RenderLevelStageEvent event, LivingEntity entity) {
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        ensureSelectionTarget(mainTarget.viewWidth, mainTarget.viewHeight);
        if (selectionTarget == null) {
            return false;
        }

        selectionTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        selectionTarget.clear(Minecraft.ON_OSX);
        selectionTarget.bindWrite(true);

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = SELECTION_BUFFER_SOURCE;
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick();
        double x = Mth.lerp(partialTick, entity.xo, entity.getX()) - camera.x;
        double y = Mth.lerp(partialTick, entity.yo, entity.getY()) - camera.y;
        double z = Mth.lerp(partialTick, entity.zo, entity.getZ()) - camera.z;

        boolean hit;
        poseStack.pushPose();
        try {
            RenderSystem.enableDepthTest();
            dispatcher.setRenderShadow(false);
            dispatcher.render(
                    entity,
                    x,
                    y,
                    z,
                    Mth.lerp(partialTick, entity.yRotO, entity.getYRot()),
                    partialTick,
                    poseStack,
                    bufferSource,
                    LightTexture.FULL_BRIGHT
            );
            bufferSource.endBatch();
            hit = sampleCrosshair(selectionTarget);
        } finally {
            poseStack.popPose();
            dispatcher.setRenderShadow(true);
            selectionTarget.unbindWrite();
            mainTarget.bindWrite(true);
        }
        return hit;
    }

    private static void ensureSelectionTarget(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (selectionTarget == null) {
            selectionTarget = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            return;
        }
        if (selectionTarget.viewWidth != width || selectionTarget.viewHeight != height) {
            selectionTarget.resize(width, height, Minecraft.ON_OSX);
        }
    }

    private static boolean sampleCrosshair(RenderTarget target) {
        int x = target.viewWidth / 2 - SAMPLE_RADIUS;
        int y = target.viewHeight / 2 - SAMPLE_RADIUS;
        PIXELS.clear();
        int previousReadFramebuffer = GlStateManager._getInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
        RenderSystem.pixelStore(GL11.GL_PACK_ALIGNMENT, 1);
        RenderSystem.readPixels(x, y, SAMPLE_SIZE, SAMPLE_SIZE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, PIXELS);
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousReadFramebuffer);
        for (int i = 0; i < SAMPLE_SIZE * SAMPLE_SIZE; i++) {
            if ((PIXELS.get(i * 4 + 3) & 0xFF) != 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean rayCanReachRenderedModel(Vec3 eye, Vec3 look, LivingEntity entity) {
        AABB box = entity.getBoundingBox();
        Vec3 center = box.getCenter();
        Vec3 toCenter = center.subtract(eye);
        double along = toCenter.dot(look);
        if (along <= 0.0D) {
            return false;
        }
        double perpendicularSqr = Math.max(0.0D, toCenter.lengthSqr() - along * along);
        double radius = Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize())) * 0.75D + 0.5D;
        return perpendicularSqr <= radius * radius;
    }
}
