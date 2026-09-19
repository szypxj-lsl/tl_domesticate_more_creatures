package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CImprintStatePacket(
        int entityId,
        boolean active,
        boolean finished,
        int percent,
        int completed,
        int total,
        long remainingTicks,
        String needType,
        String foodNameKey,
        long nextNeedTicks,
        boolean bonded
) {
    public S2CImprintStatePacket {
        percent = Math.max(0, Math.min(100, percent));
        total = Math.max(0, total);
        completed = Math.max(0, Math.min(total, completed));
        remainingTicks = Math.max(0L, remainingTicks);
        needType = needType == null ? "" : needType;
        foodNameKey = foodNameKey == null ? "" : foodNameKey;
        nextNeedTicks = Math.max(0L, nextNeedTicks);
    }

    public static void encode(S2CImprintStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
        buffer.writeBoolean(packet.active());
        buffer.writeBoolean(packet.finished());
        buffer.writeVarInt(packet.percent());
        buffer.writeVarInt(packet.completed());
        buffer.writeVarInt(packet.total());
        buffer.writeVarLong(packet.remainingTicks());
        buffer.writeUtf(packet.needType());
        buffer.writeUtf(packet.foodNameKey());
        buffer.writeVarLong(packet.nextNeedTicks());
        buffer.writeBoolean(packet.bonded());
    }

    public static S2CImprintStatePacket decode(FriendlyByteBuf buffer) {
        return new S2CImprintStatePacket(
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarLong(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readVarLong(),
                buffer.readBoolean()
        );
    }

    public static void handle(S2CImprintStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientState.setImprintState(packet)
        ));
        context.setPacketHandled(true);
    }
}
