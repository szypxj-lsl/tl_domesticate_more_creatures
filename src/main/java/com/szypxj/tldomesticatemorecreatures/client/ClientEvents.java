package com.szypxj.tldomesticatemorecreatures.client;

import com.szypxj.tldomesticatemorecreatures.client.gui.theme.TdmcUiTheme;
import com.szypxj.tldomesticatemorecreatures.client.talent.ClientActiveTalentState;
import com.szypxj.tldomesticatemorecreatures.item.SpyglassItemHelper;
import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.client.riding.ClientRidingConfigCache;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticBlockCarrierData;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticCarrierEntityMarker;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticEggBlockMarker;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticItemCarrier;
import com.szypxj.tldomesticatemorecreatures.game.genetics.GeneticItemStackCarrierView;
import com.szypxj.tldomesticatemorecreatures.network.InspectSnapshot;
import com.szypxj.tldomesticatemorecreatures.network.InspectTarget;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.network.TargetHudSnapshot;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.Locale;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final double TARGET_HUD_RANGE = 6.0D;
    private static final ResourceLocation SUPER_SPYGLASS_SCOPE = ResourceLocation.tryBuild(TlDomesticateMoreCreatures.MOD_ID, "textures/gui/spyglass/super_scope_overlay.png");
    private static final double SUPER_SPYGLASS_FOV_FACTOR = 0.1D;
    private static int inspectCooldown;
    private static InspectTarget lastInspectTarget;
    private static int targetHudCooldown;
    private static int lastTargetHudEntityId = -1;
    private static int lastScanCandidateEntityId = -1;
    private static int scanCandidateCooldown;
    private static Double superSpyglassOriginalSensitivity;

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            restoreSuperSpyglassSensitivity(minecraft);
            lastScanCandidateEntityId = -1;
            scanCandidateCooldown = 0;
            return;
        }
        updateSuperSpyglassSensitivity(minecraft);
        updateSpyglassScanCandidate(minecraft);
        while (ClientKeys.EDIT_SPYGLASS_PANEL.consumeClick()) {
            if (minecraft.screen == null) {
                minecraft.setScreen(new SpyglassPanelPositionScreen(null));
            }
        }

        double inspectRange = inspectRange(minecraft.player);
        if (inspectRange > 0.0D) {
            ClientState.clearTargetHud();
            targetHudCooldown = 0;
            lastTargetHudEntityId = -1;
            if (inspectCooldown > 0) {
                inspectCooldown--;
            }
            InspectTarget target = findSpyglassTarget(minecraft, inspectRange);
            if (target == null) {
                ClientState.clearInspect();
                lastInspectTarget = null;
                return;
            }
            if (!target.equals(lastInspectTarget) || inspectCooldown <= 0) {
                lastInspectTarget = target;
                inspectCooldown = 10;
                NetworkHandler.inspect(target);
            }
            return;
        }

        ClientState.clearInspect();
        lastInspectTarget = null;
        inspectCooldown = 0;
        if (minecraft.screen != null) {
            ClientState.clearTargetHud();
            targetHudCooldown = 0;
            lastTargetHudEntityId = -1;
            return;
        }
        if (targetHudCooldown > 0) {
            targetHudCooldown--;
        }
        LivingEntity target = findTargetHudTarget(minecraft, TARGET_HUD_RANGE);
        if (target == null) {
            ClientState.clearTargetHud();
            targetHudCooldown = 0;
            lastTargetHudEntityId = -1;
            return;
        }
        if (target.getId() != lastTargetHudEntityId || targetHudCooldown <= 0) {
            lastTargetHudEntityId = target.getId();
            targetHudCooldown = 10;
            NetworkHandler.requestTargetHud(target.getId());
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || !SpyglassItemHelper.isHoldingSuperSpyglass(minecraft.player)) {
            SuperSpyglassOutlineSelector.reset();
            return;
        }
        double range = superSpyglassEffectRange(minecraft.player);
        SuperSpyglassOutlineSelector.updateSelection(
                minecraft,
                event,
                range,
                entity -> entity.isPickable() && entity instanceof LivingEntity && entity != minecraft.player
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof SpyglassPanelPositionScreen || minecraft.player == null) {
            return;
        }
        if (inspectRange(minecraft.player) > 0.0D) {
            if (isUsingSuperSpyglass(minecraft.player)) {
                renderSuperSpyglassScope(event.getGuiGraphics(), minecraft);
            }
            if (!ClientState.inspectFresh()) {
                return;
            }
            InspectSnapshot snapshot = ClientState.inspect();
            if (snapshot == null || lastInspectTarget == null || !snapshot.target().equals(lastInspectTarget)) {
                return;
            }
            SpyglassPanelRenderer.renderConfigured(event.getGuiGraphics(), snapshot);
            return;
        }
        if (minecraft.screen != null || !ClientState.targetHudFresh()) {
            return;
        }
        TargetHudSnapshot snapshot = ClientState.targetHud();
        if (snapshot == null || snapshot.entityId() != lastTargetHudEntityId) {
            return;
        }
        if (minecraft.player.getVehicle() != null && snapshot.entityId() == minecraft.player.getVehicle().getId()) {
            return;
        }
        TargetHudRenderer.render(event.getGuiGraphics(), snapshot);
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && isUsingSuperSpyglass(minecraft.player)) {
            event.setFOV(event.getFOV() * SUPER_SPYGLASS_FOV_FACTOR);
        }
    }

    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = event.getEntity();

        if (entity == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        boolean aimedAt = minecraft.crosshairPickEntity == entity;
        boolean vanillaRelevant = entity instanceof Player || entity.hasCustomName();
        boolean imprintActive = ClientState.hasActiveImprint(entity.getId());

        if (!aimedAt && !vanillaRelevant && !imprintActive) {
            return;
        }

        Integer level = ClientState.level(entity.getId());
        if (level != null) {
            Component nameValue = event.getContent().copy().withStyle(ChatFormatting.WHITE);
            Component levelValue = Component.literal(Integer.toString(level)).withStyle(ChatFormatting.GREEN);
            event.setContent(Component.translatable(
                    "nameplate.tl_domesticate_more_creatures.level",
                    nameValue,
                    levelValue
            ));
        }

        if (aimedAt || imprintActive) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        ClientImprintState state = ClientState.imprintState(entity.getId());
        if (state == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        long now = minecraft.level.getGameTime();
        long remainingTicks = state.remainingTicks(now);
        if (remainingTicks <= 0L) {
            ClientState.removeImprintState(entity.getId());
            return;
        }

        Component attention = Component.translatable(
                "nameplate.tl_domesticate_more_creatures.imprint_attention_remaining",
                formatNameplateTime(remainingTicks)
        );
        Component progress = Component.translatable(
                "nameplate.tl_domesticate_more_creatures.imprint_progress",
                state.percent(),
                state.completed(),
                state.total()
        );
        long nextNeedTicks = state.nextNeedTicks(now);
        Component need = nextNeedTicks > 0L
                ? Component.translatable(
                        "nameplate.tl_domesticate_more_creatures.imprint_next_need",
                        formatNameplateTime(nextNeedTicks)
                )
                : imprintNeedText(state);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 0.75F, 0.0D);
        poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        Matrix4f matrix = poseStack.last().pose();
        Font font = minecraft.font;
        drawImprintNameplateLine(font, event, matrix, attention, -20.0F);
        drawImprintNameplateLine(font, event, matrix, progress, -10.0F);
        drawImprintNameplateLine(font, event, matrix, need, 0.0F);
        poseStack.popPose();
    }

    private static Component imprintNeedText(ClientImprintState state) {
        return switch (state.needType()) {
            case "PET" -> Component.translatable("nameplate.tl_domesticate_more_creatures.imprint_need_pet");
            case "WALK" -> Component.translatable("nameplate.tl_domesticate_more_creatures.imprint_need_walk");
            case "FEED" -> Component.translatable(
                    "nameplate.tl_domesticate_more_creatures.imprint_need_feed",
                    state.foodNameKey().isBlank()
                            ? Component.translatable("nameplate.tl_domesticate_more_creatures.imprint_food_unknown")
                            : Component.translatable(state.foodNameKey())
            );
            default -> Component.translatable("nameplate.tl_domesticate_more_creatures.imprint_need_pet");
        };
    }

    private static void drawImprintNameplateLine(
            Font font,
            RenderLivingEvent.Post<?, ?> event,
            Matrix4f matrix,
            Component text,
            float y
    ) {
        font.drawInBatch(
                text,
                -font.width(text) / 2.0F,
                y,
                0xFFFFFFFF,
                false,
                matrix,
                event.getMultiBufferSource(),
                Font.DisplayMode.NORMAL,
                TdmcUiTheme.HUD_BACKGROUND,
                event.getPackedLight()
        );
    }

    private static String formatNameplateTime(long ticks) {
        long totalSeconds = Math.max(0L, (ticks + 19L) / 20L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            ClientState.removeLevel(event.getEntity().getId());
            ClientState.removeImprintState(event.getEntity().getId());
            ClientState.removeOwnedPet(event.getEntity().getId());
            TargetHudSnapshot targetHud = ClientState.targetHud();
            if (targetHud != null && targetHud.entityId() == event.getEntity().getId()) {
                ClientState.clearTargetHud();
                lastTargetHudEntityId = -1;
                targetHudCooldown = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() == Minecraft.getInstance().level) {
            ClientState.clearAll();
            ClientRidingConfigCache.clear();
            ClientActiveTalentState.clear();
            lastInspectTarget = null;
            inspectCooldown = 0;
            lastTargetHudEntityId = -1;
            targetHudCooldown = 0;
            lastScanCandidateEntityId = -1;
            scanCandidateCooldown = 0;
            SuperSpyglassOutlineSelector.reset();
        }
    }

    private static void updateSpyglassScanCandidate(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null
                || !isUsingAnySpyglass(minecraft.player)) {
            clearSpyglassScanCandidate();
            return;
        }
        double range = Math.max(0.0D, Config.INSPECT_SPYGLASS_RANGE.get());
        LivingEntity target = findSpyglassLivingTarget(minecraft, range);
        int candidate = target == null ? -1 : target.getId();
        if (candidate != lastScanCandidateEntityId) {
            lastScanCandidateEntityId = candidate;
            scanCandidateCooldown = candidate < 0 ? 0 : 5;
            NetworkHandler.sendSpyglassScanCandidate(candidate);
            return;
        }
        if (candidate >= 0) {
            if (scanCandidateCooldown > 0) {
                scanCandidateCooldown--;
            }
            if (scanCandidateCooldown <= 0) {
                scanCandidateCooldown = 5;
                NetworkHandler.sendSpyglassScanCandidate(candidate);
            }
        }
    }

    private static void clearSpyglassScanCandidate() {
        if (lastScanCandidateEntityId >= 0) {
            NetworkHandler.sendSpyglassScanCandidate(-1);
        }
        lastScanCandidateEntityId = -1;
        scanCandidateCooldown = 0;
    }

    private static LivingEntity findSpyglassLivingTarget(Minecraft minecraft, double range) {
        if (minecraft.player == null || minecraft.level == null || range <= 0.0D) {
            return null;
        }
        if (SpyglassItemHelper.isHoldingSuperSpyglass(minecraft.player)) {
            return SuperSpyglassOutlineSelector.findTarget(
                    minecraft,
                    range,
                    entity -> entity.isPickable() && entity instanceof LivingEntity && entity != minecraft.player
            );
        }
        EntityHitResult entityHit = raycastInspectableEntity(
                minecraft,
                range,
                entity -> entity.isPickable() && entity instanceof LivingEntity && entity != minecraft.player
        );
        return entityHit != null && entityHit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static double inspectRange(Player player) {
        if (player == null) {
            return 0.0D;
        }
        if (SpyglassItemHelper.isHoldingSuperSpyglass(player)) {
            return Config.INSPECT_SPYGLASS_RANGE.get();
        }
        if (isUsingAnySpyglass(player)) {
            return Config.INSPECT_SPYGLASS_RANGE.get();
        }
        if (SpyglassItemHelper.isHoldingSpyglass(player)) {
            return Config.INSPECT_HANDHELD_RANGE.get();
        }
        return 0.0D;
    }

    private static boolean isUsingAnySpyglass(Player player) {
        return SpyglassItemHelper.isUsingSpyglass(player);
    }

    static boolean isUsingSuperSpyglass(Player player) {
        return SpyglassItemHelper.isUsingSuperSpyglass(player);
    }

    static boolean isSuperSpyglassEquippedOrHeld(Player player) {
        return SpyglassItemHelper.isHoldingSuperSpyglass(player);
    }

    public static boolean shouldForceSuperSpyglassGlow(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(entity instanceof LivingEntity living)
                || minecraft.player == null
                || minecraft.level == null
                || entity == minecraft.player) {
            return false;
        }
        double range = superSpyglassEffectRange(minecraft.player);
        if (range <= 0.0D) {
            return false;
        }
        return minecraft.player.getEyePosition(1.0F)
                .distanceToSqr(living.getBoundingBox().getCenter()) <= range * range;
    }

    private static double superSpyglassEffectRange(Player player) {
        if (player == null || !SpyglassItemHelper.isHoldingSuperSpyglass(player)) {
            return 0.0D;
        }
        return Math.max(0.0D, Config.INSPECT_SPYGLASS_RANGE.get());
    }

    private static void updateSuperSpyglassSensitivity(Minecraft minecraft) {
        if (minecraft == null || minecraft.options == null) {
            return;
        }
        if (minecraft.player != null && isUsingSuperSpyglass(minecraft.player)) {
            if (superSpyglassOriginalSensitivity == null) {
                superSpyglassOriginalSensitivity = minecraft.options.sensitivity().get();
                minecraft.options.sensitivity().set(superSpyglassOriginalSensitivity * 0.2D);
            }
            return;
        }
        restoreSuperSpyglassSensitivity(minecraft);
    }

    private static void restoreSuperSpyglassSensitivity(Minecraft minecraft) {
        if (minecraft == null || minecraft.options == null || superSpyglassOriginalSensitivity == null) {
            return;
        }
        minecraft.options.sensitivity().set(superSpyglassOriginalSensitivity);
        superSpyglassOriginalSensitivity = null;
    }

    private static InspectTarget findSpyglassTarget(Minecraft minecraft, double range) {
        if (minecraft.player == null || minecraft.level == null || range <= 0.0D) {
            return null;
        }
        if (SpyglassItemHelper.isHoldingSuperSpyglass(minecraft.player)) {
            LivingEntity outlineTarget = SuperSpyglassOutlineSelector.findTarget(
                    minecraft,
                    range,
                    entity -> entity.isPickable() && isInspectableEntityCandidate(entity)
            );
            if (outlineTarget != null) {
                return InspectTarget.entity(outlineTarget.getId());
            }
        } else {
            EntityHitResult entityHit = raycastInspectableEntity(
                    minecraft,
                    range,
                    entity -> entity.isPickable() && isInspectableEntityCandidate(entity)
            );
            if (entityHit != null) {
                return InspectTarget.entity(entityHit.getEntity().getId());
            }
        }
        HitResult blockHit = minecraft.player.pick(range, 1.0F, false);
        if (blockHit instanceof BlockHitResult blockResult && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockEntity blockEntity = minecraft.level.getBlockEntity(blockResult.getBlockPos());
            if (isInspectableBlockCandidate(blockEntity)) {
                return InspectTarget.block(blockResult.getBlockPos());
            }
        }
        return null;
    }

    private static void renderSuperSpyglassScope(net.minecraft.client.gui.GuiGraphics graphics, Minecraft minecraft) {
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int size = Math.min(width, height);
        int x = (width - size) / 2;
        int y = (height - size) / 2;
        RenderSystem.enableBlend();
        graphics.blit(SUPER_SPYGLASS_SCOPE, x, y, 0.0F, 0.0F, size, size, size, size);
    }

    private static EntityHitResult raycastInspectableEntity(Minecraft minecraft, double range, Predicate<Entity> predicate) {
        Vec3 start = minecraft.player.getEyePosition(1.0F);
        Vec3 look = minecraft.player.getViewVector(1.0F);
        Vec3 fullEnd = start.add(look.scale(range));
        HitResult blockHit = minecraft.player.pick(range, 1.0F, false);
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? fullEnd : blockHit.getLocation();
        double maxDistance = start.distanceToSqr(end);
        AABB searchBox = minecraft.player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        return ProjectileUtil.getEntityHitResult(
                minecraft.player,
                start,
                end,
                searchBox,
                predicate,
                maxDistance
        );
    }

    private static LivingEntity findTargetHudTarget(Minecraft minecraft, double range) {
        if (minecraft.player == null || minecraft.level == null || range <= 0.0D) {
            return null;
        }
        Vec3 start = minecraft.player.getEyePosition(1.0F);
        Vec3 look = minecraft.player.getViewVector(1.0F);
        Vec3 fullEnd = start.add(look.scale(range));
        HitResult blockHit = minecraft.player.pick(range, 1.0F, false);
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? fullEnd : blockHit.getLocation();
        double maxDistance = start.distanceToSqr(end);
        AABB box = minecraft.player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                minecraft.player,
                start,
                end,
                box,
                entity -> entity.isPickable()
                        && entity instanceof LivingEntity
                        && entity != minecraft.player
                        && entity != minecraft.player.getVehicle(),
                maxDistance
        );
        return entityHit != null && entityHit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static boolean isInspectableEntityCandidate(Entity entity) {
        if (entity instanceof LivingEntity || entity instanceof GeneticCarrierEntityMarker) {
            return true;
        }
        return entity instanceof ItemEntity itemEntity && GeneticItemCarrier.get(itemEntity.getItem()).isPresent();
    }

    private static boolean isInspectableBlockCandidate(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return false;
        }
        if (blockEntity instanceof GeneticEggBlockMarker || GeneticBlockCarrierData.get(blockEntity).isPresent()) {
            return true;
        }
        if (blockEntity instanceof GeneticItemStackCarrierView carrierView) {
            return carrierView.tdmc$getGeneticItemStack() != null && !carrierView.tdmc$getGeneticItemStack().isEmpty();
        }
        return false;
    }

}
