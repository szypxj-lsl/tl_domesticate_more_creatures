package com.szypxj.tldomesticatemorecreatures.petmanagement;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PetManagementSavedData extends SavedData {
    private static final String DATA_NAME = TlDomesticateMoreCreatures.MOD_ID + "_pet_management";
    private static final String RECORDS = "records";

    private final Map<UUID, PetRecord> recordsByPet = new HashMap<>();
    private final Map<UUID, Set<UUID>> petsByOwner = new HashMap<>();

    public static PetManagementSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                PetManagementSavedData::load,
                PetManagementSavedData::new,
                DATA_NAME
        );
    }

    public PetRecord record(UUID petUuid) {
        return petUuid == null ? null : recordsByPet.get(petUuid);
    }

    public List<PetRecord> recordsFor(UUID ownerUuid) {
        if (ownerUuid == null) return List.of();
        Set<UUID> ids = petsByOwner.get(ownerUuid);
        if (ids == null || ids.isEmpty()) return List.of();
        List<PetRecord> result = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            PetRecord record = recordsByPet.get(id);
            if (record != null && ownerUuid.equals(record.ownerUuid())) result.add(record);
        }
        result.sort((left, right) -> {
            int nameOrder = String.CASE_INSENSITIVE_ORDER.compare(left.displayName(), right.displayName());
            if (nameOrder != 0) return nameOrder;
            return left.petUuid().compareTo(right.petUuid());
        });
        return List.copyOf(result);
    }

    public void put(PetRecord record) {
        if (record == null || record.petUuid() == null || record.ownerUuid() == null) return;
        removeFromOtherOwners(record.petUuid(), record.ownerUuid());
        recordsByPet.put(record.petUuid(), record);
        petsByOwner.computeIfAbsent(record.ownerUuid(), ignored -> new LinkedHashSet<>()).add(record.petUuid());
        setDirty();
    }

    private void removeFromOtherOwners(UUID petUuid, UUID currentOwner) {
        for (Map.Entry<UUID, Set<UUID>> entry : new ArrayList<>(petsByOwner.entrySet())) {
            if (entry.getKey().equals(currentOwner)) continue;
            Set<UUID> ids = entry.getValue();
            ids.remove(petUuid);
            if (ids.isEmpty()) petsByOwner.remove(entry.getKey());
        }
    }

    public boolean removeRecord(UUID ownerUuid, UUID petUuid) {
        PetRecord record = recordsByPet.get(petUuid);
        if (record == null || !ownerUuid.equals(record.ownerUuid())) return false;
        recordsByPet.remove(petUuid);
        Set<UUID> ids = petsByOwner.get(ownerUuid);
        if (ids != null) {
            ids.remove(petUuid);
            if (ids.isEmpty()) petsByOwner.remove(ownerUuid);
        }
        setDirty();
        return true;
    }

    public boolean setShortcut(UUID ownerUuid, UUID petUuid, int slot) {
        if (slot < 0 || slot > 9) return false;
        PetRecord target = recordsByPet.get(petUuid);
        if (target == null || !ownerUuid.equals(target.ownerUuid())) return false;
        for (PetRecord record : recordsFor(ownerUuid)) {
            if (record.petUuid().equals(petUuid)) continue;
            if (slot != 0 && record.shortcutSlot() == slot) record.shortcutSlot(0);
        }
        target.shortcutSlot(slot);
        setDirty();
        return true;
    }

    public PetRecord shortcut(UUID ownerUuid, int slot) {
        if (ownerUuid == null || slot < 1 || slot > 9) return null;
        for (PetRecord record : recordsFor(ownerUuid)) {
            if (record.shortcutSlot() == slot) return record;
        }
        return null;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        Collection<PetRecord> values = recordsByPet.values();
        for (PetRecord record : values) list.add(record.save());
        tag.put(RECORDS, list);
        return tag;
    }

    public static PetManagementSavedData load(CompoundTag tag) {
        PetManagementSavedData data = new PetManagementSavedData();
        ListTag list = tag.getList(RECORDS, Tag.TAG_COMPOUND);
        for (Tag raw : list) {
            if (!(raw instanceof CompoundTag compound)) continue;
            PetRecord record = PetRecord.load(compound);
            if (record == null || record.ownerUuid() == null) continue;
            data.recordsByPet.put(record.petUuid(), record);
            data.petsByOwner.computeIfAbsent(record.ownerUuid(), ignored -> new LinkedHashSet<>()).add(record.petUuid());
        }
        return data;
    }
}
