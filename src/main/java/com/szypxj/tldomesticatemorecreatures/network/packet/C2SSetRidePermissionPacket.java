package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.domestication.DomesticationData;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.network.SnapshotFactory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SSetRidePermissionPacket(int entityId, boolean allowOtherRiders) {
    public static void encode(C2SSetRidePermissionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId());
        buffer.writeBoolean(packet.allowOtherRiders());
    }

    public static C2SSetRidePermissionPacket decode(FriendlyByteBuf buffer) {
        return new C2SSetRidePermissionPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(C2SSetRidePermissionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            Entity entity = sender.serverLevel().getEntity(packet.entityId());
            if (!(entity instanceof LivingEntity living)) {
                return;
            }
            if (sender.distanceToSqr(living) > 64.0D) {
                return;
            }
            if (!PetOwnershipService.isOwnedBy(living, sender)) {
                return;
            }
            DomesticationData.of(living).allowOtherRiders(packet.allowOtherRiders());
            sender.displayClientMessage(Component.translatable(packet.allowOtherRiders()
                    ? "msg.tl_domesticate_more_creatures.riding.permission_enabled"
                    : "msg.tl_domesticate_more_creatures.riding.permission_disabled"), true);
            NetworkHandler.sendPanel(sender, SnapshotFactory.panel(sender, living));
        });
        context.setPacketHandled(true);
    }
}
