package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.domestication.DomesticationData;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SToggleRidePermissionPacket(int entityId) {
    public static void encode(C2SToggleRidePermissionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
    }

    public static C2SToggleRidePermissionPacket decode(FriendlyByteBuf buffer) {
        return new C2SToggleRidePermissionPacket(buffer.readVarInt());
    }

    public static void handle(C2SToggleRidePermissionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            Entity entity = sender.serverLevel().getEntity(packet.entityId());
            if (!(entity instanceof LivingEntity living)
                    || sender.distanceToSqr(living) > 64.0D
                    || !PetOwnershipService.isOwnedBy(living, sender)) {
                return;
            }
            DomesticationData data = DomesticationData.of(living);
            boolean next = !data.allowOtherRiders();
            data.allowOtherRiders(next);
            sender.displayClientMessage(Component.translatable(next
                    ? "msg.tl_domesticate_more_creatures.riding.permission_enabled"
                    : "msg.tl_domesticate_more_creatures.riding.permission_disabled"), true);
        });
        context.setPacketHandled(true);
    }
}
