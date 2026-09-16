package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.riding.RideService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SRideMountPacket(int entityId) {
    private static final double MAX_DISTANCE_SQR = 64.0D;

    public static void encode(C2SRideMountPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId());
    }

    public static C2SRideMountPacket decode(FriendlyByteBuf buffer) {
        return new C2SRideMountPacket(buffer.readInt());
    }

    public static void handle(C2SRideMountPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            if (sender.getVehicle() instanceof LivingEntity currentMount) {
                if (RideService.isGenericRider(sender)) {
                    RideService.stopRide(currentMount, RideService.StopReason.MANUAL);
                } else {
                    sender.stopRiding();
                }
                return;
            }
            if (packet.entityId() < 0) {
                return;
            }
            Entity entity = sender.serverLevel().getEntity(packet.entityId());
            if (!(entity instanceof LivingEntity living)) {
                return;
            }
            if (sender.distanceToSqr(living) > MAX_DISTANCE_SQR) {
                return;
            }
            RideService.tryMount(sender, living);
        });
        context.setPacketHandled(true);
    }
}
