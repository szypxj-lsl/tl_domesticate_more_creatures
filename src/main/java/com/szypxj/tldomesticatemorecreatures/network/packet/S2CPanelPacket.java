package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import com.szypxj.tldomesticatemorecreatures.network.PanelSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CPanelPacket(PanelSnapshot snapshot) {
    public static void encode(S2CPanelPacket packet, FriendlyByteBuf buffer) {
        packet.snapshot().encode(buffer);
    }

    public static S2CPanelPacket decode(FriendlyByteBuf buffer) {
        return new S2CPanelPacket(PanelSnapshot.decode(buffer));
    }

    public static void handle(S2CPanelPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientState.openPanel(packet.snapshot())));
        context.setPacketHandled(true);
    }
}
