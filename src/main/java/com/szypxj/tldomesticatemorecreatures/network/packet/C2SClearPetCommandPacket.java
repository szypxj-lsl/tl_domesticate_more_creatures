package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SClearPetCommandPacket() {
    public static void encode(C2SClearPetCommandPacket packet, FriendlyByteBuf buffer) {
    }

    public static C2SClearPetCommandPacket decode(FriendlyByteBuf buffer) {
        return new C2SClearPetCommandPacket();
    }

    public static void handle(C2SClearPetCommandPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                PetCommandService.clearForOwner(sender);
            }
        });
        context.setPacketHandled(true);
    }
}
