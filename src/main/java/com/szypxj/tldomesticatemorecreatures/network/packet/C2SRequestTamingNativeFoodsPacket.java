package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SRequestTamingNativeFoodsPacket(ResourceLocation entityId) {
    public static void encode(C2SRequestTamingNativeFoodsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.entityId());
    }

    public static C2SRequestTamingNativeFoodsPacket decode(FriendlyByteBuf buffer) {
        return new C2SRequestTamingNativeFoodsPacket(buffer.readResourceLocation());
    }

    public static void handle(C2SRequestTamingNativeFoodsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (!TamingEditorService.mayManage(sender)) {
                if (sender != null) {
                    sender.sendSystemMessage(Component.translatable("msg.tl_domesticate_more_creatures.taming_editor.no_permission"));
                }
                return;
            }
            TamingEditorService.NativeFoodsResult result = TamingEditorService.detectNativeFoods(sender.serverLevel(), packet.entityId());
            NetworkHandler.sendTamingNativeFoods(sender, packet.entityId(), result);
        });
        context.setPacketHandled(true);
    }
}
