package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.api.riding.RideAction;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionState;
import com.szypxj.tldomesticatemorecreatures.api.riding.RideActionStatus;
import com.szypxj.tldomesticatemorecreatures.client.riding.ClientRideControlState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CRideActionStatePacket(
        int mountEntityId,
        RideAction action,
        RideActionStatus status
) {
    public S2CRideActionStatePacket {
        status = status == null ? RideActionStatus.READY : status;
    }

    public static void encode(S2CRideActionStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.mountEntityId());
        buffer.writeEnum(packet.action());
        buffer.writeEnum(packet.status().state());
        buffer.writeVarInt(packet.status().remainingTicks());
        buffer.writeVarInt(packet.status().totalTicks());
        buffer.writeFloat(packet.status().chargeProgress());
    }

    public static S2CRideActionStatePacket decode(FriendlyByteBuf buffer) {
        return new S2CRideActionStatePacket(
                buffer.readVarInt(),
                buffer.readEnum(RideAction.class),
                new RideActionStatus(
                        buffer.readEnum(RideActionState.class),
                        Math.max(0, buffer.readVarInt()),
                        Math.max(0, buffer.readVarInt()),
                        buffer.readFloat()
                )
        );
    }

    public static void handle(S2CRideActionStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientRideControlState.applyStatus(packet.mountEntityId(), packet.action(), packet.status())
        ));
        context.setPacketHandled(true);
    }
}
