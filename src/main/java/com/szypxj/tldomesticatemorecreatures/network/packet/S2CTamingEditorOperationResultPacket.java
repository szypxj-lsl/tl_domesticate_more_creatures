package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.taming.TamingEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CTamingEditorOperationResultPacket(
        boolean saveOperation,
        boolean success,
        String messageKey
) {
    public S2CTamingEditorOperationResultPacket {
        messageKey = messageKey == null ? "" : messageKey;
    }

    public static void encode(S2CTamingEditorOperationResultPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.saveOperation());
        buffer.writeBoolean(packet.success());
        buffer.writeUtf(packet.messageKey(), 256);
    }

    public static S2CTamingEditorOperationResultPacket decode(FriendlyByteBuf buffer) {
        return new S2CTamingEditorOperationResultPacket(
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readUtf(256)
        );
    }

    public static void handle(S2CTamingEditorOperationResultPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof TamingEditorScreen screen) {
                screen.onOperationResult(packet.saveOperation(), packet.success(), packet.messageKey());
            }
        }));
        context.setPacketHandled(true);
    }
}
