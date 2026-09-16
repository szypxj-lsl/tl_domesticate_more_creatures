package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.petmanagement.PetManagementService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record C2SStoreManagedPetPacket(UUID petUuid) {
    public static void encode(C2SStoreManagedPetPacket packet, FriendlyByteBuf buffer) { buffer.writeUUID(packet.petUuid); }
    public static C2SStoreManagedPetPacket decode(FriendlyByteBuf buffer) { return new C2SStoreManagedPetPacket(buffer.readUUID()); }
    public static void handle(C2SStoreManagedPetPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;
            if (!PetManagementService.store(sender, packet.petUuid)) {
                sender.sendSystemMessage(Component.translatable("msg.tl_domesticate_more_creatures.pet_management.store_failed"));
            }
            NetworkHandler.sendPetManagementList(sender);
        });
        context.setPacketHandled(true);
    }
}
