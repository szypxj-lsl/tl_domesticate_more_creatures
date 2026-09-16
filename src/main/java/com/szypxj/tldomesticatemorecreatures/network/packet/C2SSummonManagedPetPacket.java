package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.petmanagement.summon.PetSummonService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record C2SSummonManagedPetPacket(UUID petUuid) {
    public static void encode(C2SSummonManagedPetPacket packet, FriendlyByteBuf buffer) { buffer.writeUUID(packet.petUuid); }
    public static C2SSummonManagedPetPacket decode(FriendlyByteBuf buffer) { return new C2SSummonManagedPetPacket(buffer.readUUID()); }
    public static void handle(C2SSummonManagedPetPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;
            PetSummonService.start(sender, packet.petUuid);
            NetworkHandler.sendPetManagementList(sender);
        });
        context.setPacketHandled(true);
    }
}
