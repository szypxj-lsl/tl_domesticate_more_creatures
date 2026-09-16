package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingConfigManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SRequestRidingConfigPacket() {
    public static void encode(C2SRequestRidingConfigPacket packet, FriendlyByteBuf buffer) {
    }

    public static C2SRequestRidingConfigPacket decode(FriendlyByteBuf buffer) {
        return new C2SRequestRidingConfigPacket();
    }

    public static void handle(C2SRequestRidingConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                NetworkHandler.sendRidingSnapshot(sender, RidingConfigManager.snapshot(sender.serverLevel()));
            }
        });
        context.setPacketHandled(true);
    }
}
