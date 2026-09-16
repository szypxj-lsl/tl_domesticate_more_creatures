package com.szypxj.tldomesticatemorecreatures.client.riding;

import com.mojang.blaze3d.vertex.PoseStack;
import com.szypxj.tldomesticatemorecreatures.riding.RideSeatPositioner;
import com.szypxj.tldomesticatemorecreatures.riding.config.RiderVisualProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class RiderPreviewRenderer {
    private static final int FULL_BRIGHT = 15728880;

    private RiderPreviewRenderer() {
    }

    public static RemotePlayer createPreviewRider(LivingEntity mount, AbstractClientPlayer template) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || mount == null || template == null) {
            return null;
        }
        RemotePlayer rider = new RemotePlayer(minecraft.level, template.getGameProfile());
        mount.moveTo(0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
        rider.moveTo(0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
        rider.startRiding(mount, true);
        return rider;
    }

    public static void render(
            GuiGraphics graphics,
            LivingEntity mount,
            RemotePlayer rider,
            RiderVisualProfile profile,
            PreviewViewport viewport,
            float yaw,
            float pitch,
            float zoom,
            float partialTick
    ) {
        if (graphics == null || mount == null || rider == null || profile == null || viewport == null) {
            return;
        }
        if (rider.getVehicle() != mount) {
            rider.startRiding(mount, true);
        }

        // Salvation-style preview authority: authorize the mount UUID with the
        // current unsaved draft, then let the same seat/pose path used by a live
        // ride consume it.
        RideVisualAuthority.authorizeClient(mount.getUUID(), profile);
        try {
            mount.moveTo(0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
            mount.setYRot(0.0F);
            mount.setYBodyRot(0.0F);
            mount.setYHeadRot(0.0F);
            rider.setYRot(0.0F);
            rider.setYBodyRot(0.0F);
            rider.setYHeadRot(0.0F);
            RideSeatPositioner.position(mount, rider, profile);

            AABB bounds = mount.getBoundingBox().minmax(rider.getBoundingBox());
            Vec3 center = bounds.getCenter();
            double extent = Math.max(0.5D, Math.max(bounds.getXsize(), Math.max(bounds.getYsize(), bounds.getZsize())));
            float fitX = (float) ((viewport.width() * 0.72D) / extent);
            float fitY = (float) ((viewport.height() * 0.72D) / extent);
            float renderScale = Mth.clamp(Math.min(fitX, fitY) * Mth.clamp(zoom, 0.35F, 3.0F), 8.0F, 120.0F);

            renderChecker(graphics, viewport);
            graphics.enableScissor(viewport.x(), viewport.y(), viewport.x() + viewport.width(), viewport.y() + viewport.height());
            PoseStack poseStack = graphics.pose();
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            poseStack.pushPose();
            try {
                graphics.flush();
                poseStack.translate(
                        viewport.x() + viewport.width() * 0.5D,
                        viewport.y() + viewport.height() * 0.54D,
                        160.0D
                );
                poseStack.scale(renderScale, -renderScale, renderScale);
                poseStack.mulPose(new Quaternionf().rotateXYZ(
                        pitch * Mth.DEG_TO_RAD,
                        yaw * Mth.DEG_TO_RAD,
                        0.0F
                ));
                poseStack.translate(-center.x, -center.y, -center.z);

                dispatcher.setRenderShadow(false);
                dispatcher.render(
                        mount,
                        mount.getX(), mount.getY(), mount.getZ(),
                        0.0F, partialTick,
                        poseStack, graphics.bufferSource(), FULL_BRIGHT
                );
                dispatcher.render(
                        rider,
                        rider.getX(), rider.getY(), rider.getZ(),
                        0.0F, partialTick,
                        poseStack, graphics.bufferSource(), FULL_BRIGHT
                );
                graphics.flush();
            } finally {
                dispatcher.setRenderShadow(true);
                poseStack.popPose();
                graphics.disableScissor();
            }
        } finally {
            RideVisualAuthority.revokeClient(mount.getUUID());
        }
    }

    private static void renderChecker(GuiGraphics graphics, PreviewViewport viewport) {
        int checker = 10;
        for (int y = viewport.y(); y < viewport.y() + viewport.height(); y += checker) {
            for (int x = viewport.x(); x < viewport.x() + viewport.width(); x += checker) {
                boolean alternate = (((x - viewport.x()) / checker) + ((y - viewport.y()) / checker)) % 2 == 0;
                graphics.fill(
                        x,
                        y,
                        Math.min(x + checker, viewport.x() + viewport.width()),
                        Math.min(y + checker, viewport.y() + viewport.height()),
                        alternate ? 0xFFB8B8B8 : 0xFFE2E2E2
                );
            }
        }
    }

    public record PreviewViewport(int x, int y, int width, int height) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
