package com.szypxj.tldomesticatemorecreatures.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record PetManagementDetail(PetManagementSummary summary, CompoundTag previewTag) {
    public PetManagementDetail {
        previewTag = previewTag == null ? new CompoundTag() : previewTag.copy();
    }

    @Override
    public CompoundTag previewTag() {
        return previewTag.copy();
    }

    public void encode(FriendlyByteBuf buffer) {
        summary.encode(buffer);
        buffer.writeNbt(previewTag);
    }

    public static PetManagementDetail decode(FriendlyByteBuf buffer) {
        PetManagementSummary summary = PetManagementSummary.decode(buffer);
        CompoundTag tag = buffer.readNbt();
        return new PetManagementDetail(summary, tag == null ? new CompoundTag() : tag);
    }
}
