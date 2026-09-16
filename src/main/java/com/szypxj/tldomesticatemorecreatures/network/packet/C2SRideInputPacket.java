package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.riding.RideInputState;
import com.szypxj.tldomesticatemorecreatures.riding.RideService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SRideInputPacket(
        int mountEntityId,
        float forward,
        float strafe,
        boolean jump,
        boolean descend,
        int sequence
) {
    public static void encode(C2SRideInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.mountEntityId());
        buffer.writeFloat(packet.forward());
        buffer.writeFloat(packet.strafe());
        buffer.writeBoolean(packet.jump());
        buffer.writeBoolean(packet.descend());
        buffer.writeVarInt(packet.sequence());
    }

    public static C2SRideInputPacket decode(FriendlyByteBuf buffer) {
        return new C2SRideInputPacket(
                buffer.readVarInt(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt()
        );
    }

    public static void handle(C2SRideInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                RideService.acceptInput(sender, packet.mountEntityId(), new RideInputState(
                        packet.forward(), packet.strafe(), packet.jump(), packet.descend(), packet.sequence()
                ));
            }
        });
        context.setPacketHandled(true);
    }
}
