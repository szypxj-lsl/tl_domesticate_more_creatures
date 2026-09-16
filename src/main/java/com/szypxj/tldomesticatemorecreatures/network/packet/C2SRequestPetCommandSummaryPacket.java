package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.command.pet.PetCommandService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SRequestPetCommandSummaryPacket() {
    public static void encode(C2SRequestPetCommandSummaryPacket packet, FriendlyByteBuf buffer) {
    }

    public static C2SRequestPetCommandSummaryPacket decode(FriendlyByteBuf buffer) {
        return new C2SRequestPetCommandSummaryPacket();
    }

    public static void handle(C2SRequestPetCommandSummaryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                PetCommandService.sendSummary(sender);
            }
        });
        context.setPacketHandled(true);
    }
}
