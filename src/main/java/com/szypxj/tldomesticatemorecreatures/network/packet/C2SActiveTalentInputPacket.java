package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.talent.active.ActiveTalentService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SActiveTalentInputPacket(int mountEntityId, int sequence) {
    public static void encode(C2SActiveTalentInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.mountEntityId());
        buffer.writeVarInt(packet.sequence());
    }

    public static C2SActiveTalentInputPacket decode(FriendlyByteBuf buffer) {
        return new C2SActiveTalentInputPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(C2SActiveTalentInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                ActiveTalentService.activateOrInput(sender, packet.mountEntityId(), packet.sequence());
            }
        });
        context.setPacketHandled(true);
    }
}
