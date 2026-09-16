package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import com.szypxj.tldomesticatemorecreatures.network.TargetHudSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CTargetHudPacket(TargetHudSnapshot snapshot) {
    public static void encode(S2CTargetHudPacket packet, FriendlyByteBuf buffer) {
        packet.snapshot().encode(buffer);
    }

    public static S2CTargetHudPacket decode(FriendlyByteBuf buffer) {
        return new S2CTargetHudPacket(TargetHudSnapshot.decode(buffer));
    }

    public static void handle(S2CTargetHudPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientState.setTargetHud(packet.snapshot())));
        context.setPacketHandled(true);
    }
}
