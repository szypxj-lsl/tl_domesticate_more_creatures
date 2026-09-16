package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.riding.config.RidingConfigManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SRideConfigReloadPacket() {
    public static void encode(C2SRideConfigReloadPacket packet, FriendlyByteBuf buffer) {
    }

    public static C2SRideConfigReloadPacket decode(FriendlyByteBuf buffer) {
        return new C2SRideConfigReloadPacket();
    }

    public static void handle(C2SRideConfigReloadPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || !RidingConfigManager.mayManage(sender)) {
                if (sender != null) {
                    sender.sendSystemMessage(Component.translatable("msg.tl_domesticate_more_creatures.riding.no_permission"));
                }
                return;
            }
            RidingConfigManager.ReloadResult result = RidingConfigManager.reload(sender);
            sender.sendSystemMessage(Component.translatable(result.messageKey()));
            if (result.success()) {
                NetworkHandler.broadcastRidingSnapshot(RidingConfigManager.snapshot(sender.serverLevel()));
            }
        });
        context.setPacketHandled(true);
    }
}
