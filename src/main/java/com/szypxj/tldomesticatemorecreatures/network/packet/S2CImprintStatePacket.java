package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CImprintStatePacket(int entityId, boolean active, long remainingTicks) {
    public static void encode(S2CImprintStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
        buffer.writeBoolean(packet.active());
        buffer.writeVarLong(Math.max(0L, packet.remainingTicks()));
    }

    public static S2CImprintStatePacket decode(FriendlyByteBuf buffer) {
        return new S2CImprintStatePacket(buffer.readVarInt(), buffer.readBoolean(), buffer.readVarLong());
    }

    public static void handle(S2CImprintStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientState.setImprintState(packet.entityId(), packet.active(), packet.remainingTicks())
        ));
        context.setPacketHandled(true);
    }
}
