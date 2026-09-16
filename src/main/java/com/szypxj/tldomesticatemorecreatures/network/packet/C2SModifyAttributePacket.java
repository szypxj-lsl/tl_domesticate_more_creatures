package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.api.attribute.TdmcAttributeOperations;
import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.network.SnapshotFactory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SModifyAttributePacket(int entityId, ResourceLocation attributeId, int pointCount) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeResourceLocation(attributeId);
        buffer.writeVarInt(pointCount);
    }

    public static C2SModifyAttributePacket decode(FriendlyByteBuf buffer) {
        return new C2SModifyAttributePacket(
                buffer.readVarInt(),
                buffer.readResourceLocation(),
                buffer.readVarInt()
        );
    }

    public static void handle(C2SModifyAttributePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> apply(sender, packet));
        }
        context.setPacketHandled(true);
    }

    private static void apply(ServerPlayer sender, C2SModifyAttributePacket packet) {
        if (packet.attributeId() == null || packet.pointCount() <= 0 || packet.pointCount() > 100) {
            return;
        }
        Entity entity = sender.level().getEntity(packet.entityId());
        if (!(entity instanceof LivingEntity target)) {
            return;
        }
        if (target != sender && !PetOwnershipService.isOwnedBy(target, sender)) {
            return;
        }
        LevelService.initializeIfNeeded(target);
        if (TdmcAttributeOperations.allocatePoints(sender, target, packet.attributeId(), packet.pointCount())) {
            NetworkHandler.sendPanel(sender, SnapshotFactory.panel(sender, target));
        }
    }
}
