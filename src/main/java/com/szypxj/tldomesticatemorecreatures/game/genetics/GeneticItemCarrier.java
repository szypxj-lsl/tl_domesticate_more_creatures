package com.szypxj.tldomesticatemorecreatures.game.genetics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class GeneticItemCarrier {
    private GeneticItemCarrier() {
    }

    public static void set(ItemStack stack, HatchGeneticPayload payload) {
        if (stack == null || stack.isEmpty() || payload == null) {
            return;
        }
        stack.getOrCreateTag().put(GeneticPayloadCodec.ROOT_KEY, GeneticPayloadCodec.encode(payload));
    }

    public static Optional<HatchGeneticPayload> get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(GeneticPayloadCodec.ROOT_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        return GeneticPayloadCodec.decode(root.getCompound(GeneticPayloadCodec.ROOT_KEY));
    }

    public static void clear(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getTag() == null) {
            return;
        }
        stack.getTag().remove(GeneticPayloadCodec.ROOT_KEY);
    }
}
