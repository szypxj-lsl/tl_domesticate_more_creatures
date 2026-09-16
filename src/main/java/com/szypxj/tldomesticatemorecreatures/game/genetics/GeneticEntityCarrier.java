package com.szypxj.tldomesticatemorecreatures.game.genetics;

import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class GeneticEntityCarrier {
    private GeneticEntityCarrier() {
    }

    public static void set(Entity entity, HatchGeneticPayload payload) {
        if (entity == null || payload == null) {
            return;
        }
        entity.getPersistentData().put(GeneticPayloadCodec.ROOT_KEY, GeneticPayloadCodec.encode(payload));
    }

    public static Optional<HatchGeneticPayload> get(Entity entity) {
        if (entity == null || !entity.getPersistentData().contains(GeneticPayloadCodec.ROOT_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        return GeneticPayloadCodec.decode(entity.getPersistentData().getCompound(GeneticPayloadCodec.ROOT_KEY));
    }

    public static boolean has(Entity entity) {
        return get(entity).isPresent();
    }

    public static void clear(Entity entity) {
        if (entity != null) {
            entity.getPersistentData().remove(GeneticPayloadCodec.ROOT_KEY);
        }
    }
}
