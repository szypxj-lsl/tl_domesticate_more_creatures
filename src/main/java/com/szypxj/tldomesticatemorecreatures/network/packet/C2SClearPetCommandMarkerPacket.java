package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record C2SClearPetCommandMarkerPacket(UUID markerUuid) {
    public static void encode(C2SClearPetCommandMarkerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.markerUuid());
    }

    public static C2SClearPetCommandMarkerPacket decode(FriendlyByteBuf buffer) {
        return new C2SClearPetCommandMarkerPacket(buffer.readUUID());
    }

    public static void handle(C2SClearPetCommandMarkerPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                PetCommandService.clearMarkerForOwner(sender, packet.markerUuid());
            }
        });
        context.setPacketHandled(true);
    }
}
