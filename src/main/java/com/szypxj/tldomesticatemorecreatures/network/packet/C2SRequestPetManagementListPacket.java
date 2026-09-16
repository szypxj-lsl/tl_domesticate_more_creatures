package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SRequestPetManagementListPacket() {
    public static void encode(C2SRequestPetManagementListPacket packet, FriendlyByteBuf buffer) {
    }

    public static C2SRequestPetManagementListPacket decode(FriendlyByteBuf buffer) {
        return new C2SRequestPetManagementListPacket();
    }

    public static void handle(C2SRequestPetManagementListPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) NetworkHandler.sendPetManagementList(sender);
        });
        context.setPacketHandled(true);
    }
}
