package com.szypxj.tldomesticatemorecreatures.network.packet;

import com.szypxj.tldomesticatemorecreatures.game.LevelService;
import com.szypxj.tldomesticatemorecreatures.game.PetOwnershipService;
import com.szypxj.tldomesticatemorecreatures.network.NetworkHandler;
import com.szypxj.tldomesticatemorecreatures.network.SnapshotFactory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SAllocateStatPacket(int entityId, String statId) {
    public static void encode(C2SAllocateStatPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId());
        buffer.writeUtf(packet.statId(), 128);
    }

    public static C2SAllocateStatPacket decode(FriendlyByteBuf buffer) {
        return new C2SAllocateStatPacket(buffer.readInt(), buffer.readUtf(128));
    }

    public static void handle(C2SAllocateStatPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            LivingEntity target = resolveOwned(sender, packet.entityId());
            if (target == null) {
                return;
            }
            LevelService.allocatePoint(target, packet.statId());
            NetworkHandler.sendPanel(sender, SnapshotFactory.panel(sender, target));
        });
        context.setPacketHandled(true);
    }

    private static LivingEntity resolveOwned(ServerPlayer sender, int id) {
        if (id == sender.getId()) {
            return sender;
        }
        Entity entity = sender.level().getEntity(id);
        if (!(entity instanceof LivingEntity living)
                || !PetOwnershipService.isOwnedBy(living, sender)
                || !PetOwnershipService.canUsePanel(living)) {
            return null;
        }
        return sender.getVehicle() == living || sender.distanceToSqr(entity) <= 64.0D ? living : null;
    }
}
