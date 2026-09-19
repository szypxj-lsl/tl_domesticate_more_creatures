package com.szypxj.tldomesticatemorecreatures.game.genetics;

import com.szypxj.tldomesticatemorecreatures.TlDomesticateMoreCreatures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Persistent genetics sidecar for third-party egg blocks that do not have a BlockEntity. */
public final class GeneticBlockPosStore extends SavedData {
    private static final String DATA_NAME = TlDomesticateMoreCreatures.MOD_ID + "_genetic_block_positions";
    private static final String ENTRIES = "entries";
    private final Map<Long, CompoundTag> payloads = new HashMap<>();

    public static GeneticBlockPosStore get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(GeneticBlockPosStore::load, GeneticBlockPosStore::new, DATA_NAME);
    }

    public Optional<HatchGeneticPayload> get(BlockPos pos) {
        CompoundTag tag = pos == null ? null : payloads.get(pos.asLong());
        return tag == null ? Optional.empty() : GeneticPayloadCodec.decode(tag);
    }

    public void put(BlockPos pos, HatchGeneticPayload payload) {
        if (pos == null || payload == null) return;
        payloads.put(pos.asLong(), GeneticPayloadCodec.encode(payload));
        setDirty();
    }

    public void remove(BlockPos pos) {
        if (pos != null && payloads.remove(pos.asLong()) != null) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, CompoundTag> entry : payloads.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putLong("pos", entry.getKey());
            row.put("payload", entry.getValue().copy());
            list.add(row);
        }
        tag.put(ENTRIES, list);
        return tag;
    }

    private static GeneticBlockPosStore load(CompoundTag tag) {
        GeneticBlockPosStore data = new GeneticBlockPosStore();
        ListTag list = tag.getList(ENTRIES, Tag.TAG_COMPOUND);
        for (Tag raw : list) {
            if (!(raw instanceof CompoundTag row) || !row.contains("payload", Tag.TAG_COMPOUND)) continue;
            CompoundTag payload = row.getCompound("payload");
            if (GeneticPayloadCodec.decode(payload).isPresent()) data.payloads.put(row.getLong("pos"), payload.copy());
        }
        return data;
    }
}
