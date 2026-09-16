package com.szypxj.tldomesticatemorecreatures.petmanagement.summon;

import com.szypxj.tldomesticatemorecreatures.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PetPlacementFinder {
    private static final double RESCUE_PREDICTION_TICKS = 6.0D;

    private PetPlacementFinder() {
    }

    public static Vec3 findGroundBehind(ServerPlayer player, LivingEntity pet) {
        int min = Math.max(4, Config.PET_MANAGEMENT_ENTRY_MIN_DISTANCE.get());
        int max = Math.max(min, Config.PET_MANAGEMENT_ENTRY_MAX_DISTANCE.get());
        Vec3 look = horizontalLook(player);
        for (int distance = max; distance >= min; distance -= 2) {
            Vec3 base = player.position().subtract(look.scale(distance));
            Vec3 found = findGroundAt(player.serverLevel(), pet, BlockPos.containing(base));
            if (found != null) return found;
        }
        return findGroundAt(player.serverLevel(), pet, player.blockPosition());
    }

    public static Vec3 findGroundBelowPlayer(ServerPlayer player, LivingEntity pet) {
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        for (int radius = 0; radius <= 8; radius += 2) {
            int step = Math.max(1, radius);
            for (int dx = -radius; dx <= radius; dx += step) {
                for (int dz = -radius; dz <= radius; dz += step) {
                    Vec3 found = findGroundAt(level, pet, origin.offset(dx, 0, dz));
                    if (found != null) return found;
                }
            }
        }
        return null;
    }


    public static double findContinuousGroundY(ServerLevel level, LivingEntity pet, double x, double currentY, double z) {
        if (level == null || pet == null) return Double.NaN;
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        int centerY = Mth.floor(currentY);
        int minY = Math.max(level.getMinBuildHeight() + 1, centerY - 6);
        int maxY = Math.min(level.getMaxBuildHeight() - 2, centerY + 4);
        for (int y = maxY; y >= minY; y--) {
            BlockPos floor = new BlockPos(blockX, y - 1, blockZ);
            BlockPos feet = new BlockPos(blockX, y, blockZ);
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) continue;
            if (level.getFluidState(feet).is(Fluids.LAVA) || level.getFluidState(floor).is(Fluids.LAVA)) continue;
            return y;
        }
        return currentY;
    }

    public static Vec3 findAirBehind(ServerPlayer player, LivingEntity pet) {
        int min = Math.max(4, Config.PET_MANAGEMENT_ENTRY_MIN_DISTANCE.get());
        int max = Math.max(min, Config.PET_MANAGEMENT_ENTRY_MAX_DISTANCE.get());
        return findAirBehind(player, pet, min, max);
    }

    public static Vec3 findEmergencyIntercept(ServerPlayer player, LivingEntity pet) {
        if (player == null || pet == null) return null;
        ServerLevel level = player.serverLevel();
        Vec3 velocity = player.getDeltaMovement();
        double ticks = RESCUE_PREDICTION_TICKS;
        double predictedX = player.getX() + velocity.x * ticks;
        double predictedY = player.getY() + velocity.y * ticks - 0.04D * ticks * ticks;
        double predictedZ = player.getZ() + velocity.z * ticks;
        predictedY = Mth.clamp(predictedY, level.getMinBuildHeight() + 2.0D, level.getMaxBuildHeight() - 3.0D);

        double below = Mth.clamp(0.65D + pet.getBbHeight() * 0.30D, 0.9D, 2.75D);
        Vec3 center = new Vec3(predictedX, predictedY - below, predictedZ);
        double[] lateral = {0.0D, 1.25D, -1.25D, 2.5D, -2.5D};
        int[] vertical = {0, 1, -1, 2, -2, 3, -3};
        for (int dy : vertical) {
            for (double dx : lateral) {
                for (double dz : lateral) {
                    if (dx != 0.0D && dz != 0.0D && Math.abs(dx) != Math.abs(dz)) continue;
                    Vec3 candidate = center.add(dx, dy, dz);
                    if (isCollisionFree(level, pet, candidate)) return candidate;
                }
            }
        }

        Vec3 currentFallback = player.position().add(0.0D, -below, 0.0D);
        if (isCollisionFree(level, pet, currentFallback)) return currentFallback;
        return findGroundBelowPlayer(player, pet);
    }

    private static Vec3 findAirBehind(ServerPlayer player, LivingEntity pet, int min, int max) {
        ServerLevel level = player.serverLevel();
        Vec3 look = horizontalLook(player);
        for (int distance = max; distance >= min; distance -= 2) {
            Vec3 center = player.position().subtract(look.scale(distance)).add(0.0D, -1.0D, 0.0D);
            for (int dy = 2; dy >= -4; dy--) {
                Vec3 candidate = center.add(0.0D, dy, 0.0D);
                if (isCollisionFree(level, pet, candidate)) return candidate;
            }
        }
        Vec3 fallback = player.position().subtract(look.scale(Math.max(4, min))).add(0.0D, -1.0D, 0.0D);
        return isCollisionFree(level, pet, fallback) ? fallback : null;
    }


    private static Vec3 findGroundNearY(ServerLevel level, LivingEntity pet, BlockPos around, int centerY) {
        int minY = Math.max(level.getMinBuildHeight() + 1, centerY - 6);
        int maxY = Math.min(level.getMaxBuildHeight() - 2, centerY + 4);
        for (int y = maxY; y >= minY; y--) {
            BlockPos floor = new BlockPos(around.getX(), y - 1, around.getZ());
            BlockPos feet = new BlockPos(around.getX(), y, around.getZ());
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) continue;
            if (level.getFluidState(feet).is(Fluids.LAVA) || level.getFluidState(floor).is(Fluids.LAVA)) continue;
            Vec3 candidate = new Vec3(around.getX() + 0.5D, y, around.getZ() + 0.5D);
            if (isCollisionFree(level, pet, candidate)) return candidate;
        }
        return null;
    }

    private static Vec3 findGroundAt(ServerLevel level, LivingEntity pet, BlockPos around) {
        int x = around.getX();
        int z = around.getZ();
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int minY = level.getMinBuildHeight() + 1;
        int startY = Math.min(level.getMaxBuildHeight() - 2, Math.max(minY, top + 2));
        for (int y = startY; y >= minY; y--) {
            BlockPos floor = new BlockPos(x, y - 1, z);
            BlockPos feet = new BlockPos(x, y, z);
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) continue;
            if (level.getFluidState(feet).is(Fluids.LAVA) || level.getFluidState(floor).is(Fluids.LAVA)) continue;
            Vec3 candidate = new Vec3(x + 0.5D, y, z + 0.5D);
            if (isCollisionFree(level, pet, candidate)) return candidate;
        }
        return null;
    }

    private static boolean isCollisionFree(ServerLevel level, LivingEntity pet, Vec3 candidate) {
        AABB moved = pet.getBoundingBox().move(candidate.subtract(pet.position()));
        return level.noCollision(pet, moved);
    }

    private static Vec3 horizontalLook(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-6D) return new Vec3(0.0D, 0.0D, 1.0D);
        return horizontal.normalize();
    }
}
