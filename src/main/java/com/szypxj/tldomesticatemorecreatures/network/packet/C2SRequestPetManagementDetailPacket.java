package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record C2SRequestPetManagementDetailPacket(UUID petUuid) {
    public static void encode(C2SRequestPetManagementDetailPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.petUuid);
    }

    public static C2SRequestPetManagementDetailPacket decode(FriendlyByteBuf buffer) {
        return new C2SRequestPetManagementDetailPacket(buffer.readUUID());
    }

    public static void handle(C2SRequestPetManagementDetailPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) NetworkHandler.sendPetManagementDetail(sender, packet.petUuid);
        });
        context.setPacketHandled(true);
    }
}
