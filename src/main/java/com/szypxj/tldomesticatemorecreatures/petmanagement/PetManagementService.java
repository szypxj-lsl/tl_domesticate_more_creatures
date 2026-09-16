package com.szypxj.tldomesticatemorecreatures.petmanagement;

import com.mojang.logging.LogUtils;
import com.szypxj.tldomesticatemorecreatures.config.Config;
import com.szypxj.tldomesticatemorecreatures.data.ProgressData;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.riding.RideEligibilityResolver;
import com.szypxj.tldomesticatemorecreatures.riding.RideEligibilityResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PetManagementService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LAST_HURT_GAME_TIME = "tdmcPetManagementLastHurtGameTime";
    private static final Map<UUID, WeakReference<LivingEntity>> TRACKED = new ConcurrentHashMap<>();

    private PetManagementService() {
    }

    public static int storageCapacity(ServerPlayer player) {
        int level = ProgressData.of(player).level();
        long capacity = (long) Config.PET_MANAGEMENT_BASE_STORAGE_CAPACITY.get()
                + (long) level * Config.PET_MANAGEMENT_STORAGE_CAPACITY_PER_PLAYER_LEVEL.get();
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, capacity));
    }

    public static int storedCount(ServerPlayer player) {
        int count = 0;
        for (PetRecord record : recordsFor(player)) {
            if (record.state() == PetRecordState.STORED) count++;
        }
        return count;
    }

    public static List<PetRecord> recordsFor(ServerPlayer player) {
        if (player == null) return List.of();
        PetManagementSavedData savedData = data(player.server);
        purgeInvalidOwnershipRecords(savedData, player);
        return savedData.recordsFor(player.getUUID());
    }

    public static PetRecord record(MinecraftServer server, UUID petUuid) {
        return server == null ? null : data(server).record(petUuid);
    }

    public static PetRecord track(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide || entity.getServer() == null) return null;
        PetManagementSavedData data = data(entity.getServer());
        PetRecord record = data.record(entity.getUUID());
        UUID ownerUuid = PetOwnershipService.ownerUuid(entity).orElse(null);
        ServerPlayer owner = ownerUuid == null ? null : entity.getServer().getPlayerList().getPlayer(ownerUuid);
        if (ownerUuid == null
                || !PetOwnershipService.isManagedPet(entity)
                || (owner != null && !PetOwnershipService.isOwnedBy(entity, owner))
                || !isRecordablePetType(entity.getType())) {
            removeReleasedWorldRecord(entity, data, record);
            return null;
        }
        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (typeId == null) {
            removeReleasedWorldRecord(entity, data, record);
            return null;
        }
        TRACKED.put(entity.getUUID(), new WeakReference<>(entity));
        if (record != null && record.state() == PetRecordState.DEAD) {
            if (!entity.isAlive()) return record;
            record.deadEntityTag(null);
            record.state(PetRecordState.WORLD);
        }
        if (record != null && record.state() == PetRecordState.STORED) {
            record.storedEntityTag(null);
            record.state(PetRecordState.WORLD);
        }
        if (record == null) record = new PetRecord(entity.getUUID(), ownerUuid, typeId, entity.getName().getString());
        UUID previousOwner = record.ownerUuid();
        if (previousOwner != null && !previousOwner.equals(ownerUuid)) record.shortcutSlot(0);
        record.ownerUuid(ownerUuid);
        record.entityTypeId(typeId);
        record.displayName(entity.getName().getString());
        record.level(ProgressData.exists(entity) ? ProgressData.of(entity).level() : 1);
        if (record.state() != PetRecordState.SUMMONING) record.state(PetRecordState.WORLD);
        if (owner != null) {
            record.rideable(RideEligibilityResolver.evaluate(entity, owner) != RideEligibilityResult.DENY);
        }
        record.lastLocation(entity.level().dimension().location(), entity.getX(), entity.getY(), entity.getZ(), entity.level().getGameTime());
        data.put(record);
        return record;
    }

    public static void refreshTracked(MinecraftServer server) {
        if (server == null || server.getTickCount() % 100 != 0 || TRACKED.isEmpty()) return;
        Iterator<Map.Entry<UUID, WeakReference<LivingEntity>>> iterator = TRACKED.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, WeakReference<LivingEntity>> entry = iterator.next();
            LivingEntity entity = entry.getValue().get();
            if (entity == null || entity.isRemoved() || !entity.isAlive() || entity.getServer() != server) {
                TRACKED.remove(entry.getKey());
                continue;
            }
            if (PetOwnershipService.ownerUuid(entity).isPresent()) {
                track(entity);
            } else {
                PetManagementSavedData data = data(server);
                removeReleasedWorldRecord(entity, data, data.record(entity.getUUID()));
            }
        }
    }

    public static void reconcileOwnership(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide || entity.getServer() == null) return;
        PetManagementSavedData data = data(entity.getServer());
        PetRecord record = data.record(entity.getUUID());
        if (record == null) return;

        UUID ownerUuid = PetOwnershipService.ownerUuid(entity).orElse(null);
        ServerPlayer owner = ownerUuid == null ? null : entity.getServer().getPlayerList().getPlayer(ownerUuid);
        if (ownerUuid != null
                && ownerUuid.equals(record.ownerUuid())
                && PetOwnershipService.isManagedPet(entity)
                && (owner == null || PetOwnershipService.isOwnedBy(entity, owner))) {
            track(entity);
            return;
        }
        removeReleasedWorldRecord(entity, data, record);
    }

    private static void removeReleasedWorldRecord(LivingEntity entity, PetManagementSavedData data, PetRecord record) {
        if (entity == null || data == null || record == null) return;
        if (record.state() == PetRecordState.STORED || record.state() == PetRecordState.DEAD) return;
        UUID previousOwner = record.ownerUuid();
        record.shortcutSlot(0);
        if (previousOwner != null) {
            data.removeRecord(previousOwner, entity.getUUID());
            ServerPlayer owner = entity.getServer().getPlayerList().getPlayer(previousOwner);
            if (owner != null) NetworkHandler.sendPetManagementList(owner);
        }
        TRACKED.remove(entity.getUUID());
    }

    private static boolean isRecordablePetType(EntityType<?> type) {
        return type != null && type.canSerialize() && type.canSummon();
    }

    private static void purgeInvalidOwnershipRecords(PetManagementSavedData data, ServerPlayer player) {
        if (data == null || player == null) return;
        UUID ownerUuid = player.getUUID();
        for (PetRecord record : data.recordsFor(ownerUuid)) {
            if (record == null) continue;
            if (record.state() == PetRecordState.STORED || record.state() == PetRecordState.DEAD) continue;

            ResourceLocation typeId = record.entityTypeId();
            EntityType<?> type = typeId == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(typeId);
            if (!isRecordablePetType(type) || record.state() == PetRecordState.UNLOCATED) {
                removeInvalidRecord(data, ownerUuid, record.petUuid());
                continue;
            }

            LivingEntity entity = findLoadedEntity(player.server, record.petUuid());
            if (entity == null) continue;
            if (!PetOwnershipService.isManagedPet(entity)
                    || !ownerUuid.equals(PetOwnershipService.ownerUuid(entity).orElse(null))
                    || !PetOwnershipService.isOwnedBy(entity, player)) {
                removeInvalidRecord(data, ownerUuid, record.petUuid());
            }
        }
    }

    private static LivingEntity findLoadedEntity(MinecraftServer server, UUID petUuid) {
        if (server == null || petUuid == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(petUuid);
            if (entity instanceof LivingEntity living) return living;
        }
        return null;
    }

    private static void removeInvalidRecord(PetManagementSavedData data, UUID ownerUuid, UUID petUuid) {
        if (data == null || ownerUuid == null || petUuid == null) return;
        PetRecord record = data.record(petUuid);
        if (record != null) record.shortcutSlot(0);
        data.removeRecord(ownerUuid, petUuid);
        TRACKED.remove(petUuid);
    }

    public static void recordHurt(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) return;
        entity.getPersistentData().putLong(LAST_HURT_GAME_TIME, entity.level().getGameTime());
    }

    public static boolean recentlyHurt(LivingEntity entity) {
        if (entity == null) return false;
        long last = entity.getPersistentData().getLong(LAST_HURT_GAME_TIME);
        long cooldown = Config.PET_MANAGEMENT_REMOTE_STORE_COMBAT_COOLDOWN_SECONDS.get() * 20L;
        return last > 0L && entity.level().getGameTime() - last < cooldown;
    }

    public static boolean inCombat(LivingEntity entity) {
        if (recentlyHurt(entity)) return true;
        if (entity instanceof Mob mob) {
            LivingEntity target = mob.getTarget();
            return target != null && target.isAlive();
        }
        return false;
    }

    public static boolean store(ServerPlayer player, UUID petUuid) {
        if (player == null || petUuid == null) return false;
        PetRecord record = data(player.server).record(petUuid);
        if (record == null || !player.getUUID().equals(record.ownerUuid()) || record.state() == PetRecordState.DEAD) return false;
        if (record.state() == PetRecordState.STORED) return true;
        if (storedCount(player) >= storageCapacity(player)) return false;
        LivingEntity entity = locate(player, petUuid, true);
        if (entity == null || !entity.isAlive() || !PetOwnershipService.isOwnedBy(entity, player)) return false;
        if (inCombat(entity)) return false;
        if (record.state() == PetRecordState.SUMMONING) return false;

        if (entity.isPassenger()) entity.stopRiding();
        if (!entity.getPassengers().isEmpty()) entity.ejectPassengers();

        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (typeId == null) return false;
        CompoundTag snapshot = new CompoundTag();
        entity.saveWithoutId(snapshot);
        snapshot.putString("id", typeId.toString());
        if (!snapshot.hasUUID("UUID")) snapshot.putUUID("UUID", entity.getUUID());
        record.displayName(entity.getName().getString());
        record.level(ProgressData.exists(entity) ? ProgressData.of(entity).level() : record.level());
        record.rideable(RideEligibilityResolver.evaluate(entity, player) != RideEligibilityResult.DENY);
        record.storedEntityTag(snapshot);
        record.deadEntityTag(null);
        record.state(PetRecordState.STORED);
        record.lastLocation(entity.level().dimension().location(), entity.getX(), entity.getY(), entity.getZ(), entity.level().getGameTime());
        data(player.server).put(record);
        entity.discard();
        return true;
    }

    public static void markDead(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide || entity.getServer() == null) return;
        UUID ownerUuid = PetOwnershipService.ownerUuid(entity).orElse(null);
        if (ownerUuid == null) return;
        PetRecord record = track(entity);
        if (record == null) return;
        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        CompoundTag snapshot = new CompoundTag();
        entity.saveWithoutId(snapshot);
        if (typeId != null) snapshot.putString("id", typeId.toString());
        if (!snapshot.hasUUID("UUID")) snapshot.putUUID("UUID", entity.getUUID());
        record.deadEntityTag(snapshot);
        record.storedEntityTag(null);
        record.state(PetRecordState.DEAD);
        record.lastLocation(entity.level().dimension().location(), entity.getX(), entity.getY(), entity.getZ(), entity.level().getGameTime());
        data(entity.getServer()).put(record);
        ServerPlayer owner = entity.getServer().getPlayerList().getPlayer(ownerUuid);
        if (owner != null) NetworkHandler.sendPetManagementList(owner);
    }

    public static CompoundTag previewTag(ServerPlayer player, UUID petUuid) {
        if (player == null || petUuid == null) return new CompoundTag();
        PetRecord record = data(player.server).record(petUuid);
        if (record == null || !player.getUUID().equals(record.ownerUuid())) return new CompoundTag();
        CompoundTag stored = record.state() == PetRecordState.STORED ? record.storedEntityTag() : null;
        if (stored != null) return stored;
        CompoundTag dead = record.state() == PetRecordState.DEAD ? record.deadEntityTag() : null;
        if (dead != null) return dead;
        if (record.state() == PetRecordState.UNLOCATED) return new CompoundTag();
        LivingEntity entity = locate(player, petUuid, true);
        if (entity == null) return new CompoundTag();
        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        CompoundTag tag = new CompoundTag();
        entity.saveWithoutId(tag);
        if (typeId != null) tag.putString("id", typeId.toString());
        if (!tag.hasUUID("UUID")) tag.putUUID("UUID", entity.getUUID());
        return tag;
    }

    public static boolean removeDeathRecord(ServerPlayer player, UUID petUuid) {
        if (player == null || petUuid == null) return false;
        PetRecord record = data(player.server).record(petUuid);
        if (record == null || !player.getUUID().equals(record.ownerUuid()) || record.state() != PetRecordState.DEAD) return false;
        return data(player.server).removeRecord(player.getUUID(), petUuid);
    }

    public static boolean attemptLocate(ServerPlayer player, UUID petUuid) {
        if (player == null || petUuid == null) return false;
        PetRecord record = data(player.server).record(petUuid);
        if (record == null || !player.getUUID().equals(record.ownerUuid())) return false;
        if (record.state() == PetRecordState.STORED || record.state() == PetRecordState.DEAD) return false;
        LivingEntity entity = locate(player, petUuid, true);
        if (entity == null) return false;
        track(entity);
        return true;
    }

    public static LivingEntity locate(ServerPlayer player, UUID petUuid, boolean loadLastKnownChunk) {
        if (player == null || petUuid == null) return null;
        PetRecord record = data(player.server).record(petUuid);
        if (record == null || !player.getUUID().equals(record.ownerUuid())) return null;
        if (record.state() == PetRecordState.STORED || record.state() == PetRecordState.DEAD) return null;

        for (ServerLevel level : player.server.getAllLevels()) {
            Entity found = level.getEntity(petUuid);
            if (found instanceof LivingEntity living) {
                track(living);
                return living;
            }
        }
        if (!loadLastKnownChunk || record.lastDimension() == null) return null;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, record.lastDimension());
        ServerLevel lastLevel = player.server.getLevel(key);
        if (lastLevel == null) {
            record.state(PetRecordState.UNLOCATED);
            data(player.server).put(record);
            return null;
        }
        BlockPos pos = BlockPos.containing(record.lastX(), record.lastY(), record.lastZ());
        lastLevel.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        Entity found = lastLevel.getEntity(petUuid);
        if (found instanceof LivingEntity living) {
            track(living);
            return living;
        }
        record.state(PetRecordState.UNLOCATED);
        data(player.server).put(record);
        LOGGER.debug("Unable to locate managed pet {} at last known position {} {}", petUuid, record.lastDimension(), pos);
        return null;
    }

    public static boolean setShortcut(ServerPlayer player, UUID petUuid, int slot) {
        if (player == null || petUuid == null || slot < 0 || slot > 9) return false;
        PetRecord record = data(player.server).record(petUuid);
        if (record == null || !player.getUUID().equals(record.ownerUuid()) || record.state() == PetRecordState.DEAD || !record.rideable()) return false;
        return data(player.server).setShortcut(player.getUUID(), petUuid, slot);
    }

    public static PetRecord shortcut(ServerPlayer player, int slot) {
        if (player == null) return null;
        return data(player.server).shortcut(player.getUUID(), slot);
    }

    public static void saveRecord(MinecraftServer server, PetRecord record) {
        if (server != null && record != null) data(server).put(record);
    }

    static PetManagementSavedData data(MinecraftServer server) {
        return PetManagementSavedData.get(server);
    }
}
