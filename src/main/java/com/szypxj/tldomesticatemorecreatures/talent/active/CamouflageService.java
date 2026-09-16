package com.szypxj.tldomesticatemorecreatures.talent.active;

import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.network.packet.S2CActiveTalentVisualPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CamouflageService {
    private static final Map<UUID, UUID> CAMOUFLAGED_TARGETS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> MOB_TARGETS = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> TARGETING_MOBS = new ConcurrentHashMap<>();

    private CamouflageService() {
    }

    public static void start(ServerPlayer rider, LivingEntity mount, ActiveTalentConfig.CamouflageConfig config) {
        if (rider == null || mount == null || config == null) return;
        long now = mount.level().getGameTime();
        ActiveTalentRuntimeState state = new ActiveTalentRuntimeState(
                rider.getUUID(), mount.getUUID(), mount.getId(), ActiveTalentIds.CAMOUFLAGE,
                ActiveTalentPhase.CAMOUFLAGED, now + config.maxDurationTicks(), null, config
        );
        ActiveTalentService.install(mount, state);
        CAMOUFLAGED_TARGETS.put(mount.getUUID(), mount.getUUID());
        CAMOUFLAGED_TARGETS.put(rider.getUUID(), mount.getUUID());
        clearKnownAggro(mount, rider);
        NetworkHandler.sendActiveTalentVisual(mount, new S2CActiveTalentVisualPacket(
                mount.getId(), rider.getId(), S2CActiveTalentVisualPacket.VisualType.CAMOUFLAGE_START, config.renderAlpha()
        ));
        ActiveTalentService.sync(rider, mount, state);
    }

    public static void tick(ServerPlayer rider, LivingEntity mount, ActiveTalentRuntimeState state) {
        if (mount.level().getGameTime() >= state.phaseEndGameTime()) {
            breakCamouflage(rider, mount, state);
            return;
        }
        validateTrackedAggro(mount, rider);
    }

    public static double detectionMultiplierFor(Mob mob, LivingEntity target) {
        if (mob == null || target == null) return 1.0D;
        UUID mountUuid = CAMOUFLAGED_TARGETS.get(target.getUUID());
        if (mountUuid == null) return 1.0D;
        ActiveTalentRuntimeState state = activeStateForMount(mob, mountUuid);
        return state == null || state.camouflageConfig() == null ? 1.0D : state.camouflageConfig().enemyDetectionMultiplier();
    }

    public static boolean mayAcquireTarget(Mob mob, LivingEntity target) {
        double multiplier = detectionMultiplierFor(mob, target);
        if (multiplier >= 0.999D) return true;
        double followRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        double range = Math.max(1.0D, followRange * multiplier);
        return mob.distanceToSqr(target) <= range * range;
    }

    public static void onMobTargetChanged(Mob mob, LivingEntity target) {
        if (mob == null) return;
        removeTrackedMob(mob.getUUID());
        if (target == null || !CAMOUFLAGED_TARGETS.containsKey(target.getUUID())) return;
        UUID mobUuid = mob.getUUID();
        MOB_TARGETS.put(mobUuid, target.getUUID());
        TARGETING_MOBS.computeIfAbsent(target.getUUID(), ignored -> ConcurrentHashMap.newKeySet()).add(mobUuid);
    }

    public static void onMobRemoved(Mob mob) {
        if (mob != null) removeTrackedMob(mob.getUUID());
    }

    public static void syncVisualTo(ServerPlayer viewer, LivingEntity mount) {
        if (viewer == null || mount == null) return;
        ActiveTalentRuntimeState state = ActiveTalentService.runtime(mount);
        if (state == null || !ActiveTalentIds.CAMOUFLAGE.equals(state.skillId()) || state.camouflageConfig() == null) return;
        ServerPlayer rider = ownerRider(mount, state);
        NetworkHandler.sendActiveTalentVisual(viewer, new S2CActiveTalentVisualPacket(
                mount.getId(), rider == null ? -1 : rider.getId(),
                S2CActiveTalentVisualPacket.VisualType.CAMOUFLAGE_START, state.camouflageConfig().renderAlpha()
        ));
    }


    public static void captureProjectileAmbush(Projectile projectile) {
        if (projectile == null || !(projectile.getOwner() instanceof LivingEntity owner)) return;
        ActiveTalentRuntimeState state = ActiveTalentService.runtime(owner);
        if (state == null || !ActiveTalentIds.CAMOUFLAGE.equals(state.skillId())) return;
        double multiplier = consumeAmbushMultiplier(owner);
        if (multiplier > 1.0D) {
            projectile.getPersistentData().putDouble("tdmcCamouflageAmbushMultiplier", multiplier);
        }
    }

    public static double takeProjectileAmbushMultiplier(Projectile projectile) {
        if (projectile == null || !projectile.getPersistentData().contains("tdmcCamouflageAmbushMultiplier")) return 1.0D;
        double multiplier = projectile.getPersistentData().getDouble("tdmcCamouflageAmbushMultiplier");
        projectile.getPersistentData().remove("tdmcCamouflageAmbushMultiplier");
        return Double.isFinite(multiplier) && multiplier > 1.0D ? multiplier : 1.0D;
    }

    public static double consumeAmbushMultiplier(LivingEntity attacker) {
        if (attacker == null) return 1.0D;
        ActiveTalentRuntimeState state = ActiveTalentService.runtime(attacker);
        if (state == null || !ActiveTalentIds.CAMOUFLAGE.equals(state.skillId()) || state.camouflageConfig() == null) return 1.0D;
        double multiplier = state.camouflageConfig().ambushDamageMultiplier();
        ServerPlayer rider = ownerRider(attacker, state);
        breakCamouflage(rider, attacker, state);
        return multiplier;
    }

    public static void onPositiveIncomingDamage(LivingEntity victim, float amount) {
        if (victim == null || amount <= 0.0F) return;
        UUID mountUuid = CAMOUFLAGED_TARGETS.get(victim.getUUID());
        if (mountUuid == null || !(victim.level() instanceof ServerLevel level)) return;
        Entity entity = level.getEntity(mountUuid);
        if (!(entity instanceof LivingEntity mount)) return;
        ActiveTalentRuntimeState state = ActiveTalentService.runtime(mount);
        if (state == null || !ActiveTalentIds.CAMOUFLAGE.equals(state.skillId())) return;
        breakCamouflage(ownerRider(mount, state), mount, state);
    }

    public static void breakCamouflage(ServerPlayer rider, LivingEntity mount, ActiveTalentRuntimeState state) {
        if (mount == null || state == null || !ActiveTalentIds.CAMOUFLAGE.equals(state.skillId())) return;
        cleanupRuntimeTargets(state);
        NetworkHandler.sendActiveTalentVisual(mount, new S2CActiveTalentVisualPacket(
                mount.getId(), rider == null ? -1 : rider.getId(), S2CActiveTalentVisualPacket.VisualType.CAMOUFLAGE_END, 1.0F
        ));
        ActiveTalentService.finish(rider, mount, state, state.camouflageConfig().cooldownTicks());
    }

    static void cleanupRuntimeTargets(ActiveTalentRuntimeState state) {
        if (state == null) return;
        CAMOUFLAGED_TARGETS.remove(state.mountUuid(), state.mountUuid());
        CAMOUFLAGED_TARGETS.remove(state.riderUuid(), state.mountUuid());
    }

    public static void clearAll() {
        CAMOUFLAGED_TARGETS.clear();
        MOB_TARGETS.clear();
        TARGETING_MOBS.clear();
    }

    private static void clearKnownAggro(LivingEntity mount, ServerPlayer rider) {
        if (!(mount.level() instanceof ServerLevel level)) return;
        clearAggroFor(level, mount.getUUID());
        clearAggroFor(level, rider.getUUID());
        AABB area = mount.getBoundingBox().inflate(64.0D);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
            LivingEntity target = mob.getTarget();
            if (target == mount || target == rider) mob.setTarget(null);
        }
    }

    private static void clearAggroFor(ServerLevel level, UUID targetUuid) {
        Set<UUID> mobs = TARGETING_MOBS.get(targetUuid);
        if (mobs == null) return;
        for (UUID mobUuid : new ArrayList<>(mobs)) {
            Entity entity = level.getEntity(mobUuid);
            if (entity instanceof Mob mob && mob.getTarget() != null && targetUuid.equals(mob.getTarget().getUUID())) {
                mob.setTarget(null);
            }
        }
    }

    private static void validateTrackedAggro(LivingEntity mount, ServerPlayer rider) {
        if (!(mount.level() instanceof ServerLevel level)) return;
        validateAggroFor(level, mount);
        validateAggroFor(level, rider);
    }

    private static void validateAggroFor(ServerLevel level, LivingEntity target) {
        Set<UUID> mobs = TARGETING_MOBS.get(target.getUUID());
        if (mobs == null) return;
        for (UUID mobUuid : new ArrayList<>(mobs)) {
            Entity entity = level.getEntity(mobUuid);
            if (!(entity instanceof Mob mob) || mob.getTarget() != target) continue;
            if (!mayAcquireTarget(mob, target)) mob.setTarget(null);
        }
    }

    private static void removeTrackedMob(UUID mobUuid) {
        UUID previous = MOB_TARGETS.remove(mobUuid);
        if (previous == null) return;
        Set<UUID> previousSet = TARGETING_MOBS.get(previous);
        if (previousSet != null) {
            previousSet.remove(mobUuid);
            if (previousSet.isEmpty()) TARGETING_MOBS.remove(previous, previousSet);
        }
    }

    private static ActiveTalentRuntimeState activeStateForMount(Mob mob, UUID mountUuid) {
        if (!(mob.level() instanceof ServerLevel level)) return null;
        Entity entity = level.getEntity(mountUuid);
        if (!(entity instanceof LivingEntity mount)) return null;
        ActiveTalentRuntimeState state = ActiveTalentService.runtime(mount);
        return state != null && ActiveTalentIds.CAMOUFLAGE.equals(state.skillId()) ? state : null;
    }

    private static ServerPlayer ownerRider(LivingEntity mount, ActiveTalentRuntimeState state) {
        if (!(mount.level() instanceof ServerLevel level)) return null;
        return level.getServer().getPlayerList().getPlayer(state.riderUuid());
    }
}
