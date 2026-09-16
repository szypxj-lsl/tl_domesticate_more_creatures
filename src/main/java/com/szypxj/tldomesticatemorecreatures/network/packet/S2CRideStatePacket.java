package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.client.ClientState;
import com.szypxj.tldomesticatemorecreatures.riding.RideEnvironment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CRideStatePacket(
        int mountEntityId,
        int riderEntityId,
        boolean active,
        RideEnvironment environment,
        long generation
) {
    public static void encode(S2CRideStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.mountEntityId());
        buffer.writeVarInt(packet.riderEntityId());
        buffer.writeBoolean(packet.active());
        buffer.writeEnum(packet.environment());
        buffer.writeVarLong(packet.generation());
    }

    public static S2CRideStatePacket decode(FriendlyByteBuf buffer) {
        return new S2CRideStatePacket(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readEnum(RideEnvironment.class),
                buffer.readVarLong()
        );
    }

    public static void handle(S2CRideStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientState.setGenericRideState(
                        packet.mountEntityId(),
                        packet.riderEntityId(),
                        packet.active(),
                        packet.environment(),
                        packet.generation()
                )
        ));
        context.setPacketHandled(true);
    }
}
