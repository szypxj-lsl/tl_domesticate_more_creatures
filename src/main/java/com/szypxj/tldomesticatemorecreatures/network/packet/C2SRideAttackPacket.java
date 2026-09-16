package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.riding.RideAttackService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SRideAttackPacket(int mountEntityId, int sequence) {
    public static void encode(C2SRideAttackPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.mountEntityId());
        buffer.writeVarInt(packet.sequence());
    }

    public static C2SRideAttackPacket decode(FriendlyByteBuf buffer) {
        return new C2SRideAttackPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(C2SRideAttackPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) RideAttackService.acceptAttack(sender, packet.mountEntityId(), packet.sequence());
        });
        context.setPacketHandled(true);
    }
}
