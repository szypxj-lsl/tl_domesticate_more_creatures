package com.szypxj.tldomesticatemorecreatures.game.genetics;

import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

public final class GeneticBlockCarrierData {
    private GeneticBlockCarrierData() {
    }

    public static Optional<HatchGeneticPayload> get(BlockEntity blockEntity) {
        if (blockEntity == null || !blockEntity.getPersistentData().contains(GeneticPayloadCodec.ROOT_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        return GeneticPayloadCodec.decode(blockEntity.getPersistentData().getCompound(GeneticPayloadCodec.ROOT_KEY));
    }

    public static void set(BlockEntity blockEntity, HatchGeneticPayload payload) {
        if (blockEntity == null || payload == null) {
            return;
        }
        blockEntity.getPersistentData().put(GeneticPayloadCodec.ROOT_KEY, GeneticPayloadCodec.encode(payload));
        blockEntity.setChanged();
    }

    public static void clear(BlockEntity blockEntity) {
        if (blockEntity == null) {
            return;
        }
        blockEntity.getPersistentData().remove(GeneticPayloadCodec.ROOT_KEY);
        blockEntity.setChanged();
    }
}
