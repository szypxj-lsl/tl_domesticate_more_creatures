package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorNetworkCodec;
import com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorRule;
import com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public record C2SSaveTamingEditorPacket(
        Map<ResourceLocation, TamingEditorRule> upserts,
        Set<ResourceLocation> deletes
) {
    public C2SSaveTamingEditorPacket {
        upserts = Map.copyOf(upserts == null ? Map.of() : upserts);
        deletes = Set.copyOf(deletes == null ? Set.of() : deletes);
    }

    public static void encode(C2SSaveTamingEditorPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.upserts().size());
        for (TamingEditorRule rule : packet.upserts().values()) {
            TamingEditorNetworkCodec.writeRule(buffer, rule);
        }
        TamingEditorNetworkCodec.writeIds(buffer, packet.deletes());
    }

    public static C2SSaveTamingEditorPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > 65536) {
            throw new IllegalArgumentException("Invalid taming rule count: " + count);
        }
        Map<ResourceLocation, TamingEditorRule> upserts = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            TamingEditorRule rule = TamingEditorNetworkCodec.readRule(buffer);
            upserts.put(rule.entityId(), rule);
        }
        return new C2SSaveTamingEditorPacket(upserts, TamingEditorNetworkCodec.readIds(buffer));
    }

    public static void handle(C2SSaveTamingEditorPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (!TamingEditorService.mayManage(sender)) {
                if (sender != null) {
                    String key = "msg.tl_domesticate_more_creatures.taming_editor.no_permission";
                    sender.sendSystemMessage(Component.translatable(key));
                    NetworkHandler.sendTamingEditorOperationResult(sender, true, false, key);
                }
                return;
            }
            TamingEditorService.SaveResult result = TamingEditorService.save(sender, packet.upserts(), packet.deletes());
            sender.sendSystemMessage(Component.translatable(result.messageKey()));
            NetworkHandler.sendTamingEditorOperationResult(sender, true, result.success(), result.messageKey());
            if (result.success()) {
                NetworkHandler.sendTamingEditorSnapshot(sender, TamingEditorService.snapshot(), false);
            }
        });
        context.setPacketHandled(true);
    }
}
