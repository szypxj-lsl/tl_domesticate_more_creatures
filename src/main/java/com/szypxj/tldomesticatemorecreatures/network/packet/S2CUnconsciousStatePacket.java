package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.ClientTorporState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CUnconsciousStatePacket(boolean unconscious) {
    public static void encode(S2CUnconsciousStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.unconscious());
    }

    public static S2CUnconsciousStatePacket decode(FriendlyByteBuf buffer) {
        return new S2CUnconsciousStatePacket(buffer.readBoolean());
    }

    public static void handle(S2CUnconsciousStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientTorporState.setUnconscious(packet.unconscious())
        ));
        context.setPacketHandled(true);
    }
}
