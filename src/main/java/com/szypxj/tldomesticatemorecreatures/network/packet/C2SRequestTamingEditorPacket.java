package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SRequestTamingEditorPacket(boolean openEditor) {
    public static void encode(C2SRequestTamingEditorPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.openEditor());
    }

    public static C2SRequestTamingEditorPacket decode(FriendlyByteBuf buffer) {
        return new C2SRequestTamingEditorPacket(buffer.readBoolean());
    }

    public static void handle(C2SRequestTamingEditorPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (!TamingEditorService.mayManage(sender)) {
                if (sender != null) {
                    sender.sendSystemMessage(Component.translatable("msg.tl_domesticate_more_creatures.taming_editor.no_permission"));
                }
                return;
            }
            NetworkHandler.sendTamingEditorSnapshot(sender, TamingEditorService.snapshot(), packet.openEditor());
        });
        context.setPacketHandled(true);
    }
}
