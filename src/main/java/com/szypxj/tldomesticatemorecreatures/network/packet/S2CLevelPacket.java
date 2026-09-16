package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CLevelPacket(int entityId, int level) {
    public static void encode(S2CLevelPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
        buffer.writeVarInt(packet.level());
    }

    public static S2CLevelPacket decode(FriendlyByteBuf buffer) {
        return new S2CLevelPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(S2CLevelPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientState.setLevel(packet.entityId(), packet.level())));
        context.setPacketHandled(true);
    }
}
