package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.api.compat.ErsCompatApi;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SOpenNativePetInventoryPacket(int entityId) {
    public static void encode(C2SOpenNativePetInventoryPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
    }

    public static C2SOpenNativePetInventoryPacket decode(FriendlyByteBuf buffer) {
        return new C2SOpenNativePetInventoryPacket(buffer.readVarInt());
    }

    public static void handle(C2SOpenNativePetInventoryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            Entity entity = sender.serverLevel().getEntity(packet.entityId());
            if (!(entity instanceof LivingEntity living)
                    || (sender.getVehicle() != living && sender.distanceToSqr(living) > 64.0D)
                    || !PetOwnershipService.isOwnedBy(living, sender)
                    || !ErsCompatApi.hasNativeInventory(living)) {
                return;
            }
            ErsCompatApi.openNativeInventory(sender, living);
        });
        context.setPacketHandled(true);
    }
}
