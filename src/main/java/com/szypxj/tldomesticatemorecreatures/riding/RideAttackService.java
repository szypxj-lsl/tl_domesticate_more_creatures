package com.szypxj.tldomesticatemorecreatures.riding;

import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionApi;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.talent.active.ActiveTalentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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

public final class RideAttackService {
    private static final double AIM_RANGE = 6.0D;
    private static final int DEFAULT_ATTACK_INTERVAL_TICKS = 10;
    private static final Map<UUID, Integer> LAST_SEQUENCE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_ATTACK_GAME_TIME = new ConcurrentHashMap<>();

    private RideAttackService() {
    }

    public static void acceptAttack(ServerPlayer rider, int mountEntityId, int sequence) {
        if (rider == null || sequence < 0 || !(rider.getVehicle() instanceof Mob mount) || mount.getId() != mountEntityId) return;
        if (!RideService.isGenericRider(rider)) return;
        if (ActiveTalentService.blocksRideInput(mount)) return;
        if (!RideActionApi.allows(rider, mount, RideAction.ATTACK)) return;

        int lastSequence = LAST_SEQUENCE.getOrDefault(rider.getUUID(), Integer.MIN_VALUE);
        if (sequence <= lastSequence) return;
        LAST_SEQUENCE.put(rider.getUUID(), sequence);

        long now = mount.level().getGameTime();
        if (now < NEXT_ATTACK_GAME_TIME.getOrDefault(rider.getUUID(), 0L)) return;
        LivingEntity target = raycastTarget(rider, mount);
        if (target == null || !withinAttackRange(mount, target)) return;

        mount.getLookControl().setLookAt(target, 180.0F, 180.0F);
        mount.doHurtTarget(target);
        NEXT_ATTACK_GAME_TIME.put(rider.getUUID(), now + attackIntervalTicks(mount));
    }

    public static void clearPlayer(ServerPlayer rider) {
        if (rider == null) return;
        LAST_SEQUENCE.remove(rider.getUUID());
        NEXT_ATTACK_GAME_TIME.remove(rider.getUUID());
    }

    public static void clearAll() {
        LAST_SEQUENCE.clear();
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

    private static int attackIntervalTicks(Mob mount) {
        AttributeInstance attackSpeed = mount.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) return DEFAULT_ATTACK_INTERVAL_TICKS;
        double value = attackSpeed.getValue();
        if (!Double.isFinite(value) || value <= 0.0D) return DEFAULT_ATTACK_INTERVAL_TICKS;
        return Mth.clamp((int) Math.ceil(20.0D / value), 5, 20);
    }

    private record Candidate(LivingEntity entity, Optional<Vec3> hit) {
    }
}
