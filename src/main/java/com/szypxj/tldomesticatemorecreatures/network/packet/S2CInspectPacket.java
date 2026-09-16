package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import com.szypxj.tldomesticatemorecreatures.network.InspectSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CInspectPacket(InspectSnapshot snapshot) {
    public static void encode(S2CInspectPacket packet, FriendlyByteBuf buffer) {
        packet.snapshot().encode(buffer);
    }

    public static S2CInspectPacket decode(FriendlyByteBuf buffer) {
        return new S2CInspectPacket(InspectSnapshot.decode(buffer));
    }

    public static void handle(S2CInspectPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientState.setInspect(packet.snapshot())));
        context.setPacketHandled(true);
    }
}
