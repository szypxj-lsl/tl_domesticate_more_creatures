package com.szypxj.tldomesticatemorecreatures.petmanagement.summon;

import com.szypxj.tldomesticatemorecreatures.petmanagement.PetManagementService;
import com.szypxj.tldomesticatemorecreatures.petmanagement.PetRecord;
import com.szypxj.tldomesticatemorecreatures.petmanagement.PetRecordState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

public final class PetEntityTransferService {
    private PetEntityTransferService() {
    }

    public static LivingEntity restoreStored(ServerLevel level, PetRecord record, Vec3 position) {
        if (level == null || record == null || record.state() != PetRecordState.STORED || position == null) return null;
        CompoundTag stored = record.storedEntityTag();
        if (stored == null || stored.isEmpty()) return null;
        Entity loaded = EntityType.loadEntityRecursive(stored, level, entity -> entity);
        if (!(loaded instanceof LivingEntity living)) return null;
        living.moveTo(position.x, position.y, position.z, living.getYRot(), living.getXRot());
        living.setDeltaMovement(Vec3.ZERO);
        living.fallDistance = 0.0F;
        if (!level.addFreshEntity(living)) return null;
        record.storedEntityTag(null);
        record.state(PetRecordState.SUMMONING);
        record.lastLocation(level.dimension().location(), living.getX(), living.getY(), living.getZ(), level.getGameTime());
        PetManagementService.saveRecord(level.getServer(), record);
        return living;
    }

    public static Vec3 previewForPlacement(ServerLevel level, PetRecord record, Function<LivingEntity, Vec3> finder) {
        if (level == null || record == null || finder == null) return null;
        CompoundTag stored = record.storedEntityTag();
        if (stored == null || stored.isEmpty()) return null;
        Entity loaded = EntityType.loadEntityRecursive(stored, level, entity -> entity);
        if (!(loaded instanceof LivingEntity living)) return null;
        return finder.apply(living);
    }

    public static LivingEntity transferTo(LivingEntity entity, ServerLevel targetLevel, Vec3 position) {
        if (entity == null || targetLevel == null || position == null) return null;
        if (entity.level() == targetLevel) {
            entity.teleportTo(position.x, position.y, position.z);
            entity.setDeltaMovement(Vec3.ZERO);
            entity.fallDistance = 0.0F;
            return entity;
        }
        Entity moved = entity.changeDimension(targetLevel);
        if (!(moved instanceof LivingEntity living)) return null;
        living.teleportTo(position.x, position.y, position.z);
        living.setDeltaMovement(Vec3.ZERO);
        living.fallDistance = 0.0F;
        return living;
    }
}
