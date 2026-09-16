package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.petmanagement.PetManagementService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record C2SSetPetShortcutPacket(UUID petUuid, int slot) {
    public static void encode(C2SSetPetShortcutPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.petUuid);
        buffer.writeByte(packet.slot);
    }

    public static C2SSetPetShortcutPacket decode(FriendlyByteBuf buffer) {
        return new C2SSetPetShortcutPacket(buffer.readUUID(), buffer.readByte());
    }

    public static void handle(C2SSetPetShortcutPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;
            if (!PetManagementService.setShortcut(sender, packet.petUuid, packet.slot)) {
                sender.sendSystemMessage(Component.translatable("msg.tl_domesticate_more_creatures.pet_management.shortcut_failed"));
            }
            NetworkHandler.sendPetManagementList(sender);
        });
        context.setPacketHandled(true);
    }
}
