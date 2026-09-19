package com.szypxj.tldomesticatemorecreatures.riding;

import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionStatus;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared implementation of TDMC's generic mounted melee attack.
 *
 * <p>Input validation and provider routing live in RideControlDispatcher. This
 * class only resolves the aimed target, invokes the mount's own attack entry
 * point and tracks the generic attack cooldown.</p>
 */
public final class RideAttackService {
    private static final double AIM_RANGE = 6.0D;
    private static final int DEFAULT_ATTACK_INTERVAL_TICKS = 10;
    private static final Map<UUID, Long> NEXT_ATTACK_GAME_TIME = new ConcurrentHashMap<>();

    private RideAttackService() {
    }

    public static BasicAttackOutcome performBasicAttack(ServerPlayer rider, Mob mount) {
        if (rider == null || mount == null || rider.getVehicle() != mount || !mount.isAlive() || mount.isRemoved()) {
            return BasicAttackOutcome.INVALID;
        }
        long now = mount.level().getGameTime();
        if (now < NEXT_ATTACK_GAME_TIME.getOrDefault(rider.getUUID(), 0L)) {
            return BasicAttackOutcome.COOLDOWN;
        }

        LivingEntity target = raycastTarget(rider, mount);
        if (target == null || !withinAttackRange(mount, target)) {
            return BasicAttackOutcome.NO_TARGET;
        }

        mount.getLookControl().setLookAt(target, 180.0F, 180.0F);
        if (!mount.doHurtTarget(target)) {
            return BasicAttackOutcome.NO_TARGET;
        }
        mount.swing(InteractionHand.MAIN_HAND, true);
        NEXT_ATTACK_GAME_TIME.put(rider.getUUID(), now + attackIntervalTicks(mount));
        return BasicAttackOutcome.HIT;
    }

    public static RideActionStatus attackStatus(ServerPlayer rider, Mob mount) {
        if (rider == null || mount == null) {
            return RideActionStatus.BLOCKED;
        }
        long now = mount.level().getGameTime();
        long next = NEXT_ATTACK_GAME_TIME.getOrDefault(rider.getUUID(), 0L);
        int remaining = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, next - now));
        return remaining <= 0
                ? RideActionStatus.READY
                : RideActionStatus.cooldown(remaining, attackIntervalTicks(mount));
    }

    public static int attackIntervalTicks(Mob mount) {
        if (mount == null) {
            return DEFAULT_ATTACK_INTERVAL_TICKS;
        }
        AttributeInstance attackSpeed = mount.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) return DEFAULT_ATTACK_INTERVAL_TICKS;
        double value = attackSpeed.getValue();
        if (!Double.isFinite(value) || value <= 0.0D) return DEFAULT_ATTACK_INTERVAL_TICKS;
        return Mth.clamp((int) Math.ceil(20.0D / value), 5, 20);
    }

    public static void clearPlayer(ServerPlayer rider) {
        if (rider != null) {
            NEXT_ATTACK_GAME_TIME.remove(rider.getUUID());
        }
    }

    public static void clearAll() {
        NEXT_ATTACK_GAME_TIME.clear();
    }

    private static LivingEntity raycastTarget(ServerPlayer rider, Mob mount) {
        Vec3 start = rider.getEyePosition();
        Vec3 look = rider.getViewVector(1.0F);
        Vec3 requestedEnd = start.add(look.scale(AIM_RANGE));
        HitResult blockHit = rider.pick(AIM_RANGE, 1.0F, false);
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? requestedEnd : blockHit.getLocation();
        AABB area = rider.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        return rider.level().getEntitiesOfClass(LivingEntity.class, area, target -> isValidTarget(rider, mount, target))
                .stream()
                .map(target -> new Candidate(target, target.getBoundingBox().inflate(target.getPickRadius() + 0.25D).clip(start, end)))
                .filter(candidate -> candidate.hit().isPresent())
                .filter(candidate -> rider.hasLineOfSight(candidate.entity()))
                .min(Comparator.comparingDouble(candidate -> candidate.hit().get().distanceToSqr(start)))
                .map(Candidate::entity)
                .orElse(null);
    }

    private static boolean isValidTarget(ServerPlayer rider, Mob mount, LivingEntity target) {
        if (target == null || target == rider || target == mount || !target.isAlive() || target.isRemoved()) return false;
        UUID ownerUuid = PetOwnershipService.ownerUuid(mount).orElse(null);
        if (ownerUuid != null && ownerUuid.equals(target.getUUID())) return false;
        if (PetOwnershipService.haveSameOwner(mount, target)) return false;
        if (target instanceof Player playerTarget && !rider.canHarmPlayer(playerTarget)) return false;
        return true;
    }

    private static boolean withinAttackRange(Mob mount, LivingEntity target) {
        double reach = 4.0D + (mount.getBbWidth() + target.getBbWidth()) * 0.5D;
        return mount.distanceToSqr(target) <= reach * reach && mount.hasLineOfSight(target);
    }

    public enum BasicAttackOutcome {
        HIT,
        NO_TARGET,
        COOLDOWN,
        INVALID
    }

    private record Candidate(LivingEntity entity, Optional<Vec3> hit) {
    }
}
