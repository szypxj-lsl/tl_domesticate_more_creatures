package com.szypxj.tldomesticatemorecreatures.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record InspectTarget(Kind kind, int entityId, long blockPos) {
    public enum Kind {
        ENTITY,
        BLOCK
    }

    public InspectTarget {
        if (kind == null) {
            kind = Kind.ENTITY;
        }
    }

    public static InspectTarget entity(int entityId) {
        return new InspectTarget(Kind.ENTITY, entityId, 0L);
    }

    public static InspectTarget block(BlockPos pos) {
        return new InspectTarget(Kind.BLOCK, -1, pos == null ? 0L : pos.asLong());
    }

    public BlockPos blockPosition() {
        return BlockPos.of(blockPos);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeByte(kind.ordinal());
        if (kind == Kind.BLOCK) {
            buffer.writeLong(blockPos);
        } else {
            buffer.writeVarInt(entityId);
        }
    }

    public static InspectTarget decode(FriendlyByteBuf buffer) {
        int ordinal = buffer.readUnsignedByte();
        if (ordinal == Kind.BLOCK.ordinal()) {
            return new InspectTarget(Kind.BLOCK, -1, buffer.readLong());
        }
        return new InspectTarget(Kind.ENTITY, buffer.readVarInt(), 0L);
    }
}
