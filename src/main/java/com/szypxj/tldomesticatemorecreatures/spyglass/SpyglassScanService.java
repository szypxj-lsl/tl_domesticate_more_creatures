package com.szypxj.tldomesticatemorecreatures.spyglass;

import com.szypxj.tldomesticatemorecreatures.api.spyglass.SpyglassInspectionApi;
import com.szypxj.tldomesticatemorecreatures.api.spyglass.SpyglassScanCompleted;
import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.game.BaseAttributeSnapshotService;
import com.szypxj.tldomesticatemorecreatures.item.SpyglassItemHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SpyglassScanService {
    private static final int REQUIRED_TICKS = 20;
    private static final Map<UUID, ScanState> STATES = new HashMap<>();

    private SpyglassScanService() {
    }

    public static void setCandidate(ServerPlayer player, int entityId) {
        if (player == null) {
            return;
        }
        if (entityId < 0) {
            STATES.remove(player.getUUID());
            return;
        }
        ScanState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new ScanState());
        if (state.candidateEntityId != entityId) {
            state.candidateEntityId = entityId;
            state.progress.reset();
        }
    }

    public static void tickPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ScanState state = STATES.get(player.getUUID());
        if (state == null || state.candidateEntityId < 0) {
            return;
        }

        LivingEntity target = validate(player, state.candidateEntityId);
        boolean valid = target != null;
        if (!state.progress.advance(state.candidateEntityId, valid)) {
            if (!valid) {
                STATES.remove(player.getUUID());
            }
            return;
        }

        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (typeId != null) {
            SpyglassInspectionApi.fireCompleted(new SpyglassScanCompleted(player, target, typeId));
        }
    }

    public static void clear(ServerPlayer player) {
        if (player != null) {
            STATES.remove(player.getUUID());
        }
    }

    public static void clearAll() {
        STATES.clear();
    }

    private static LivingEntity validate(ServerPlayer player, int candidateEntityId) {
        if (!SpyglassItemHelper.isUsingSpyglass(player)) {
            return null;
        }
        Entity candidate = player.level().getEntity(candidateEntityId);
        if (!(candidate instanceof LivingEntity target) || target instanceof Player || !target.isAlive()) {
            return null;
        }
        double range = Math.max(0.0D, Config.INSPECT_SPYGLASS_RANGE.get());
        boolean superSpyglass = SpyglassItemHelper.isUsingSuperSpyglass(player);
        if (range <= 0.0D || player.distanceToSqr(target) > range * range) {
            return null;
        }
        if (!superSpyglass) {
            if (!player.hasLineOfSight(target)) {
                return null;
            }
            EntityHitResult hit = raycastLiving(player, range);
            if (hit == null || hit.getEntity().getId() != candidateEntityId) {
                return null;
            }
        }
        BaseAttributeSnapshotService.captureIfAbsent(target);
        return target;
    }

    private static EntityHitResult raycastLiving(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 fullEnd = start.add(look.scale(range));
        HitResult blockHit = player.pick(range, 1.0F, false);
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? fullEnd : blockHit.getLocation();
        double maxDistance = start.distanceToSqr(end);
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        return net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                searchBox,
                entity -> entity.isPickable() && entity instanceof LivingEntity && !(entity instanceof Player),
                maxDistance
        );
    }

    private static final class ScanState {
        private int candidateEntityId = -1;
        private final SpyglassScanProgress progress = new SpyglassScanProgress(REQUIRED_TICKS);
    }
}
