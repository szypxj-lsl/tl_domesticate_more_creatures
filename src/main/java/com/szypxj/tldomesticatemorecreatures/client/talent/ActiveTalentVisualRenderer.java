package com.szypxj.tldomesticatemorecreatures.client.talent;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CActiveTalentVisualPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID, value = Dist.CLIENT)
public final class ActiveTalentVisualRenderer {
    private static final int BULLET_TIME_BASE_ALPHA = 22;
    private static final int VIGNETTE_LAYERS = 12;
    private static final long FOCUS_PULSE_NANOS = 420_000_000L;
    private static final long LIGHTNING_LIFETIME_NANOS = 250_000_000L;
    private static final long LIGHTNING_MAX_WAIT_NANOS = 750_000_000L;
    private static final int MAX_ACTIVE_LIGHTNING = 96;
    private static final double LIGHTNING_SEGMENTS_PER_BLOCK = 2.9D;
    private static final int MAX_LIGHTNING_SEGMENTS = 32;
    private static final List<LightningTrail> ACTIVE_LIGHTNING = new ArrayList<>();
    private static final Map<Integer, Long> MARKING_STARTED_NANOS = new HashMap<>();

    private ActiveTalentVisualRenderer() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ACTIVE_LIGHTNING.clear();
            MARKING_STARTED_NANOS.clear();
            return;
        }
        for (int i = 0; i < 32; i++) {
            ClientActiveTalentState.VisualEvent visual = ClientActiveTalentState.consumeVisual();
            if (visual == null) break;
            renderVisual(minecraft, visual);
        }
    }

    private static void renderVisual(Minecraft minecraft, ClientActiveTalentState.VisualEvent visual) {
        if (minecraft.level == null) return;
        Entity mount = minecraft.level.getEntity(visual.mountEntityId());
        Entity target = visual.targetEntityId() < 0 ? null : minecraft.level.getEntity(visual.targetEntityId());
        if (visual.type() == S2CActiveTalentVisualPacket.VisualType.SHADOWSTEP_START) {
            MARKING_STARTED_NANOS.put(visual.mountEntityId(), System.nanoTime());
            if (mount != null) burst(minecraft, mount, true);
        } else if (visual.type() == S2CActiveTalentVisualPacket.VisualType.SHADOWSTEP_CLEAR) {
            MARKING_STARTED_NANOS.remove(visual.mountEntityId());
        } else if (visual.type() == S2CActiveTalentVisualPacket.VisualType.SHADOWSTEP_MARK && target != null) {
            minecraft.level.addParticle(ParticleTypes.ENCHANT, target.getX(), target.getY() + target.getBbHeight() * 0.7D, target.getZ(), 0.0D, 0.05D, 0.0D);
        } else if (visual.type() == S2CActiveTalentVisualPacket.VisualType.SHADOWSTEP_HIT && target != null) {
            minecraft.level.addParticle(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(), 0.0D, 0.0D, 0.0D);
            hitSparkBurst(minecraft, target);
        } else if (visual.type() == S2CActiveTalentVisualPacket.VisualType.SHADOWSTEP_TRAIL) {
            queueLightningTrail(visual);
        } else if ((visual.type() == S2CActiveTalentVisualPacket.VisualType.CAMOUFLAGE_START
                || visual.type() == S2CActiveTalentVisualPacket.VisualType.CAMOUFLAGE_END) && mount != null) {
            burst(minecraft, mount, false);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide) {
            ClientActiveTalentState.removeEntity(event.getEntity().getId());
            MARKING_STARTED_NANOS.remove(event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientActiveTalentState.Snapshot state = ClientActiveTalentState.currentForMountedPet();
        if (minecraft.player == null || minecraft.screen != null || state == null || !"MARKING".equals(state.phase())) return;
        int mountEntityId = minecraft.player.getVehicle() == null ? -1 : minecraft.player.getVehicle().getId();
        renderBulletTimeOverlay(event.getGuiGraphics(), minecraft, mountEntityId);
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        ClientActiveTalentState.Snapshot state = ClientActiveTalentState.currentForMountedPet();
        if (state == null || !"MARKING".equals(state.phase())) return;
        LivingEntity entity = event.getEntity();
        int count = ClientActiveTalentState.markedCount(entity.getId());
        if (count <= 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        Component text = Component.literal(Integer.toString(count));
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 0.45D, 0.0D);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix = poseStack.last().pose();
        Font font = minecraft.font;
        font.drawInBatch(text, -font.width(text) / 2.0F, 0.0F, 0xFFFFFFFF, false, matrix,
                event.getMultiBufferSource(), Font.DisplayMode.NORMAL, 0x66000000, event.getPackedLight());
        poseStack.popPose();
    }

    private static void renderBulletTimeOverlay(GuiGraphics graphics, Minecraft minecraft, int mountEntityId) {
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        graphics.fill(0, 0, width, height, argb(BULLET_TIME_BASE_ALPHA, 31, 29, 27));
        renderHuntingVignette(graphics, width, height);
        renderFocusPulse(graphics, width, height, mountEntityId);
    }

    private static void renderHuntingVignette(GuiGraphics graphics, int width, int height) {
        int maxDepth = Math.max(24, Math.min(52, Math.min(width, height) / 7));
        int band = Math.max(2, maxDepth / VIGNETTE_LAYERS);
        for (int layer = 0; layer < VIGNETTE_LAYERS; layer++) {
            float t = layer / (float) (VIGNETTE_LAYERS - 1);
            int depth = layer * band;
            int thickness = Math.min(band + 1, Math.max(1, maxDepth - depth));
            int cornerInset = Math.round(depth * (0.34F + 0.18F * t));
            int alpha = Math.max(4, Math.round(82.0F * (1.0F - t) * (1.0F - t)));
            int color = argb(alpha, 18, 16, 14);
            graphics.fill(cornerInset, depth, width - cornerInset, Math.min(height, depth + thickness), color);
            graphics.fill(cornerInset, Math.max(0, height - depth - thickness), width - cornerInset, height - depth, color);
            graphics.fill(depth, cornerInset, Math.min(width, depth + thickness), height - cornerInset, color);
            graphics.fill(Math.max(0, width - depth - thickness), cornerInset, width - depth, height - cornerInset, color);
        }
    }

    private static void renderFocusPulse(GuiGraphics graphics, int width, int height, int mountEntityId) {
        Long started = MARKING_STARTED_NANOS.get(mountEntityId);
        if (started == null) return;
        long age = Math.max(0L, System.nanoTime() - started);
        if (age >= FOCUS_PULSE_NANOS) return;
        float progress = age / (float) FOCUS_PULSE_NANOS;
        float strength = (float) Math.sin(Math.PI * progress);
        if (strength <= 0.01F) return;
        int squeeze = Math.max(2, Math.round(Math.min(width, height) * 0.038F * strength));
        int alpha = Math.max(8, Math.round(92.0F * strength));
        int dark = argb(alpha, 12, 10, 9);
        int clear = argb(0, 12, 10, 9);
        graphics.fillGradient(0, 0, width, squeeze, dark, clear);
        graphics.fillGradient(0, height - squeeze, width, height, clear, dark);
        graphics.fill(0, 0, width, height, argb(Math.round(12.0F * strength), 28, 24, 21));
    }

    private static void queueLightningTrail(ClientActiveTalentState.VisualEvent visual) {
        if (!visual.hasTrailSegment()) return;
        Vec3 start = new Vec3(visual.startX(), visual.startY(), visual.startZ());
        Vec3 end = new Vec3(visual.endX(), visual.endY(), visual.endZ());
        if (start.distanceToSqr(end) <= 1.0E-6D) return;
        if (ACTIVE_LIGHTNING.size() >= MAX_ACTIVE_LIGHTNING) ACTIVE_LIGHTNING.remove(0);
        long now = System.nanoTime();
        double seed = visual.mountEntityId() * 0.731D
                + visual.targetEntityId() * 1.173D
                + now * 1.0E-7D;
        ACTIVE_LIGHTNING.add(new LightningTrail(visual.mountEntityId(), start, end, now, seed));
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE_LIGHTNING.isEmpty()) return;
        long now = System.nanoTime();
        ACTIVE_LIGHTNING.removeIf(trail -> trail.isExpired(now));
        if (ACTIVE_LIGHTNING.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ACTIVE_LIGHTNING.clear();
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (LightningTrail trail : ACTIVE_LIGHTNING) {
            Entity mount = minecraft.level.getEntity(trail.mountEntityId());
            renderLightningBolt(poseStack, builder, trail, mount, camera, now);
        }
        BufferUploader.drawWithShader(builder.end());

        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void renderLightningBolt(PoseStack poseStack, VertexConsumer consumer, LightningTrail trail,
                                            Entity mount, Vec3 camera, long now) {
        Vec3 visibleEnd = visibleTailEnd(trail, mount, now);
        if (visibleEnd == null || trail.start().distanceToSqr(visibleEnd) <= 1.0E-6D) return;

        float life = trail.fade(now);
        if (life <= 0.0F) return;

        int flicker = (int) (now / 32_000_000L);
        double seed = trail.seed() + flicker * 17.713D;
        List<Vec3> mainPath = buildJaggedPath(trail.start(), visibleEnd, seed, 1.0D);
        int outerAlpha = Math.max(36, Math.round(220.0F * life));
        int middleAlpha = Math.max(64, Math.round(245.0F * life));
        int coreAlpha = Math.max(96, Math.round(255.0F * life));

        renderLightningPath(poseStack, consumer, mainPath, camera, 0.190F, 64, 154, 255, outerAlpha);
        renderLightningPath(poseStack, consumer, mainPath, camera, 0.095F, 128, 210, 255, middleAlpha);
        renderLightningPath(poseStack, consumer, mainPath, camera, 0.036F, 248, 253, 255, coreAlpha);
        renderLightningBranches(poseStack, consumer, mainPath, camera, seed, life);
    }

    private static Vec3 visibleTailEnd(LightningTrail trail, Entity mount, long now) {
        if (mount == null) return null;
        Vec3 segment = trail.end().subtract(trail.start());
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr <= 1.0E-8D) return null;

        Vec3 mountCenter = new Vec3(
                mount.getX(),
                mount.getY() + mount.getBbHeight() * 0.45D,
                mount.getZ()
        );
        double progress = mountCenter.subtract(trail.start()).dot(segment) / segment.lengthSqr();
        progress = Math.max(0.0D, Math.min(1.0D, progress));
        if (progress <= 0.01D) return null;

        trail.markVisible(now);
        if (progress >= 0.995D) trail.markCompleted(now);
        return trail.start().lerp(trail.end(), progress);
    }

    private static List<Vec3> buildJaggedPath(Vec3 start, Vec3 end, double seed, double amplitudeScale) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        int segments = Math.max(5, Math.min(MAX_LIGHTNING_SEGMENTS,
                (int) Math.ceil(length * LIGHTNING_SEGMENTS_PER_BLOCK)));
        Vec3 direction = delta.scale(1.0D / Math.max(1.0E-6D, length));
        Vec3 reference = Math.abs(direction.y) > 0.88D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side = direction.cross(reference).normalize();
        Vec3 up = direction.cross(side).normalize();
        double amplitude = Math.min(0.50D, 0.13D + length * 0.060D) * amplitudeScale;

        List<Vec3> points = new ArrayList<>(segments + 1);
        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            double envelope = Math.sin(Math.PI * t);
            double noiseA = pseudoNoise(seed + i * 3.173D);
            double noiseB = pseudoNoise(seed * 1.491D + i * 5.117D);
            Vec3 point = start.lerp(end, t)
                    .add(side.scale(noiseA * amplitude * envelope))
                    .add(up.scale(noiseB * amplitude * 0.78D * envelope));
            points.add(point);
        }
        return points;
    }

    private static void renderLightningBranches(PoseStack poseStack, VertexConsumer consumer, List<Vec3> mainPath,
                                                Vec3 camera, double seed, float life) {
        if (mainPath.size() < 7) return;
        int branchCount = Math.min(3, Math.max(1, mainPath.size() / 8));
        for (int branchIndex = 0; branchIndex < branchCount; branchIndex++) {
            int pointIndex = 2 + (branchIndex + 1) * (mainPath.size() - 4) / (branchCount + 1);
            pointIndex = Math.max(1, Math.min(mainPath.size() - 2, pointIndex));
            Vec3 origin = mainPath.get(pointIndex);
            Vec3 forward = mainPath.get(pointIndex + 1).subtract(mainPath.get(pointIndex - 1)).normalize();
            Vec3 reference = Math.abs(forward.y) > 0.86D
                    ? new Vec3(1.0D, 0.0D, 0.0D)
                    : new Vec3(0.0D, 1.0D, 0.0D);
            Vec3 side = forward.cross(reference).normalize();
            Vec3 up = forward.cross(side).normalize();
            double sideNoise = pseudoNoise(seed + branchIndex * 11.71D);
            double upNoise = pseudoNoise(seed * 1.83D + branchIndex * 7.19D);
            double branchLength = 0.65D + Math.abs(pseudoNoise(seed + branchIndex * 4.31D)) * 0.85D;
            Vec3 branchDirection = forward.scale(0.16D)
                    .add(side.scale(sideNoise * 0.95D))
                    .add(up.scale(0.35D + upNoise * 0.55D))
                    .normalize();
            Vec3 branchEnd = origin.add(branchDirection.scale(branchLength));
            List<Vec3> branch = buildJaggedPath(origin, branchEnd, seed + 41.0D + branchIndex * 13.0D, 0.52D);
            renderLightningPath(poseStack, consumer, branch, camera, 0.092F, 72, 164, 255, Math.max(24, Math.round(165.0F * life)));
            renderLightningPath(poseStack, consumer, branch, camera, 0.032F, 235, 248, 255, Math.max(48, Math.round(235.0F * life)));
        }
    }

    private static void renderLightningPath(PoseStack poseStack, VertexConsumer consumer, List<Vec3> points, Vec3 camera,
                                            float halfWidth, int red, int green, int blue, int alpha) {
        Matrix4f matrix = poseStack.last().pose();
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 worldFrom = points.get(i);
            Vec3 worldTo = points.get(i + 1);
            Vec3 segment = worldTo.subtract(worldFrom);
            if (segment.lengthSqr() <= 1.0E-8D) continue;
            Vec3 midpoint = worldFrom.add(worldTo).scale(0.5D);
            Vec3 facing = camera.subtract(midpoint);
            Vec3 side = segment.cross(facing);
            if (side.lengthSqr() <= 1.0E-8D) side = segment.cross(new Vec3(0.0D, 1.0D, 0.0D));
            if (side.lengthSqr() <= 1.0E-8D) side = segment.cross(new Vec3(1.0D, 0.0D, 0.0D));
            side = side.normalize().scale(halfWidth);
            Vec3 from = worldFrom.subtract(camera);
            Vec3 to = worldTo.subtract(camera);
            emitLightningQuad(matrix, consumer, from, to, side, red, green, blue, alpha);
        }
    }

    private static void emitLightningQuad(Matrix4f matrix, VertexConsumer consumer, Vec3 from, Vec3 to, Vec3 side,
                                          int red, int green, int blue, int alpha) {
        Vec3 a = from.add(side);
        Vec3 b = from.subtract(side);
        Vec3 c = to.subtract(side);
        Vec3 d = to.add(side);
        consumer.vertex(matrix, (float) a.x, (float) a.y, (float) a.z).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix, (float) b.x, (float) b.y, (float) b.z).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix, (float) c.x, (float) c.y, (float) c.z).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix, (float) d.x, (float) d.y, (float) d.z).color(red, green, blue, alpha).endVertex();
    }

    private static double pseudoNoise(double value) {
        double raw = Math.sin(value * 12.9898D) * 43758.5453D;
        return ((raw - Math.floor(raw)) * 2.0D) - 1.0D;
    }

    private static void hitSparkBurst(Minecraft minecraft, Entity entity) {
        if (minecraft.level == null) return;
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0D * i / 8.0D;
            minecraft.level.addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.55D, entity.getZ(),
                    Math.cos(angle) * 0.16D, 0.03D, Math.sin(angle) * 0.16D
            );
        }
    }

    private static void burst(Minecraft minecraft, Entity entity, boolean shadowstep) {
        if (minecraft.level == null) return;
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2.0D * i / 12.0D;
            double speed = shadowstep ? 0.10D : 0.06D;
            minecraft.level.addParticle(
                    ParticleTypes.CLOUD,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                    Math.cos(angle) * speed, 0.02D, Math.sin(angle) * speed
            );
        }
    }

    private static int argb(int alpha, int red, int green, int blue) {
        int a = Math.max(0, Math.min(255, alpha));
        int r = Math.max(0, Math.min(255, red));
        int g = Math.max(0, Math.min(255, green));
        int b = Math.max(0, Math.min(255, blue));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static final class LightningTrail {
        private final int mountEntityId;
        private final Vec3 start;
        private final Vec3 end;
        private final long receivedNanos;
        private final double seed;
        private long firstVisibleNanos = -1L;
        private long completedNanos = -1L;

        private LightningTrail(int mountEntityId, Vec3 start, Vec3 end, long receivedNanos, double seed) {
            this.mountEntityId = mountEntityId;
            this.start = start;
            this.end = end;
            this.receivedNanos = receivedNanos;
            this.seed = seed;
        }

        private int mountEntityId() {
            return mountEntityId;
        }

        private Vec3 start() {
            return start;
        }

        private Vec3 end() {
            return end;
        }

        private double seed() {
            return seed;
        }

        private void markVisible(long now) {
            if (firstVisibleNanos < 0L) firstVisibleNanos = now;
        }

        private void markCompleted(long now) {
            if (completedNanos < 0L) completedNanos = now;
        }

        private float fade(long now) {
            if (completedNanos < 0L) return 1.0F;
            long age = Math.max(0L, now - completedNanos);
            return 1.0F - Math.min(1.0F, age / (float) LIGHTNING_LIFETIME_NANOS);
        }

        private boolean isExpired(long now) {
            if (completedNanos >= 0L) return now - completedNanos > LIGHTNING_LIFETIME_NANOS;
            return now - receivedNanos > LIGHTNING_MAX_WAIT_NANOS;
        }
    }
}
