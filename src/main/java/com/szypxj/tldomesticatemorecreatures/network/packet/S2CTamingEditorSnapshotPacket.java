package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.taming.TamingEditorScreen;
import com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorNetworkCodec;
import com.szypxj.tldomesticatemorecreatures.domestication.editor.TamingEditorSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CTamingEditorSnapshotPacket(TamingEditorSnapshot snapshot, boolean openEditor) {
    public static void encode(S2CTamingEditorSnapshotPacket packet, FriendlyByteBuf buffer) {
        TamingEditorNetworkCodec.writeSnapshot(buffer, packet.snapshot());
        buffer.writeBoolean(packet.openEditor());
    }

    public static S2CTamingEditorSnapshotPacket decode(FriendlyByteBuf buffer) {
        return new S2CTamingEditorSnapshotPacket(TamingEditorNetworkCodec.readSnapshot(buffer), buffer.readBoolean());
    }

    public static void handle(S2CTamingEditorSnapshotPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (packet.openEditor()) {
                minecraft.setScreen(new TamingEditorScreen(packet.snapshot()));
            } else if (minecraft.screen instanceof TamingEditorScreen screen) {
                screen.onServerSnapshot(packet.snapshot());
            }
        }));
        context.setPacketHandled(true);
    }
}
