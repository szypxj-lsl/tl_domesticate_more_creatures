package com.szypxj.tldomesticatemorecreatures.talent;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import com.szypxj.tldomesticatemorecreatures.command.pet.capability.StandardPetCapabilities;
import com.szypxj.tldomesticatemorecreatures.config.SpecialTalentConfigManager;
import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.riding.RideService;
import com.szypxj.tldomesticatemorecreatures.torpor.TorporData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = TlDomesticateMoreCreatures.MOD_ID)
public final class FuryService {
    private static final Set<LivingEntity> TRACKED = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<LivingEntity, UUID> BERSERK_TARGETS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<LivingEntity, Long> LAST_FORCED_ATTACK = Collections.synchronizedMap(new WeakHashMap<>());
    private static final int TARGET_RESCAN_TICKS = 10;
    private static final int FORCED_ATTACK_COOLDOWN_TICKS = 20;

    private FuryService() {
    }

    public static boolean hasFury(LivingEntity entity) {
        return SpecialTalentService.hasActive(entity, SpecialTalentIds.FURY);
    }

    public static boolean isBerserk(LivingEntity entity) {
        return entity != null
                && hasFury(entity)
                && ProgressData.exists(entity)
                && ProgressData.of(entity).furyBerserk();
    }

    public static double maxAnger(LivingEntity entity) {
        if (entity == null || !hasFury(entity)) {
            return 0.0D;
        }
        return SpecialTalentMath.maxAnger(
                entity.getMaxHealth(),
                SpecialTalentConfigManager.fury().angerMaxHealthRatio()
        );
    }

