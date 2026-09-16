package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.spyglass.SpyglassScanService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SSpyglassScanCandidatePacket(int entityId) {
    public static void encode(C2SSpyglassScanCandidatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
    }

    public static C2SSpyglassScanCandidatePacket decode(FriendlyByteBuf buffer) {
        return new C2SSpyglassScanCandidatePacket(buffer.readVarInt());
    }

    public static void handle(C2SSpyglassScanCandidatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                SpyglassScanService.setCandidate(sender, packet.entityId());
            }
        });
        context.setPacketHandled(true);
    }
}
