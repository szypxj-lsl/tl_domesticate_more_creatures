package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CPetOwnershipPacket(int entityId, boolean ownedByPlayer) {
    public static void encode(S2CPetOwnershipPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
        buffer.writeBoolean(packet.ownedByPlayer());
    }

    public static S2CPetOwnershipPacket decode(FriendlyByteBuf buffer) {
        return new S2CPetOwnershipPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(S2CPetOwnershipPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientState.setOwnedPet(packet.entityId(), packet.ownedByPlayer())
        ));
        context.setPacketHandled(true);
    }
}
