package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SReloadTamingEditorPacket() {
    public static void encode(C2SReloadTamingEditorPacket packet, FriendlyByteBuf buffer) {
    }

    public static C2SReloadTamingEditorPacket decode(FriendlyByteBuf buffer) {
        return new C2SReloadTamingEditorPacket();
    }

    public static void handle(C2SReloadTamingEditorPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (!TamingEditorService.mayManage(sender)) {
                if (sender != null) {
                    String key = "msg.tl_domesticate_more_creatures.taming_editor.no_permission";
                    sender.sendSystemMessage(Component.translatable(key));
                    NetworkHandler.sendTamingEditorOperationResult(sender, false, false, key);
                }
                return;
            }
            TamingEditorService.ReloadResult result = TamingEditorService.reload(sender);
            sender.sendSystemMessage(Component.translatable(result.messageKey()));
            NetworkHandler.sendTamingEditorOperationResult(sender, false, result.success(), result.messageKey());
            if (result.success()) {
                NetworkHandler.sendTamingEditorSnapshot(sender, TamingEditorService.snapshot(), false);
            }
        });
        context.setPacketHandled(true);
    }
}
