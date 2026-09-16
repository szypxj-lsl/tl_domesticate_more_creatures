package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.petmanagement.summon.PetSummonService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SSummonPetShortcutPacket(int slot) {
    public static void encode(C2SSummonPetShortcutPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.slot);
    }

    public static C2SSummonPetShortcutPacket decode(FriendlyByteBuf buffer) {
        return new C2SSummonPetShortcutPacket(buffer.readByte());
    }

    public static void handle(C2SSummonPetShortcutPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || packet.slot < 1 || packet.slot > 9) return;
            PetSummonService.startShortcut(sender, packet.slot);
        });
        context.setPacketHandled(true);
    }
}
