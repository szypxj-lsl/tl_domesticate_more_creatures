package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideInputPhase;
import com.szypxj.tldomesticatemorecreatures.riding.control.action.RideControlDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SRideActionPacket(
        int mountEntityId,
        RideAction action,
        RideInputPhase phase,
        int sequence
) {
    public static void encode(C2SRideActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.mountEntityId());
        buffer.writeEnum(packet.action());
        buffer.writeEnum(packet.phase());
        buffer.writeVarInt(packet.sequence());
    }

    public static C2SRideActionPacket decode(FriendlyByteBuf buffer) {
        return new C2SRideActionPacket(
                buffer.readVarInt(),
                buffer.readEnum(RideAction.class),
                buffer.readEnum(RideInputPhase.class),
                buffer.readVarInt()
        );
    }

    public static void handle(C2SRideActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                RideControlDispatcher.acceptAction(
                        sender,
                        packet.mountEntityId(),
                        packet.action(),
                        packet.phase(),
                        packet.sequence()
                );
            }
        });
        context.setPacketHandled(true);
    }
}