    public static void refreshRegistration(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) {
            return;
        }
        if (!hasFury(entity)) {
            TRACKED.remove(entity);
            clearBerserkTarget(entity);
            if (ProgressData.exists(entity)) {
                ProgressData.of(entity).furyBerserk(false);
            }
            return;
        }
        ProgressData data = ProgressData.of(entity);
        double max = maxAnger(entity);
        data.furyAnger(SpecialTalentMath.clampAnger(data.furyAnger(), max));
        if (data.furyLastUpdateGameTime() == Long.MIN_VALUE) {
            data.furyLastUpdateGameTime(entity.level().getGameTime());
        }
        TRACKED.add(entity);
    }

    public static void recordDamage(LivingEntity entity, double finalDamage, boolean grantsAnger) {
        if (entity == null || entity.level().isClientSide || !hasFury(entity) || finalDamage <= 0.0D) {
            return;
        }
        ProgressData data = ProgressData.of(entity);
        long now = entity.level().getGameTime();
        data.furyLastDamageGameTime(now);
        data.furyLastUpdateGameTime(now);
        if (grantsAnger) {
            double max = maxAnger(entity);
            double gain = SpecialTalentMath.rageGain(
                    finalDamage,
                    data.furyBerserk(),
                    SpecialTalentConfigManager.fury().berserkDamageRageGainMultiplier()
            );
            double anger = SpecialTalentMath.clampAnger(data.furyAnger() + gain, max);
            data.furyAnger(anger);
            if (!data.furyBerserk() && max > 0.0D && anger >= max) {
                enterBerserk(entity, data, max);
            }
        }
        TRACKED.add(entity);
    }

    public static void forceExit(LivingEntity entity) {
        if (entity == null || !ProgressData.exists(entity)) {
            return;
        }
        exitBerserk(entity, ProgressData.of(entity));
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            SpecialTalentService.ensureTalents(living);
            refreshRegistration(living);
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            TRACKED.remove(living);
            BERSERK_TARGETS.remove(living);
            LAST_FORCED_ATTACK.remove(living);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TRACKED.isEmpty()) {
            return;
        }
        for (LivingEntity entity : new ArrayList<>(TRACKED)) {
            if (entity == null || entity.isRemoved() || entity.level().isClientSide || !entity.isAlive() || !hasFury(entity)) {
                TRACKED.remove(entity);
                clearBerserkTarget(entity);
                continue;
            }
            tickEntity(entity);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TRACKED.clear();
        BERSERK_TARGETS.clear();
        LAST_FORCED_ATTACK.clear();
    }

    private static void tickEntity(LivingEntity entity) {
        ProgressData data = ProgressData.of(entity);
        long now = entity.level().getGameTime();
        long lastUpdate = data.furyLastUpdateGameTime();
        if (lastUpdate == Long.MIN_VALUE || lastUpdate > now) {
            lastUpdate = now;
        }
        long elapsedTicks = Math.max(0L, now - lastUpdate);
        double max = maxAnger(entity);
        double anger = SpecialTalentMath.clampAnger(data.furyAnger(), max);

        if (data.furyBerserk()) {
            if (elapsedTicks > 0L && anger > 0.0D) {
                anger = SpecialTalentMath.decayForTicks(
                        anger,
                        max,
                        SpecialTalentConfigManager.fury().berserkDecayPercentPerSecond(),
                        elapsedTicks
                );
            }
            data.furyAnger(anger);
            data.furyLastUpdateGameTime(now);
            if (anger <= 0.0D || max <= 0.0D) {
                exitBerserk(entity, data);
                return;
            }
            tickBerserkCombat(entity, now);
            return;
        }

        if (anger > 0.0D && elapsedTicks > 0L) {
            long calmDelayTicks = Math.max(0L, (long) SpecialTalentConfigManager.fury().calmDelaySeconds() * 20L);
            long lastDamage = data.furyLastDamageGameTime();
            long decayStart = lastDamage == Long.MIN_VALUE ? lastUpdate : lastDamage + calmDelayTicks;
            long effectiveStart = Math.max(lastUpdate, decayStart);
            long decayTicks = Math.max(0L, now - effectiveStart);
            if (decayTicks > 0L) {
                anger = SpecialTalentMath.decayForTicks(
                        anger,
                        max,
                        SpecialTalentConfigManager.fury().calmDecayPercentPerSecond(),
                        decayTicks
                );
                data.furyAnger(anger);
            }
        }
        data.furyLastUpdateGameTime(now);
    }

    private static void enterBerserk(LivingEntity entity, ProgressData data, double maxAnger) {
        data.furyAnger(maxAnger);
        data.furyBerserk(true);
        data.furyLastUpdateGameTime(entity.level().getGameTime());
        RideService.stopRide(entity, RideService.StopReason.FURY);
        entity.ejectPassengers();
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        tickBerserkCombat(entity, entity.level().getGameTime());
    }

    private static void exitBerserk(LivingEntity entity, ProgressData data) {
        data.furyAnger(0.0D);
        data.furyBerserk(false);
        data.furyLastUpdateGameTime(entity.level().getGameTime());
        clearBerserkTarget(entity);
    }

    private static void tickBerserkCombat(LivingEntity entity, long now) {
        if (!(entity instanceof Mob mob) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (TorporData.exists(entity) && TorporData.of(entity).unconscious()) {
            mob.getNavigation().stop();
            return;
        }

        double radius = Math.max(1.0D, SpecialTalentConfigManager.fury().targetRadius());
        LivingEntity current = resolveTrackedTarget(level, entity, radius);
        if (current == null || now % TARGET_RESCAN_TICKS == 0L) {
            LivingEntity nearest = nearestTarget(level, entity, radius);
            if (nearest != null) {
                current = nearest;
                setBerserkTarget(mob, nearest);
            } else if (current == null) {
                clearBerserkTarget(entity);
                mob.getNavigation().stop();
                return;
            }
        }

        if (current == null) {
            return;
        }
        setBerserkTarget(mob, current);
        mob.getNavigation().moveTo(current, 1.2D);
        forcedAttackIfNeeded(mob, current, now);
    }

    private static LivingEntity nearestTarget(ServerLevel level, LivingEntity entity, double radius) {
        AABB area = entity.getBoundingBox().inflate(radius);
        return level.getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        candidate -> validTarget(entity, candidate, radius)
                ).stream()
                .min(Comparator.comparingDouble(entity::distanceToSqr))
                .orElse(null);
    }

    private static LivingEntity resolveTrackedTarget(ServerLevel level, LivingEntity entity, double radius) {
        UUID targetUuid = BERSERK_TARGETS.get(entity);
        if (targetUuid == null) {
            return null;
        }
        net.minecraft.world.entity.Entity raw = level.getEntity(targetUuid);
        if (!(raw instanceof LivingEntity target) || !validTarget(entity, target, radius)) {
            BERSERK_TARGETS.remove(entity);
            return null;
        }
        return target;
    }

    private static boolean validTarget(LivingEntity source, LivingEntity target, double radius) {
        if (source == target || target == null || !target.isAlive() || target.isRemoved()) {
            return false;
        }
        if (target instanceof Player player && (player.isSpectator() || player.isCreative())) {
            return false;
        }
        return source.distanceToSqr(target) <= radius * radius;
    }

    private static void setBerserkTarget(Mob mob, LivingEntity target) {
        BERSERK_TARGETS.put(mob, target.getUUID());
        mob.setLastHurtByMob(target);
        mob.setTarget(target);
        if (StandardPetCapabilities.isMemoryRegistered(mob, MemoryModuleType.ATTACK_TARGET)) {
            mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        }
    }

    private static void clearBerserkTarget(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        UUID furyTarget = BERSERK_TARGETS.remove(entity);
        LAST_FORCED_ATTACK.remove(entity);
        if (!(entity instanceof Mob mob) || furyTarget == null) {
            return;
        }
        LivingEntity current = mob.getTarget();
        if (current != null && furyTarget.equals(current.getUUID())) {
            mob.setTarget(null);
        }
        LivingEntity memoryTarget = StandardPetCapabilities.registeredAttackTarget(mob);
        if (memoryTarget != null && furyTarget.equals(memoryTarget.getUUID())) {
            StandardPetCapabilities.eraseRegisteredMemory(mob, MemoryModuleType.ATTACK_TARGET);
        }
        mob.getNavigation().stop();
    }

    private static void forcedAttackIfNeeded(Mob mob, LivingEntity target, long now) {
        long last = LAST_FORCED_ATTACK.getOrDefault(mob, Long.MIN_VALUE / 2L);
        if (now - last < FORCED_ATTACK_COOLDOWN_TICKS) {
            return;
        }
        double reach = mob.getBbWidth() * 0.5D + target.getBbWidth() * 0.5D + 1.5D;
        if (mob.distanceToSqr(target) > reach * reach || !mob.hasLineOfSight(target)) {
            return;
        }
        if (mob.doHurtTarget(target)) {
            LAST_FORCED_ATTACK.put(mob, now);
        }
    }
}
